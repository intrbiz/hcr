package com.intrbiz.hcr.commands;

import com.intrbiz.hcr.command.Command;
import com.intrbiz.hcr.command.CommandContext;

public class PingCommand extends Command
{
    public PingCommand()
    {
        super("ping", 0, new String[] {}, 0, 0, 0);
    }

    @Override
    public void process(CommandContext ctx)
    {
        ctx.writeSimpleString("PONG");
    }
}
