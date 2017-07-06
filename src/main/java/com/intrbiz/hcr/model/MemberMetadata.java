package com.intrbiz.hcr.model;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public class MemberMetadata implements Serializable
{
    private static final long serialVersionUID = 1L;

    private long maxMemory = 0L;

    private long totalMemory = 0L;

    private long freeMemory = 0L;

    private int cpuCount = 0;

    private long uptime;

    public MemberMetadata()
    {
        super();
    }

    public long getMaxMemory()
    {
        return maxMemory;
    }

    public void setMaxMemory(long maxMemory)
    {
        this.maxMemory = maxMemory;
    }

    public long getTotalMemory()
    {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory)
    {
        this.totalMemory = totalMemory;
    }

    public long getFreeMemory()
    {
        return freeMemory;
    }

    public void setFreeMemory(long freeMemory)
    {
        this.freeMemory = freeMemory;
    }

    public int getCpuCount()
    {
        return cpuCount;
    }

    public void setCpuCount(int cpuCount)
    {
        this.cpuCount = cpuCount;
    }

    public long getUptime()
    {
        return uptime;
    }

    public void setUptime(long uptime)
    {
        this.uptime = uptime;
    }

    public void add(MemberMetadata other)
    {
        this.maxMemory += other.maxMemory;
        this.totalMemory += other.totalMemory;
        this.freeMemory += other.freeMemory;
        this.cpuCount += other.cpuCount;
    }

    public static MemberMetadata fromRuntime()
    {
        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
        // fill in
        MemberMetadata meta = new MemberMetadata();
        meta.setTotalMemory(runtime.totalMemory());
        meta.setMaxMemory(runtime.maxMemory());
        meta.setFreeMemory(runtime.freeMemory());
        meta.setCpuCount(runtime.availableProcessors());
        meta.setUptime(mxBean.getUptime());
        return meta;
    }
}
