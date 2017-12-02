package com.intrbiz.hcr;

public class Util
{
    public static final String coalesceEmpty(String... strings)
    {
        for (String s : strings)
        {
            if (s != null && s.length() > 0)
                return s;
        }
        return null;
    }
}
