package org.m_ld.clocks.vector;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link VectorClockMessageService} which uses a non-thread-safe {@link HashMap}, to be used
 * in processes that themselves handle thread-safe access to their local clocks.
 */
public class SyncVectorClockMessageService<PID> extends VectorClockMessageService<PID>
{
    private final PID pid;
    private final Map<PID, Long> vector = new HashMap<>();

    public SyncVectorClockMessageService(PID pid)
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

    @Override public void reset(VectorClock<PID> time)
    {
        if (!pid.equals(time.processId()))
            throw new IllegalArgumentException("Cannot change the process identity of a vector clock");

        vector.clear();
        vector.putAll(time.vector());
    }
}
