package com.intrbiz.hcr.task;

import java.io.Serializable;
import java.util.concurrent.Callable;

import com.intrbiz.hcr.model.MemberMetadata;

public class MemberMetadataTask implements Callable<MemberMetadata>, Serializable
{
    private static final long serialVersionUID = 1L;

    @Override
    public MemberMetadata call() throws Exception
    {
        return MemberMetadata.fromRuntime();
    }
}
