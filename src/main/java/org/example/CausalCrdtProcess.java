package org.example;

import org.m_ld.clocks.Message;
import org.m_ld.clocks.vector.LocalVectorClock;
import org.m_ld.clocks.vector.SyncLocalVectorClock;
import org.m_ld.clocks.vector.VectorClock;

import java.util.LinkedList;
import java.util.UUID;

import static org.m_ld.clocks.Message.message;

/**
 * An example process object making use of Vector Clocks.
 * In reality this is more likely to be a framework class such as an Actor or Verticle.
 * @param <O> an operation type for an operation-based CRDT which requires causal delivery
 */
public abstract class CausalCrdtProcess<O>
{
    private final LocalVectorClock<UUID> localVectorClock = new SyncLocalVectorClock<>(UUID.randomUUID());
    private final LinkedList<Message<VectorClock<UUID>, O>> buffer = new LinkedList<>();

    /**
     * Implementation of a conflict-free operation against the CRDT.
     * @param operation the operation to perform
     */
    protected abstract void merge(O operation);

    /**
     * Method for local update of the CRDT with a single operation.
     * @param operation an operation to perform on the CRDT
     * @return A Message suitable to be sent to other replicas of the CRDT
     */
    public synchronized Message<VectorClock<UUID>, O> update(O operation)
    {
        merge(operation);
        return message(localVectorClock.onSend(), operation);
    }

    /**
     * Method to be called by the framework to deliver a message from another replica.
     * @param message the message containing an operation to apply to the CRDT
     */
    public synchronized void receive(Message<VectorClock<UUID>, O> message)
    {
        if (!localVectorClock.onReceive(message, buffer, this::merge))
            throw new IllegalStateException("Buffer overload");
    }
}
