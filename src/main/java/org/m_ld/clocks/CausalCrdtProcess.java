package org.m_ld.clocks;

import java.util.LinkedList;
import java.util.UUID;

/**
 * An example process object making use of Vector Clocks.
 * In reality this is more likely to be a framework class such as an Actor or Verticle.
 * @param <CRDT> an operation-based CRDT type which may require causal delivery
 */
public abstract class CausalCrdtProcess<CRDT>
{
    private final LocalVectorClock<UUID> localVectorClock = new SyncLocalVectorClock<>(UUID.randomUUID());
    private final LinkedList<Message<VectorClock<UUID>, CRDT>> buffer = new LinkedList<>();

    protected abstract void merge(CRDT data);

    public synchronized Message<VectorClock<UUID>, CRDT> update(CRDT data)
    {
        merge(data);
        final VectorClock<UUID> vectorClock = localVectorClock.onSend();
        return new Message<VectorClock<UUID>, CRDT>()
        {
            @Override
            public VectorClock<UUID> metadata()
            {
                return vectorClock;
            }

            @Override
            public CRDT data()
            {
                return data;
            }
        };
    }

    public synchronized void receive(Message<VectorClock<UUID>, CRDT> message)
    {
        if (!localVectorClock.onReceive(message, buffer, this::merge))
            throw new IllegalStateException("Buffer overload");
    }
}
