/*
 * Copyright (c) George Svarovsky 2019. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package org.example;

import org.m_ld.clocks.vector.SyncVectorClockMessageService;
import org.m_ld.clocks.vector.VectorClock;

import java.util.UUID;

public class VectorClockOrSetProcessTest extends
    OrSetProcessTest<VectorClock<UUID>, OrSetProcess<VectorClock<UUID>, Integer>>
{
    public OrSetProcess<VectorClock<UUID>, Integer> createProcess()
    {
        return new OrSetProcess<>(new SyncVectorClockMessageService<>(UUID::randomUUID));
    }
}
