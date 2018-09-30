package org.m_ld.clocks;

import java.util.Iterator;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Provides default Message Service functions to maintain delivery with ordering guarantees.
 * <p>
 * The terminology here is after <a href="http://gsd.di.uminho.pt/members/cbm/ps/itc2008.pdf">
 *     Interval Tree Clocks: A Logical Clock for Dynamic Systems</a>, see README.md
 *
 * @param <T> The message time type to be used
 */
public abstract class MessageService<T>
{
    /**
     * Call before sending this clock's state attached to a message.
     * Returns an immutable snapshot of time suitable for attachment to a message.
     */
    public T send()
    {
        event();
        return peek();
    }

    /**
     * Call to process a newly received message from the wire.
     *
     * @param message the message from the wire
     * @param buffer a buffer for out-of-order messages
     * @param process the local message consumer, which will receive messages in order
     * @return <code>false</code> iff the buffer is full
     */
    public <D, M extends Message<T, D>> boolean receive(
        M message, Queue<M> buffer, Consumer<D> process)
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
     * @param buffer a buffer of messages that might be caused by the delivered message.
     *               Must implement {@link Iterator#remove()}.
     * @param process the local message consumer, which will receive messages in order
     */
    private <D, M extends Message<T, D>> void deliver(
        M message, Iterable<M> buffer, Consumer<D> process)
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
     * @return an immutable snapshot of time suitable for attachment to a message.
     */
    protected abstract T peek();

    /**
     * Adds a new event, i.e. increments local process clock value
     */
    protected abstract void event();

    /**
     * Merges the given time into our local state
     *
     * @param metadata the new time to merge
     */
    protected abstract void join(T metadata);

    /**
     * The basic determinant of whether we can deliver a message with the given time.
     *
     * @param metadata an incoming message's time
     * @return <code>true</code> if our current clock state has all required history for the given time
     */
    public abstract boolean readyFor(T metadata);
}
