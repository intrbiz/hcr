package com.intrbiz.hcr.stats.handler;

import static io.netty.handler.codec.http.HttpVersion.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map.Entry;

import com.codahale.metrics.Metric;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intrbiz.gerald.polyakov.Parcel;
import com.intrbiz.gerald.source.IntelligenceSource;
import com.intrbiz.gerald.witchcraft.Witchcraft;
import com.intrbiz.hcr.stats.StatusHandler;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

public class MetricsStatusHandler extends StatusHandler
{   
    private final ObjectMapper factory;
    
    public MetricsStatusHandler()
    {
        super("metrics", "/metrics/([^/]*)/?([^/]*)");
        this.factory = new ObjectMapper();
        this.factory.configure(SerializationFeature.INDENT_OUTPUT, true);
        this.factory.setSerializationInclusion(Include.NON_NULL);
        // add types
        this.factory.registerSubtypes(Parcel.class);
    }

    @Override
    public FullHttpResponse process(FullHttpRequest request)
    {
        String[] arguments = this.getPathArgument(request.uri());
        // snapshot all metrics and buffer as JSON
        StringWriter json = new StringWriter();
        try (JsonGenerator jenny = this.factory.getFactory().createGenerator(json))
        {
            jenny.writeStartArray();
            for (IntelligenceSource source : Witchcraft.get().getSources())
            {
                // optionally filter by source
                if (arguments[0] == null || source.getName().equals(arguments[0]))
                {
                    Parcel parcel = new Parcel(null, source.getName());
                    for (Entry<String, Metric> metric : source.getRegistry().getMetrics().entrySet())
                    {
                        // optionally filter by metric name
                        if (arguments[1] == null || metric.getKey().equals(arguments[1]))
                        {
                            parcel.addMetric(metric.getKey(), metric.getValue());
                        }
                    }
                    this.factory.writeValue(jenny, parcel);
                }
            }
            jenny.writeEndArray();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to encode metrics", e);
        }
        // response
        return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(json.toString(), CharsetUtil.UTF_8));
    }
    
}
