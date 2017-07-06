package com.intrbiz.hcr.commands;

import com.intrbiz.hcr.command.Command;
import com.intrbiz.hcr.command.CommandContext;

public class QuitCommand extends Command
{
    public QuitCommand()
    {
        super("quit", 0, new String[] {}, 0, 0, 0);
    }

    @Override
    public void process(CommandContext ctx)
    {
        ctx.writeSimpleStringAndClose("OK");
    }
}
