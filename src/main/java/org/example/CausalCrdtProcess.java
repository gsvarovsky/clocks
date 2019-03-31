/*
 * Copyright (c) George Svarovsky 2019. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package org.example;

import org.m_ld.clocks.CausalClock;
import org.m_ld.clocks.Message;
import org.m_ld.clocks.MessageService;

import java.util.LinkedList;

import static org.m_ld.clocks.Message.message;

/**
 * An example process object making use of Vector Clocks.
 * In reality this is more likely to be a framework class such as an Actor or Verticle.
 *
 * @param <O> an operation type for an operation-based CRDT which requires causal delivery
 * @param <C> the message clock type for this process
 */
public abstract class CausalCrdtProcess<C extends CausalClock<C>, O>
{
    private final MessageService<C> messageService;
    private final LinkedList<Message<C, O>> buffer = new LinkedList<>();

    public CausalCrdtProcess(MessageService<C> messageService)
    {
        this.messageService = messageService;
    }

    /**
     * Implementation of a conflict-free operation against the CRDT.
     *
     * @param operation the operation to perform
     */
    protected abstract void merge(O operation);

    /**
     * Method to construct a message after a local update of the CRDT.
     *
     * @param operation an operation performed on the CRDT
     * @return A Message suitable to be sent to other replicas of the CRDT
     */
    protected synchronized Message<C, O> updated(O operation)
    {
        return message(messageService.send(), operation);
    }

    /**
     * Method to be called by the framework to deliver a message from another replica.
     *
     * @param message the message containing an operation to apply to the CRDT
     */
    public synchronized void receive(Message<C, O> message)
    {
        if (!messageService.receive(message, buffer, this::merge))
            throw new IllegalStateException("Buffer overload");
    }
}
