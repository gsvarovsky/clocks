package org.m_ld.clocks.vector;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link LocalVectorClock} which uses a non-thread-safe {@link HashMap}, to be used
 * in processes that themselves handle thread-safe access to their local clocks.
 */
public class SyncLocalVectorClock<PID> extends LocalVectorClock<PID>
{
    private final PID pid;
    private final Map<PID, Long> vector = new HashMap<>();

    public SyncLocalVectorClock(PID pid)
    {
        this.pid = pid;
        this.vector.put(pid, 0L);
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
