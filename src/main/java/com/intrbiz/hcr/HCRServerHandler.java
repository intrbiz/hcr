package com.intrbiz.hcr;

import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.intrbiz.gerald.witchcraft.Witchcraft;
import com.intrbiz.hcr.command.CommandProcessor;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.redis.ArrayRedisMessage;
import io.netty.handler.codec.redis.ErrorRedisMessage;
import io.netty.handler.codec.redis.FullBulkStringRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;
import io.netty.handler.codec.redis.SimpleStringRedisMessage;
import io.netty.util.CharsetUtil;

public class HCRServerHandler extends SimpleChannelInboundHandler<Object>
{
    private static Logger logger = Logger.getLogger(HCRServerHandler.class);
    
    // metrics
    
    private static Counter activeConnections = Witchcraft.get().source("hcr").getRegistry().counter("active.connections");
    
    private static Meter connectionOpenRate = Witchcraft.get().source("hcr").getRegistry().meter("connection.open.rate");
    
    private static Meter connectionCloseRate = Witchcraft.get().source("hcr").getRegistry().meter("connection.close.rate");
    
    private static Meter connectionIdleTimeoutRate = Witchcraft.get().source("hcr").getRegistry().meter("connection.idle.timeout.rate");
    
    private static Meter connectionErrorRate = Witchcraft.get().source("hcr").getRegistry().meter("connection.error.rate");
    
    private static Timer connectionLifetime = Witchcraft.get().source("hcr").getRegistry().timer("connection.lifetime");
    
    private static Meter commandRate = Witchcraft.get().source("hcr").getRegistry().meter("command.rate");
    
    private static Meter commandBadRate = Witchcraft.get().source("hcr").getRegistry().meter("command.bad.rate");
    
    //
    
    private final CommandProcessor processor;
    
    private Timer.Context connectionTimer;

    public HCRServerHandler(CommandProcessor processor)
    {
        super();
        this.processor = processor;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        activeConnections.inc();
        connectionOpenRate.mark();
        this.connectionTimer = connectionLifetime.time();
        logger.info("Connect from " + ctx.channel().remoteAddress());
        this.processor.clientConnected();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        activeConnections.dec();
        connectionCloseRate.mark();
        this.connectionTimer.stop();
        logger.info("Disconnect from " + ctx.channel().remoteAddress());
        this.processor.clientDisconnected();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof ArrayRedisMessage)
        {
            ArrayRedisMessage m = (ArrayRedisMessage) msg;
            if (m.children().size() > 0)
            {
                RedisMessage command = m.children().remove(0);
                if (command instanceof FullBulkStringRedisMessage)
                {
                    commandRate.mark();
                    this.processor.process(ctx, ((FullBulkStringRedisMessage) command).content().toString(CharsetUtil.UTF_8), m.children());
                }
                else if (command instanceof SimpleStringRedisMessage)
                {
                    commandRate.mark();
                    this.processor.process(ctx, ((SimpleStringRedisMessage) command).content(), m.children());
                }
                else
                {
                    commandBadRate.mark();
                    ctx.writeAndFlush(new ErrorRedisMessage("Bad command")).addListener(ChannelFutureListener.CLOSE);
                }
            }
            else
            {
                commandBadRate.mark();
                ctx.writeAndFlush(new ErrorRedisMessage("Bad command")).addListener(ChannelFutureListener.CLOSE);
            }
        }
        else
        {
            commandBadRate.mark();
            ctx.writeAndFlush(new ErrorRedisMessage("Bad command")).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        if (cause instanceof TimeoutException)
        {
            connectionIdleTimeoutRate.mark();
            logger.debug("Closing idle connection: " + ctx.channel().remoteAddress());
        }
        else
        {
            connectionErrorRate.mark();
            logger.warn("Error processing request for client " + ctx.channel().remoteAddress() + ", error: " + cause);
        }
        ctx.close();
    }
}
