/*
 * Copyright (c) George Svarovsky 2019. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package org.example;

import java.util.*;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.example.OrSet.Operation.Type.ADD;
import static org.example.OrSet.Operation.Type.REMOVE;

/**
 * A very inefficient example implementation of the OR-Set CRDT.
 *
 * @param <E> the set element type
 */
public class OrSet<E> implements SetProxy<E, Optional<List<OrSet.Operation<E>>>>
{
    private final Map<E, Set<UUID>> elementIds = new HashMap<>();

    static class Operation<E>
    {
        enum Type
        {
            ADD, REMOVE
        }

        final Operation.Type type;
        final UUID id;
        final E element;

        Operation(Operation.Type type, UUID id, E element)
        {
            this.type = requireNonNull(type);
            this.id = requireNonNull(id);
            this.element = element;
        }

        @Override public int hashCode()
        {
            return Objects.hash(type, id);
        }

        @Override public boolean equals(Object obj)
        {
            return obj instanceof Operation && type == ((Operation)obj).type && id.equals(((Operation)obj).id);
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

    public synchronized Set<Map.Entry<E, Set<UUID>>> entries()
    {
        return unmodifiableSet(elementIds.entrySet());
    }

    public synchronized Set<UUID> putEntry(Map.Entry<E, Set<UUID>> entry)
    {
        return elementIds.put(entry.getKey(), entry.getValue());
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

    /**
     * Clears the content of this OR-Set.
     * Great care should be taken to ensure that any process clocks are synchronised, otherwise this method could
     * permanently break convergence.
     */
    public synchronized void clear()
    {
        elementIds.clear();
    }
}
