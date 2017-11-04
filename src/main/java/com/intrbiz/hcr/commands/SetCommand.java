package com.intrbiz.hcr.commands;

import com.intrbiz.hcr.command.Command;
import com.intrbiz.hcr.command.CommandContext;

public class SetCommand extends Command
{
    public SetCommand()
    {
        super("set", 1, new String[] {}, 1, 1, 0);
    }

    @Override
    public void process(CommandContext ctx)
    {
        if (ctx.hasAtLeastArguments(2))
        {
            String key = ctx.getStringArgument(0);
            String value = ctx.getStringArgument(1);
            // set the key
            if (key != null && key.length() > 0)
            {
                ctx.processor().getData().put(key, value);
                ctx.writeSimpleString("OK");
            }
            else
            {
                this.markError();
                ctx.writeError("Invalid arguments");
            }
        }
        else
        {
            this.markError();
            ctx.writeError("Invalid arguments");
        }
    }
}
