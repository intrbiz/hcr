package com.intrbiz.hcr;

import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

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
    private static Logger logger = Logger.getLogger(HCRServerHandler.class.getCanonicalName());
    
    private final CommandProcessor processor;

    public HCRServerHandler(CommandProcessor processor)
    {
        super();
        this.processor = processor;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        logger.info("Connect from " + ctx.channel().remoteAddress());
        this.processor.clientConnected();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
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
                    this.processor.process(ctx, ((FullBulkStringRedisMessage) command).content().toString(CharsetUtil.UTF_8), m.children());
                }
                else if (command instanceof SimpleStringRedisMessage)
                {
                    this.processor.process(ctx, ((SimpleStringRedisMessage) command).content(), m.children());
                }
                else
                {
                    ctx.writeAndFlush(new ErrorRedisMessage("Bad command")).addListener(ChannelFutureListener.CLOSE);
                }
            }
            else
            {
                ctx.writeAndFlush(new ErrorRedisMessage("Bad command")).addListener(ChannelFutureListener.CLOSE);
            }
        }
        else
        {
            ctx.writeAndFlush(new ErrorRedisMessage("Bad command")).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        if (cause instanceof TimeoutException)
        {
            logger.info("Closing idle connection: " + ctx.channel().remoteAddress());
        }
        else
        {
            logger.warning("Error processing request for client " + ctx.channel().remoteAddress() + ", error: " + cause);
        }
        ctx.close();
    }
}
