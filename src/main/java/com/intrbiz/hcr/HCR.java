package com.intrbiz.hcr;

import java.util.logging.Logger;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.intrbiz.hcr.command.CommandProcessor;
import com.intrbiz.hcr.stats.StatsServer;
import com.intrbiz.hcr.stats.handler.RootStatsHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

public class HCR implements Runnable
{
    private static Logger logger = Logger.getLogger(HCR.class.getCanonicalName());
    
    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private Channel serverChannel;

    private final int port;

    private int socketTimeout = 90; // seconds
    
    private int keyTimeout = 3600; // seconds

    private HazelcastInstance hazelcastInstance;
    
    private CommandProcessor processor;
    
    private StatsServer statsServer;

    public HCR(int port, int statsPort)
    {
        super();
        this.port = port;
        this.statsServer = new StatsServer(statsPort);
    }

    public int getSocketTimeout()
    {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout)
    {
        this.socketTimeout = socketTimeout;
    }

    public int getKeyTimeout()
    {
        return keyTimeout;
    }

    public void setKeyTimeout(int keyTimeout)
    {
        this.keyTimeout = keyTimeout;
    }
    
    public void run()
    {
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        try
        {
            Config config;
            // setup config
            String hazelcastConfigFile = coalesceEmpty(System.getProperty("hcr.hazelcast.config"), System.getenv("hcr_hazelcast_config"));
            if (hazelcastConfigFile != null)
            {
                config = new XmlConfigBuilder(hazelcastConfigFile).build();
            }
            else
            {
                config = new Config();
            }
            // add our custom config
            MapConfig dataMapConfig = config.getMapConfig("hcr.*");
            dataMapConfig.setAsyncBackupCount(2);
            dataMapConfig.setBackupCount(1);
            dataMapConfig.setEvictionPolicy(EvictionPolicy.LRU);
            dataMapConfig.setMaxIdleSeconds(this.keyTimeout);
            // set the instance name
            config.setInstanceName("hcr");
            // create the instance
            this.hazelcastInstance = Hazelcast.getOrCreateHazelcastInstance(config);
            // setup our processor
            this.processor = new CommandProcessor(this.hazelcastInstance);
            this.statsServer.registerHandler(new RootStatsHandler(this.processor));
            // start the stats server
            this.statsServer.start();
            // start the sever
            ServerBootstrap b = new ServerBootstrap();
            b.group(this.bossGroup, this.workerGroup);
            b.channel(NioServerSocketChannel.class);
            b.option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator());
            b.childHandler(new ChannelInitializer<SocketChannel>()
            {
                @Override
                public void initChannel(SocketChannel ch) throws Exception
                {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("read-timeout", new ReadTimeoutHandler(socketTimeout));
                    pipeline.addLast("write-timeout", new WriteTimeoutHandler(socketTimeout));
                    pipeline.addLast("redis-encoder", new RedisEncoder());
                    pipeline.addLast("redis-decoder", new RedisDecoder());
                    pipeline.addLast("string-aggregator", new RedisBulkStringAggregator());
                    pipeline.addLast("array-aggregator", new RedisArrayAggregator());
                    pipeline.addLast("handler", new HCRServerHandler(processor));
                }
            });
            //
            this.serverChannel = b.bind(this.port).sync().channel();
            logger.info("HCR server started at port " + this.port + '.');
            // await the server to stop
            this.serverChannel.closeFuture().sync();
            // log
            logger.info("HCR server has shutdown");
        }
        catch (Exception e)
        {
            logger.severe("Failed to start HCR");
            e.printStackTrace();
        }
        finally
        {
            this.bossGroup.shutdownGracefully();
            this.workerGroup.shutdownGracefully();
        }
    }

    public void stop()
    {
        this.serverChannel.close().awaitUninterruptibly();
        this.statsServer.stop();
    }
    
    private static String coalesceEmpty(String... strings)
    {
        for (String s : strings)
        {
            if (s != null && s.length() > 0)
                return s;
        }
        return null;
    }
    
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    
    public static void main(String[] args)
    {
        // get some config
        int port = Integer.parseInt(coalesceEmpty(System.getProperty("hcr.port"), System.getenv("hcr_port"), "6379"));
        int socketTimeout = Integer.parseInt(coalesceEmpty(System.getProperty("hcr.socket.timeout"), System.getenv("hcr_socket_timeout"), "600"));
        int keyTimeout = Integer.parseInt(coalesceEmpty(System.getProperty("hcr.key.timeout"), System.getenv("hcr_key_timeout"), "3600"));
        int statsPort = Integer.parseInt(coalesceEmpty(System.getProperty("hcr.stats.port"), System.getenv("hcr_stats_port"), "6380"));
        // create our server
        HCR hcr = new HCR(port, statsPort);
        hcr.setSocketTimeout(socketTimeout);
        hcr.setKeyTimeout(keyTimeout);
        hcr.run();
    }
}
