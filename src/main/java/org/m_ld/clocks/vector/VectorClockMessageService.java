/*
 * Copyright (c) George Svarovsky 2019. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package org.m_ld.clocks.vector;

import org.m_ld.clocks.Message;
import org.m_ld.clocks.MessageService;

import java.util.Queue;
import java.util.function.Consumer;

import static org.m_ld.clocks.vector.WireVectorClock.clock;

/**
 * Provides default Message Service functions to maintain state as the local process's vector clock.
 * Since the methods {@link #send()} and {@link #receive(Message, Queue, Consumer)} mutate the clock state,
 * implementors must ensure these methods are atomically applied in a concurrent environment.
 * <p>
 * Vector clock assumptions (after <a href="https://www.cl.cam.ac.uk/teaching/0910/ConcDistS/10b-ProcGp-order.pdf">
 *     Concurrent and Distributed Systems 2009–10, Process groups and message ordering</a>):<ul>
 * <li>messages are multicast to named process groups</li>
 * <li>reliable channels: a given message is delivered reliably to all members of the group</li>
 * <li>FIFO from a given source to a given destination</li>
 * <li>processes don’t crash (failure and restart not considered)</li>
 * <li>no Byzantine behaviour</li>
 * </ul>
 */
public abstract class VectorClockMessageService<PID> extends MessageService<VectorClock<PID>> implements VectorClock<PID>
{
    @Override
    public VectorClock<PID> peek()
    {
        return clock(processId(), vector());
    }

    @Override
    public void event()
    {
        //noinspection ConstantConditions
        vector().compute(processId(), (pid, ticks) -> ticks + 1);
    }

    @Override
    public void join(VectorClock<PID> metadata)
    {
        // update the other fields of the vector by comparing its values with the incoming vector (timestamp)
        // and recording the higher value in each field, thus updating this process’s knowledge of system state
        metadata.vector().forEach((pid, ticks) -> vector().merge(pid, ticks, Math::max));
    }
}
