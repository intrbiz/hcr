package com.intrbiz.hcr.stats;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.codahale.metrics.Timer;
import com.intrbiz.gerald.witchcraft.Witchcraft;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public abstract class StatusHandler
{
    private final String name;
    
    private final Pattern path;
    
    private final Timer handlerRuntime;
    
    public StatusHandler(String name, String pathPattern)
    {
        super();
        this.name = name;
        this.path = Pattern.compile(pathPattern);
        this.handlerRuntime = Witchcraft.get().source("status").getRegistry().timer("handler.[" + this.name + "].runtime");
    }
    
    public String getName()
    {
        return this.name;
    }
    
    public Pattern getPath()
    {
        return this.path;
    }
    
    /**
     * Does this handler match the given request path
     */
    public boolean matches(String requestPath)
    {
        return this.path.matcher(requestPath).matches();
    }
    
    /**
     * Extract any groups in the path pattern as arguments for the handler
     * @param requestPath the reqeust.uri()
     * @return An array of any capturing group values
     */
    public String[] getPathArgument(String requestPath)
    {
        Matcher pathMatcher = this.getPath().matcher(requestPath);
        String[] arguments = new String[pathMatcher.groupCount()];
        if (pathMatcher.matches())
        {
            for (int i = 0; i < arguments.length; i++)
            {
                try
                {
                    String value = pathMatcher.group(i + 1);
                    if (value != null && value.length() > 0)
                    {
                        arguments[i] = value;
                    }
                }
                catch (IllegalStateException e)
                {
                }
            }
        }
        return arguments;
    }
    
    public final FullHttpResponse execute(FullHttpRequest request)
    {
        try (Timer.Context tctx = this.handlerRuntime.time())
        {
            return this.process(request);
        }
    }
    
    protected abstract FullHttpResponse process(FullHttpRequest request);
}
