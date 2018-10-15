package org.m_ld.clocks;

import java.util.Iterator;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Provides default Message Service functions to maintain delivery with ordering guarantees.
 * <p>
 * The terminology here is after <a href="http://gsd.di.uminho.pt/members/cbm/ps/itc2008.pdf">
 * Interval Tree Clocks: A Logical Clock for Dynamic Systems</a>, see README.md
 *
 * @param <C> The message clock type to be used
 */
public abstract class MessageService<C extends CausalClock<C>>
{
    /**
     * Call before sending this clock's state attached to a message.
     * Returns an immutable snapshot of time suitable for attachment to a message.
     */
    public C send()
    {
        event();
        return peek();
    }

    /**
     * Call to process a newly received message from the wire.
     *
     * @param message the message from the wire
     * @param buffer  a buffer for out-of-order messages
     * @param process the local message consumer, which will receive messages in order
     * @return <code>false</code> iff the buffer is full
     */
    public <D, M extends Message<C, D>> boolean receive(
        M message, Queue<M> buffer, Consumer<? super D> process)
    {
        if (readyFor(message.time()))
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
     * Call to deliver a message from the wire, irrespective of whether the service
     * is ready for them. Use to deliver messages for which the cause is not important
     * or has already been determined.
     *
     * @param message the message from the wire
     * @param buffer  a buffer of messages that might be caused by the delivered message.
     *                Must implement {@link Iterator#remove()}.
     * @param process the local message consumer, which will receive messages in order
     */
    private <D, M extends Message<C, D>> void deliver(
        M message, Iterable<M> buffer, Consumer<? super D> process)
    {
        process.accept(message.data());

        join(message.time());

        // increment receiving process’s state value in its local vector
        event();

        // reconsider buffered messages
        for (Iterator<M> bufferIter = buffer.iterator(); bufferIter.hasNext(); )
        {
            final M next = bufferIter.next();
            if (readyFor(next.time()))
            {
                bufferIter.remove();
                // Recurse to start the iteration again on the modified buffer
                deliver(next, buffer, process);
                break;
            }
        }
    }

    /**
     * Resets the time. Expected to be used once on initialisation; if used otherwise, may cause irrecoverable loss of
     * causality.
     *
     * @param time the time to reset this service to
     */
    public abstract void reset(C time);

    /**
     * @return an immutable snapshot of time suitable for attachment to a message.
     */
    public abstract C peek();

    /**
     * Adds a new event, i.e. increments local process clock value
     */
    protected abstract void event();

    /**
     * Merges the given time into our local state
     *
     * @param time the new time to merge
     */
    protected abstract void join(C time);

    /**
     * The basic determinant of whether we can deliver a message with the given time.
     *
     * @param senderTime an incoming message's time
     * @return <code>true</code> if our current clock state has all required history for the given time
     */
    private synchronized boolean readyFor(C senderTime)
    {
        // do the sender and receiver agree on the state of all other processes?
        // If the sender has a higher state value for any of these others, the receiver is missing
        // a message so buffer the message
        return !peek().anyLt(senderTime);
    }
}
