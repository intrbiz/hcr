package com.intrbiz.hcr.commands;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.Member;
import com.intrbiz.hcr.command.Command;
import com.intrbiz.hcr.command.CommandContext;
import com.intrbiz.hcr.model.MemberMetadata;
import com.intrbiz.hcr.task.MemberMetadataTask;

public class InfoCommand extends Command
{
    public InfoCommand()
    {
        super("info", 0, new String[] {}, 0, 0, 0);
    }

    @Override
    public void process(CommandContext ctx)
    {
        StringBuilder m = new StringBuilder("{\r\n");
        m.append("\"version\": \"0.0.1\"").append(",\r\n");
        m.append("\"clients\": ").append(ctx.processor().getClientCount()).append(",\r\n");
        m.append("\"keys\": ").append(ctx.processor().getData().size()).append(",\r\n");
        // cluster info
        MemberMetadata total = new MemberMetadata();
        Cluster cluster = ctx.processor().getHazelcast().getCluster();
        Map<Member, Future<MemberMetadata>> metadataFutures = ctx.processor().getExecutor().submitToAllMembers(new MemberMetadataTask());
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
                    m.append("    \"uptime\": ").append(meta.getUptime()).append(",\r\n");
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
        ctx.writeBulkString(m.toString());
    }
}
