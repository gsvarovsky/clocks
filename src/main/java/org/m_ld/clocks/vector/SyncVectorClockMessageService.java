/*
 * Copyright (c) George Svarovsky 2020. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

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
        final PID thatPid = newPid.get();
        vector.put(thatPid, 0L);
        return clock(thatPid, vector);
    }
}
