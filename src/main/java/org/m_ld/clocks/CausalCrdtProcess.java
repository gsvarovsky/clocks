package org.m_ld.clocks;

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

    protected abstract void merge(O data);

    public synchronized Message<VectorClock<UUID>, O> update(O data)
    {
        merge(data);
        return message(localVectorClock.onSend(), data);
    }

    public synchronized void receive(Message<VectorClock<UUID>, O> message)
    {
        if (!localVectorClock.onReceive(message, buffer, this::merge))
            throw new IllegalStateException("Buffer overload");
    }
}
