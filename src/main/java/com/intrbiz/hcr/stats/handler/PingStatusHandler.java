package com.intrbiz.hcr.stats.handler;

import static com.intrbiz.hcr.Util.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import java.io.IOException;

import com.intrbiz.hcr.client.RESPSocket;
import com.intrbiz.hcr.stats.StatusHandler;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

public class PingStatusHandler extends StatusHandler
{    
    public PingStatusHandler()
    {
        super("ping", "/ping");
    }

    @Override
    public FullHttpResponse process(FullHttpRequest request)
    {
        // get the host and port of ourself to connect to
        String host = coalesceEmpty(System.getProperty("hcr.host"), System.getenv("hcr_host"), "127.0.0.1");
        int port = Integer.parseInt(coalesceEmpty(System.getProperty("hcr.port"), System.getenv("hcr_port"), "6379"));
        // connect to ourself and send a ping
        HttpResponseStatus status = HttpResponseStatus.OK;
        StringBuilder message = new StringBuilder();
        try (RESPSocket socket = new RESPSocket(host, port))
        {
            message.append("PING: ").append(socket.ping()).append("\r\n");
            socket.quit();
        }
        catch (IOException e)
        {
            status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
            message.append("FAILED\r\nERROR: ").append(e.getMessage()).append("\r\n");
        }
        return new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer(message, CharsetUtil.UTF_8));
    }
    
}
