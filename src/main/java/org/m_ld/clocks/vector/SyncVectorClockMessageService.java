package org.m_ld.clocks.vector;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;

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
        this(pid, singletonMap(pid, 0L));
    }

    public SyncVectorClockMessageService(PID pid, Map<PID, Long> vector)
    {
        this.pid = pid;
        this.vector.putAll(vector);
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
