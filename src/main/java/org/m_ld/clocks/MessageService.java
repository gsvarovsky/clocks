/*
 * Copyright (c) George Svarovsky 2019. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

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
     * Call to process newly received message data from the wire.
     *
     * @param message the message from the wire
     * @param buffer  a buffer for out-of-order messages
     * @param process the local message data consumer, which will receive message data in order
     * @return <code>false</code> iff the buffer is full
     * @throws RuntimeException thrown by {@code process.accept(message)}. If this occurs, the clock time will have been
     *                          updated but no buffered messages re-considered. The caller has the opportunity to re-try
     *                          or ignore the exception prior to calling {@link #reconsider(Iterable, Consumer)}; but
     *                          this must be done before any further messages are received or delivered.
     */
    public <D, M extends Message<C, D>> boolean receive(
        M message, Queue<M> buffer, Consumer<? super D> process)
    {
        return receiveMessage(message, buffer, m -> process.accept(m.data()));
    }

    /**
     * Call to process a newly received message from the wire.
     * <p>
     * This variant supports message recipients who may be journaling messages for themselves.
     *
     * @param message the message from the wire
     * @param buffer  a buffer for out-of-order messages
     * @param process the local message consumer, which will receive messages in order
     * @return <code>false</code> iff the buffer is full
     * @throws RuntimeException thrown by {@code process.accept(message)}. If this occurs, the clock time will have been
     *                          updated but no buffered messages re-considered. The caller has the opportunity to re-try
     *                          or ignore the exception prior to calling {@link #reconsider(Iterable, Consumer)}; but
     *                          this must be done before any further messages are received or delivered.
     */
    public <M extends Message<C, ?>> boolean receiveMessage(
        M message, Queue<M> buffer, Consumer<? super M> process)
    {
        if (readyFor(message.time()))
        {
            // increment receiving process’s state value in its local vector
            event();
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
     * <p>
     * Calling this method does not increment the process clock, unless buffered causal
     * messages are also delivered as a result.
     *
     * @param message the message from the wire
     * @param buffer  a buffer of messages that might be caused by the delivered message.
     *                Must implement {@link Iterator#remove()}.
     * @param process the local message consumer, which will receive messages in order
     * @throws RuntimeException thrown by {@code process.accept(message)}. If this occurs, the clock time will have been
     *                          updated but no buffered messages re-considered. The caller has the opportunity to re-try
     *                          or ignore the exception prior to calling {@link #reconsider(Iterable, Consumer)}; but
     *                          this must be done before any further messages are received or delivered.
     */
    public <M extends Message<C, ?>> void deliver(
        M message, Iterable<M> buffer, Consumer<? super M> process)
    {
        join(message.time());

        process.accept(message);

        reconsider(buffer, process);
    }

    /**
     * Reconsiders the given buffer of messages, assuming that some change has been made to the local clock.
     *
     * @param buffer  a buffer of messages that might be caused by the delivered message.
     *                Must implement {@link Iterator#remove()}.
     * @param process the local message consumer, which will receive messages in order
     * @throws RuntimeException thrown by {@code process.accept(message)}. If this occurs, the clock time will have
     *                          been updated but no further buffered messages re-considered. The caller has the
     *                          opportunity to re-try or ignore the exception prior to calling this method again; but
     *                          this must be done before any further messages are received or delivered.
     */
    public <M extends Message<C, ?>> void reconsider(Iterable<M> buffer, Consumer<? super M> process)
    {
        for (Iterator<M> bufferIter = buffer.iterator(); bufferIter.hasNext(); )
        {
            final M next = bufferIter.next();
            if (readyFor(next.time()))
            {
                bufferIter.remove();
                // increment receiving process’s state value in its local vector
                event();
                // Recurse to start the iteration again on the modified buffer
                deliver(next, buffer, process);
                break;
            }
        }
    }

    /**
     * @return an immutable snapshot of time suitable for attachment to a message.
     */
    public abstract C peek();

    /**
     * Adds a new event, i.e. increments local process clock value
     */
    public abstract void event();

    /**
     * Merges the given time into our local state
     *
     * @param time the new time to merge
     */
    public abstract void join(C time);

    /**
     * Forks the clock in this message service, for a new process
     *
     * @return a clock suitable for a new process
     */
    public abstract C fork();

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
