package com.intrbiz.hcr.commands;

import java.util.stream.Collectors;

import com.intrbiz.hcr.command.Command;
import com.intrbiz.hcr.command.CommandContext;

import io.netty.handler.codec.redis.ArrayRedisMessage;
import io.netty.handler.codec.redis.SimpleStringRedisMessage;

public class KeysCommand extends Command
{
    public KeysCommand()
    {
        super("keys", 0, new String[] {}, 0, 0, 0);
    }

    @Override
    public void process(CommandContext ctx)
    {
       ctx.write(new ArrayRedisMessage(
               ctx.processor().getData().keySet().stream()
               .map((k) -> new SimpleStringRedisMessage(k))
               .collect(Collectors.toList())
       ));
    }
}
