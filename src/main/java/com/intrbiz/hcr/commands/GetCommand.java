package com.intrbiz.hcr.commands;

import com.intrbiz.hcr.command.Command;
import com.intrbiz.hcr.command.CommandContext;

public class GetCommand extends Command
{
    public GetCommand()
    {
        super("get", 1, new String[] {}, 1, 1, 0);
    }

    @Override
    public void process(CommandContext ctx)
    {
        if (ctx.hasArguments(1))
        {
            String key = ctx.getStringArgument(0);
            if (key != null && key.length() > 0)
            {
                ctx.writeBulkString(ctx.processor().getData().get(key));
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
