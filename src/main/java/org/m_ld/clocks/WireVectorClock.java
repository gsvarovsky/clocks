package org.m_ld.clocks;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;

public class WireVectorClock<PID> implements VectorClock<PID>
{
    private final PID pid;
    private final Map<PID, Long> vector;

    private WireVectorClock(PID pid, Map<PID, Long> vector)
    {
        this.pid = pid;
        this.vector = vector;
    }

    public static <PID> WireVectorClock<PID> clock(PID pid, long ticks)
    {
        return new WireVectorClock<>(pid, singletonMap(pid, ticks));
    }

    public WireVectorClock<PID> with(PID pid, long ticks)
    {
        final HashMap<PID, Long> vector = new HashMap<>(this.vector);
        vector.put(pid, ticks);
        return new WireVectorClock<>(this.pid, vector);
    }

    @Override
    public PID processId()
    {
        return pid;
    }

    @Override
    public Map<PID, Long> vector()
    {
        return vector;
    }
}
