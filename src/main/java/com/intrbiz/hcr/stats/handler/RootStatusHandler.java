package com.intrbiz.hcr.stats.handler;

import static io.netty.handler.codec.http.HttpVersion.*;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.Member;
import com.intrbiz.hcr.HCR;
import com.intrbiz.hcr.command.CommandProcessor;
import com.intrbiz.hcr.model.MemberMetadata;
import com.intrbiz.hcr.stats.StatusHandler;
import com.intrbiz.hcr.task.MemberMetadataTask;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

public class RootStatusHandler extends StatusHandler
{
    private final CommandProcessor processor;
    
    public RootStatusHandler(CommandProcessor processor)
    {
        super("root", "/");
        this.processor = processor;
    }

    @Override
    public FullHttpResponse process(FullHttpRequest request)
    {
        StringBuilder m = new StringBuilder("{\r\n");
        m.append("\"version\": \"").append(HCR.VERSION).append("\"").append(",\r\n");
        m.append("\"clients\": ").append(this.processor.getClientCount()).append(",\r\n");
        m.append("\"keys\": ").append(this.processor.getData().size()).append(",\r\n");
        // cluster info
        MemberMetadata total = new MemberMetadata();
        Cluster cluster = this.processor.getHazelcast().getCluster();
        Map<Member, Future<MemberMetadata>> metadataFutures = this.processor.getExecutor().submitToAllMembers(new MemberMetadataTask());
        m.append("\"members\": [").append("\r\n");
        boolean ns = false;
        for (Member member : cluster.getMembers())
        {
            if (ns) m.append(",\r\n");
            m.append(" {\r\n");
            m.append("    \"id\": \"").append(member.getUuid()).append("\",\r\n");
            m.append("    \"address\": \"").append(member.getAddress()).append("\",\r\n");
            Future<MemberMetadata> future = metadataFutures.get(member);
            if (future != null)
            {
                try
                {
                    MemberMetadata meta = future.get(2, TimeUnit.SECONDS);
                    total.add(meta);
                    m.append("    \"max_memory\": ").append(meta.getMaxMemory()).append(",\r\n");
                    m.append("    \"total_memory\": ").append(meta.getTotalMemory()).append(",\r\n");
                    m.append("    \"free_memory\": ").append(meta.getFreeMemory()).append(",\r\n");
                    m.append("    \"cpu_count\": ").append(meta.getCpuCount()).append(",\r\n");
                    m.append("    \"uptime\": ").append(meta.getUptime()).append("\r\n");
                }
                catch (Exception e)
                {
                }
            }
            m.append(" }");
            ns = true;
        }
        m.append("\r\n");
        m.append("],\r\n");
        m.append("\"max_memory\": ").append(total.getMaxMemory()).append(",\r\n");
        m.append("\"total_memory\": ").append(total.getTotalMemory()).append(",\r\n");
        m.append("\"free_memory\": ").append(total.getFreeMemory()).append("\r\n");
        m.append("}\r\n");
        // response
        return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(m.toString(), CharsetUtil.UTF_8));
    }
    
}
