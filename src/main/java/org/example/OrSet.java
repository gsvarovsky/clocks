package org.example;

import java.util.*;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.example.OrSet.Operation.Type.ADD;
import static org.example.OrSet.Operation.Type.REMOVE;

/**
 * A very inefficient example implementation of the OR-Set CRDT.
 *
 * @param <E> the set element type
 */
public class OrSet<E>
{
    private final Map<E, Set<Object>> elementIds = new HashMap<>();

    static class Operation<E>
    {
        enum Type
        {
            ADD, REMOVE
        }

        final Operation.Type type;
        final Object id;
        final E element;

        Operation(Operation.Type type, Object id, E element)
        {
            this.type = type;
            this.id = id;
            this.element = element;
        }

        @Override public String toString()
        {
            return type.name() + ": " + element + " (" + id + ")";
        }
    }

    public synchronized Set<E> elements()
    {
        return unmodifiableSet(elementIds.keySet());
    }

    public synchronized Optional<List<Operation<E>>> add(E element)
    {
        return elementIds.containsKey(element) ? Optional.empty() :
            Optional.of(apply(singletonList(new Operation<>(ADD, randomUUID(), element))));
    }

    public synchronized Optional<List<Operation<E>>> remove(E element)
    {
        return !elementIds.containsKey(element) ? Optional.empty() :
            Optional.of(apply(elementIds.get(element).stream()
                                  .map(id -> new Operation<>(REMOVE, id, element))
                                  .collect(toList())));
    }

    public synchronized List<Operation<E>> apply(List<Operation<E>> ops)
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
        return ops;
    }
}
