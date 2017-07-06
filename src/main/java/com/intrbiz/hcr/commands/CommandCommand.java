package com.intrbiz.hcr.commands;

import com.intrbiz.hcr.command.Command;
import com.intrbiz.hcr.command.CommandContext;

public class CommandCommand extends Command
{
    public CommandCommand()
    {
        super("command", 0, new String[] {}, 0, 0, 0);
    }

    @Override
    public void process(CommandContext ctx)
    {
        ctx.write(ctx.processor().describe());
    }
}
