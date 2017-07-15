package com.intrbiz.hcr.stats;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public abstract class StatsHandler
{
    private final String path;
    
    public StatsHandler(String path)
    {
        super();
        this.path = path;
    }
    
    public String getPath()
    {
        return this.path;
    }
    
    public abstract FullHttpResponse process(FullHttpRequest request);
}
