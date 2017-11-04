package com.intrbiz.hcr.command;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.intrbiz.gerald.witchcraft.Witchcraft;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.redis.ArrayRedisMessage;
import io.netty.handler.codec.redis.FullBulkStringRedisMessage;
import io.netty.handler.codec.redis.IntegerRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;
import io.netty.handler.codec.redis.SimpleStringRedisMessage;
import io.netty.util.CharsetUtil;

public abstract class Command
{
    private final String name;
    
    private final int arity;
    
    private final String[] flags;
    
    private final int firstKey;
    
    private final int lastKey;
    
    private final int stepCount;
    
    private final Timer commandRuntime;
    
    private final Meter commandErrorRate;

    protected Command(String name, int arity, String[] flags, int firstKey, int lastKey, int stepCount)
    {
        super();
        this.name = name.toLowerCase();
        this.arity = arity;
        this.flags = flags;
        this.firstKey = firstKey;
        this.lastKey = lastKey;
        this.stepCount = stepCount;
        this.commandRuntime = Witchcraft.get().source("hcr").getRegistry().timer("command.[" + name + "].runtime");
        this.commandErrorRate = Witchcraft.get().source("hcr").getRegistry().meter("command.[" + name + "].error.rate");
    }

    public String getName()
    {
        return name;
    }

    public int getArity()
    {
        return arity;
    }

    public String[] getFlags()
    {
        return flags;
    }

    public int getFirstKey()
    {
        return firstKey;
    }

    public int getLastKey()
    {
        return lastKey;
    }

    public int getStepCount()
    {
        return stepCount;
    }
    
    public ArrayRedisMessage describe()
    {
        List<RedisMessage> desc = new LinkedList<RedisMessage>();
        desc.add(new FullBulkStringRedisMessage(Unpooled.copiedBuffer(this.name, CharsetUtil.UTF_8)));
        desc.add(new IntegerRedisMessage(this.arity));
        desc.add(new ArrayRedisMessage(Arrays.stream(this.flags)
                .map((f) -> new SimpleStringRedisMessage(f))
                .collect(Collectors.toList())
        ));
        desc.add(new IntegerRedisMessage(this.firstKey));
        desc.add(new IntegerRedisMessage(this.lastKey));
        desc.add(new IntegerRedisMessage(this.stepCount));
        return new ArrayRedisMessage(desc);
    }
    
    /**
     * Mark that this command errored
     */
    protected void markError()
    {
        this.commandErrorRate.mark();
    }
    
    /**
     * Execute this command
     */
    public final void execute(CommandContext ctx)
    {
        try (Timer.Context tctx = this.commandRuntime.time())
        {
            this.process(ctx);
        }
    }
    
    /**
     * Do the actual processing of this command
     */
    protected abstract void process(CommandContext ctx);
}
