package org.example;

import org.m_ld.clocks.CausalClock;
import org.m_ld.clocks.Message;
import org.m_ld.clocks.MessageService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A very inefficient example implementation of the OR-Set CRDT as a process.
 *
 * @param <C> the message clock type. Must guarantee causal ordering
 * @param <E> the set element type
 */
public class OrSetProcess<C extends CausalClock<C>, E> extends CausalCrdtProcess<C, List<OrSet.Operation<E>>>
    implements SetProxy<E, Optional<Message<C, List<OrSet.Operation<E>>>>>
{
    private final OrSet<E> orSet = new OrSet<>();

    public OrSetProcess(MessageService<C> messageService)
    {
        super(messageService);
    }

    public Set<E> elements()
    {
        return orSet.elements();
    }

    public synchronized Optional<Message<C, List<OrSet.Operation<E>>>> add(E element)
    {
        return orSet.add(element).map(this::updated);
    }

    public synchronized Optional<Message<C, List<OrSet.Operation<E>>>> remove(E element)
    {
        return orSet.remove(element).map(this::updated);
    }

    @Override protected void merge(List<OrSet.Operation<E>> operation)
    {
        orSet.apply(operation);
    }
}
