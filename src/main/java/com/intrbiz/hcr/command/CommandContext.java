package com.intrbiz.hcr.command;

import java.util.List;
import java.util.Objects;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.redis.ErrorRedisMessage;
import io.netty.handler.codec.redis.FullBulkStringRedisMessage;
import io.netty.handler.codec.redis.IntegerRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;
import io.netty.handler.codec.redis.SimpleStringRedisMessage;
import io.netty.util.CharsetUtil;

public class CommandContext
{
    private final CommandProcessor processor;

    private final ChannelHandlerContext channelContext;

    private final String commandName;

    private final List<RedisMessage> arguments;

    public CommandContext(CommandProcessor processor, ChannelHandlerContext channelContext, String commandName, List<RedisMessage> arguments)
    {
        super();
        this.processor = processor;
        this.channelContext = channelContext;
        this.commandName = commandName;
        this.arguments = arguments;
    }

    public String commandName()
    {
        return commandName;
    }

    public List<RedisMessage> arguments()
    {
        return arguments;
    }
    
    public boolean hasArguments()
    {
        return this.arguments.size() > 0;
    }
    
    public boolean hasArguments(int size)
    {
        return this.arguments.size() == size;
    }
    
    public boolean hasAtLeastArguments(int size)
    {
        return this.arguments.size() >= size;
    }
    
    public String getStringArgument(int index)
    {
        if (index > this.arguments.size()) return null;
        RedisMessage message = this.arguments.get(index);
        if (message instanceof FullBulkStringRedisMessage)
        {
            FullBulkStringRedisMessage str = (FullBulkStringRedisMessage) message;
            return str.isNull() ? null : str.content().toString(CharsetUtil.UTF_8);
        }
        else if (message instanceof SimpleStringRedisMessage)
        {
            SimpleStringRedisMessage str = (SimpleStringRedisMessage) message;
            return str.content();
        }
        return null;
    }

    public CommandProcessor processor()
    {
        return processor;
    }

    public ChannelHandlerContext channelContext()
    {
        return channelContext;
    }

    public void writeBulkString(String string)
    {
        this.write(string == null ? FullBulkStringRedisMessage.NULL_INSTANCE : new FullBulkStringRedisMessage(Unpooled.copiedBuffer(string, CharsetUtil.UTF_8)));
    }

    public void writeSimpleString(String string)
    {
        Objects.requireNonNull(string);
        this.write(new SimpleStringRedisMessage(string));
    }
    
    public void writeInteger(long val)
    {
        this.write(new IntegerRedisMessage(val));
    }
    
    public void writeError(String error)
    {
        Objects.requireNonNull(error);
        this.write(new ErrorRedisMessage(error));
    }

    public void write(RedisMessage message)
    {
        this.channelContext.writeAndFlush(message);
    }

    public void writeSimpleStringAndClose(String string)
    {
        Objects.requireNonNull(string);
        this.writeAndClose(new SimpleStringRedisMessage(string));
    }

    public void writeErrorAndClose(String error)
    {
        this.writeAndClose(new ErrorRedisMessage(error));
    }

    public void writeAndClose(RedisMessage message)
    {
        this.channelContext.writeAndFlush(message).addListener(ChannelFutureListener.CLOSE);
        ;
    }
    
    public void log(String message)
    {
        System.out.println("Client [" + this.channelContext.channel().remoteAddress() + "] " + message);
    }
}
