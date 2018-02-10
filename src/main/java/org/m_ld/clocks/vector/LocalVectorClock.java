package org.m_ld.clocks.vector;

import org.m_ld.clocks.Message;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;

import static org.m_ld.clocks.vector.WireVectorClock.clock;

/**
 * Provides default Message Service functions to maintain correct state as the local process's vector clock.
 * Since the methods {@link #onSend()} and {@link #onReceive(Message, Queue, Consumer)} mutate the clock state,
 * implementors must ensure these methods are atomically applied in a concurrent environment.
 */
public abstract class LocalVectorClock<PID> extends AbstractVectorClock<PID>
{
    /**
     * Call before sending this clock's state attached to a message.
     * Returns an immutable snapshot of this vector clock suitable for attachment to a message.
     */
    public VectorClock<PID> onSend()
    {
        // increment local process state value in local vector
        increment();
        return snapshot();
    }

    /**
     * Call to correctly process a newly received message from the wire.
     * @param message the message from the wire
     * @param buffer a buffer for out-of-order messages
     * @param process the local message consumer, which will receive messages in causal order
     * @return <code>false</code> iff the buffer is full
     */
    public <D, M extends Message<? extends VectorClock<PID>, D>> boolean onReceive(
        M message, Queue<M> buffer, Consumer<D> process)
    {
        if (canDeliver(message))
        {
            deliver(message, buffer, process);
            return true;
        }
        else
        {
            return buffer.offer(message);
        }
    }

    /**
     * @return an immutable snapshot of this vector clock.
     */
    public VectorClock<PID> snapshot()
    {
        return clock(processId(), vector());
    }

    private void increment()
    {
        vector().compute(processId(), (pid, ticks) -> ticks + 1);
    }

    private boolean canDeliver(Message<? extends VectorClock<PID>, ?> message)
    {
        // do the sender and receiver agree on the state of all other processes?
        // If the sender has a higher state value for any of these others, the receiver is missing
        // a message so buffer the message
        return message.metadata().vector().entrySet().stream()
            .filter(e -> !pid(e).equals(processId()) && !pid(e).equals(message.metadata().processId()))
            .noneMatch(e -> e.getValue() > ticks(pid(e)));
    }

    private <D, M extends Message<? extends VectorClock<PID>, D>> void deliver(
        M message, Iterable<M> buffer, Consumer<D> process)
    {
        process.accept(message.data());

        // increment receiving process’s state value in its local vector
        increment();

        // update the other fields of the vector by comparing its values with the incoming vector (timestamp)
        // and recording the higher value in each field, thus updating this process’s knowledge of system state
        message.metadata().vector().forEach((pid, ticks) -> vector().merge(pid, ticks, Math::max));

        // reconsider buffered messages
        for (Iterator<M> bufferIter = buffer.iterator(); bufferIter.hasNext(); )
        {
            final M next = bufferIter.next();
            if (canDeliver(next))
            {
                bufferIter.remove();
                // Recurse to start the iteration again on the modified buffer
                deliver(next, buffer, process);
                break;
            }
        }
    }

    private static <PID> PID pid(Map.Entry<PID, Long> e)
    {
        return e.getKey();
    }
}
