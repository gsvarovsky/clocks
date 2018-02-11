package org.example;

import org.m_ld.clocks.Message;
import org.m_ld.clocks.MessageService;

import java.util.LinkedList;

import static org.m_ld.clocks.Message.message;

/**
 * An example process object making use of Vector Clocks.
 * In reality this is more likely to be a framework class such as an Actor or Verticle.
 *
 * @param <O> an operation type for an operation-based CRDT which requires causal delivery
 * @param <M> the message metadata type for this process
 */
public abstract class CausalCrdtProcess<M, O>
{
    private final MessageService<M> messageService;
    private final LinkedList<Message<M, O>> buffer = new LinkedList<>();

    public CausalCrdtProcess(MessageService<M> messageService)
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
     * Method for local update of the CRDT with a single operation.
     *
     * @param operation an operation to perform on the CRDT
     * @return A Message suitable to be sent to other replicas of the CRDT
     */
    public synchronized Message<M, O> update(O operation)
    {
        merge(operation);
        return message(messageService.send(), operation);
    }

    /**
     * Method to be called by the framework to deliver a message from another replica.
     *
     * @param message the message containing an operation to apply to the CRDT
     */
    public synchronized void receive(Message<M, O> message)
    {
        if (!messageService.receive(message, buffer, this::merge))
            throw new IllegalStateException("Buffer overload");
    }
}
