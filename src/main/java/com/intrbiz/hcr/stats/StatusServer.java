package com.intrbiz.hcr.stats;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.intrbiz.gerald.witchcraft.Witchcraft;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.CharsetUtil;

public class StatusServer implements Runnable
{
    private static Logger logger = Logger.getLogger(StatusServer.class);
    
    // metrics
    
    private static Counter activeConnections = Witchcraft.get().source("status").getRegistry().counter("active.connections");
    
    private static Meter connectionOpenRate = Witchcraft.get().source("status").getRegistry().meter("connection.open.rate");
    
    private static Meter connectionCloseRate = Witchcraft.get().source("status").getRegistry().meter("connection.close.rate");
    
    private static Meter connectionErrorRate = Witchcraft.get().source("status").getRegistry().meter("connection.error.rate");
    
    private static Timer connectionLifetime = Witchcraft.get().source("hcr").getRegistry().timer("connection.lifetime");
    
    private static Meter requestRate = Witchcraft.get().source("status").getRegistry().meter("request.rate");
    
    private static Meter requestBadRate = Witchcraft.get().source("status").getRegistry().meter("request.bad.rate");
    
    private static Meter requestUnknownRate = Witchcraft.get().source("status").getRegistry().meter("request.unknown.rate");
    
    //
    
    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;
    
    private Thread runner = null;
    
    private Channel serverChannel;
    
    private final int port;
    
    private List<StatusHandler> handlers = new LinkedList<StatusHandler>();
    
    public StatusServer(int port)
    {
        super();
        this.port = port;
    }
    
    public void registerHandler(StatusHandler handler)
    {
        this.handlers.add(handler);
    }
    
    public StatusHandler matchHandler(String requestPath)
    {
        for (StatusHandler handler : this.handlers)
        {
            if (handler.matches(requestPath))
            {
                return handler;
            }
        }
        return null;
    }
    
    public void run()
    {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try
        {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);
            b.option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(false));
            b.childHandler(new ChannelInitializer<SocketChannel>()
            {
                @Override
                public void initChannel(SocketChannel ch) throws Exception
                {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("read-timeout",  new ReadTimeoutHandler(  90 /* seconds */ )); 
                    pipeline.addLast("write-timeout", new WriteTimeoutHandler( 90 /* seconds */ ));
                    pipeline.addLast("codec-http",    new HttpServerCodec());
                    pipeline.addLast("aggregator",    new HttpObjectAggregator(1024 * 1024));
                    pipeline.addLast("handler",       new StatsServerHandler());
                }
            });
            //
            this.serverChannel = b.bind(this.port).sync().channel();
            logger.info("Status HTTP server started on port " + this.port + '.');
            // await the server to stop
            this.serverChannel.closeFuture().sync();
            // log
            logger.info("Status server has shutdown");
        }
        catch (Exception e)
        {
            logger.fatal("Status server broke", e);
        }
        finally
        {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    
    public void start()
    {
        if (this.runner == null)
        {
            this.runner = new Thread(this);
            this.runner.start();
        }
    }
    
    public void stop()
    {
        this.serverChannel.close().awaitUninterruptibly();
    }
    
    private class StatsServerHandler extends SimpleChannelInboundHandler<FullHttpRequest>
    {
        private Timer.Context connectionTimer;
        
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception
        {
            activeConnections.inc();
            connectionOpenRate.mark();
            this.connectionTimer = connectionLifetime.time();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception
        {
            activeConnections.dec();
            connectionCloseRate.mark();
            this.connectionTimer.stop();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
        {
            connectionErrorRate.mark();
            logger.warn("Error processing status request " + ctx.channel().remoteAddress(), cause);
            ctx.close();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception
        {
            // validate the request
            if (! req.decoderResult().isSuccess())
            {
                requestBadRate.mark();
                sendHttpResponse(ctx, req, BAD_REQUEST, "Bad Request", true);
                return;
            }
            // execute the stats handler
            String path = req.uri();
            if (path == null || path.length() == 0) path = "/";
            // lookup the handler
            StatusHandler handler = matchHandler(path);
            if (handler != null)
            {
                requestRate.mark();
                sendHttpResponse(ctx, req, handler.execute(req), true);
            }
            else
            {
                requestUnknownRate.mark();
                sendHttpResponse(ctx, req, NOT_FOUND, "Not Found", true);
            }
        }
        
        private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status, String message, boolean forceClose)
        {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer(message, CharsetUtil.UTF_8)), forceClose);
        }
        
        private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res, boolean forceClose)
        {
            // set some headers
            HttpUtil.setContentLength(res, res.content().readableBytes());
            res.headers().set(HttpHeaderNames.HOST, req.headers().get(HttpHeaderNames.HOST));
            // Send the response and close the connection if necessary.
            ChannelFuture f = ctx.writeAndFlush(res);
            if (forceClose) f.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
