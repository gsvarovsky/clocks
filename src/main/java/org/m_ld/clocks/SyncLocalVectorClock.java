package org.m_ld.clocks;

import java.util.HashMap;
import java.util.Map;

public class SyncLocalVectorClock<PID> extends LocalVectorClock<PID>
{
    private final PID pid;
    private final Map<PID, Long> vector = new HashMap<>();

    SyncLocalVectorClock(PID pid)
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
