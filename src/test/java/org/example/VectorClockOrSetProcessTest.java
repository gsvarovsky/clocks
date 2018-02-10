package org.example;

import org.m_ld.clocks.vector.SyncLocalVectorClock;
import org.m_ld.clocks.vector.VectorClock;

import java.util.UUID;

public class VectorClockOrSetProcessTest extends OrSetProcessTest<VectorClock<UUID>>
{
    public OrSetProcess<VectorClock<UUID>, Integer> createProcess()
    {
        return new OrSetProcess<>(new SyncLocalVectorClock<>(UUID.randomUUID()));
    }
}
