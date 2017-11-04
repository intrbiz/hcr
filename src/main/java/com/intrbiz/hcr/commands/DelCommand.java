package com.intrbiz.hcr.commands;

import com.intrbiz.hcr.command.Command;
import com.intrbiz.hcr.command.CommandContext;

public class DelCommand extends Command
{
    public DelCommand()
    {
        super("del", 1, new String[] {}, 1, 1, 1);
    }

    @Override
    public void process(CommandContext ctx)
    {
        if (ctx.hasAtLeastArguments(1))
        {
            int count = 0;
            for (int i = 0; i < ctx.arguments().size(); i++)
            {
                String key = ctx.getStringArgument(i);
                if (ctx.processor().getData().remove(key) != null)
                    count++;
            }
            ctx.writeInteger(count);
        }
        else
        {
            this.markError();
            ctx.writeError("Invalid arguments");
        }
    }
}
