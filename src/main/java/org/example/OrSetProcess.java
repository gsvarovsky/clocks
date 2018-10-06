package org.example;

import org.m_ld.clocks.Message;
import org.m_ld.clocks.MessageService;

import java.util.*;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.example.OrSetProcess.OrSetOperation.Type.ADD;
import static org.example.OrSetProcess.OrSetOperation.Type.REMOVE;

/**
 * A very inefficient example implementation of the OR-Set CRDT as a process.
 * @param <C> the message clock type. Must guarantee causal ordering
 * @param <E> the set element type
 */
public class OrSetProcess<C, E> extends CausalCrdtProcess<C, List<OrSetProcess.OrSetOperation<E>>>
{
    final Map<E, Set<Object>> elementIds = new HashMap<>();

    static class OrSetOperation<E>
    {
        enum Type
        {
            ADD, REMOVE
        }

        final OrSetOperation.Type type;
        final Object id;
        final E element;

        OrSetOperation(OrSetOperation.Type type, Object id, E element)
        {
            this.type = type;
            this.id = id;
            this.element = element;
        }

        @Override public String toString()
        {
            return type.name() + ": " + element + " (" + id +  ")";
        }
    }

    public OrSetProcess(MessageService<C> messageService)
    {
        super(messageService);
    }

    public synchronized Optional<Message<C, List<OrSetProcess.OrSetOperation<E>>>> add(E element)
    {
        return elementIds.containsKey(element) ? Optional.empty() :
            Optional.of(update(singletonList(new OrSetOperation<>(ADD, UUID.randomUUID(), element))));
    }

    public synchronized Optional<Message<C, List<OrSetOperation<E>>>> remove(E element)
    {
        return !elementIds.containsKey(element) ? Optional.empty() :
            Optional.of(update(elementIds.get(element).stream()
                .map(id -> new OrSetOperation<>(REMOVE, id, element))
                .collect(toList())));
    }

    @Override
    protected void merge(List<OrSetOperation<E>> ops)
    {
        ops.forEach(op -> {
            switch (op.type)
            {
                case ADD:
                    elementIds.computeIfAbsent(op.element, e -> new HashSet<>()).add(op.id);
                    break;
                case REMOVE:
                    final Set ids = elementIds.get(op.element);
                    if (ids != null && ids.remove(op.id))
                    {
                        if (ids.isEmpty())
                            elementIds.remove(op.element);
                    }
            }
        });
    }
}
