package org.m_ld.clocks.vector;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.m_ld.clocks.vector.WireVectorClock.clock;

/**
 * A {@link VectorClockMessageService} which uses a non-thread-safe {@link HashMap}, to be used
 * in processes that themselves handle thread-safe access to their local clocks.
 */
public class SyncVectorClockMessageService<PID> extends VectorClockMessageService<PID>
{
    private final PID pid;
    private final Supplier<PID> newPid;
    private final Map<PID, Long> vector = new HashMap<>();

    public SyncVectorClockMessageService(Supplier<PID> newPid)
    {
        this.pid = newPid.get();
        this.vector.put(pid, 0L);
        this.newPid = newPid;
    }

    public SyncVectorClockMessageService(VectorClock<PID> time, Supplier<PID> newPid)
    {
        this.pid = time.processId();
        this.vector.putAll(time.vector());
        this.newPid = newPid;
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

    @Override public VectorClock<PID> fork()
    {
        return clock(newPid.get(), 0L); // Every vector clock identity is brand-new
    }
}
