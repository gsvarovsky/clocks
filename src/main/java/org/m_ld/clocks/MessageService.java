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
 * @param <T> The message metadata type to be used
 */
public abstract class MessageService<T>
{
    /**
     * Call before sending this clock's state attached to a message.
     * Returns an immutable snapshot of metadata suitable for attachment to a message.
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
        if (readyFor(message.metadata()))
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
     * @return an immutable snapshot of metadata suitable for attachment to a message.
     */
    public abstract T peek();

    /**
     * Adds a new event, i.e. increments local process clock value
     */
    public abstract void event();

    /**
     * Merges the given metadata into our local state
     *
     * @param metadata the new metadata to merge
     */
    public abstract void join(T metadata);

    /**
     * The basic determinant of whether we can deliver a message with the given metadata.
     *
     * @param metadata an incoming message's metadata
     * @return <code>true</code> if our current clock state has all required history for the given metadata
     */
    public abstract boolean readyFor(T metadata);

    private <D, M extends Message<T, D>> void deliver(
        M message, Iterable<M> buffer, Consumer<D> process)
    {
        process.accept(message.data());

        join(message.metadata());

        // increment receiving process’s state value in its local vector
        event();

        // reconsider buffered messages
        for (Iterator<M> bufferIter = buffer.iterator(); bufferIter.hasNext(); )
        {
            final M next = bufferIter.next();
            if (readyFor(next.metadata()))
            {
                bufferIter.remove();
                // Recurse to start the iteration again on the modified buffer
                deliver(next, buffer, process);
                break;
            }
        }
    }
}
