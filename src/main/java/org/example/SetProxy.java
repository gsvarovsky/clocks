package org.example;

import java.util.Set;

/**
 * A proxy for a Set, which exposes some operation type {@code O}.
 *
 * @param <E> the element type
 * @param <O> the operation type returned by mutations
 */
public interface SetProxy<E, O>
{
    /**
     * @param element the element to add to the underlying set
     * @return the operation enacted
     */
    O add(E element);

    /**
     * @param element the element to remove from the underlying set
     * @return the operation enacted
     */
    O remove(E element);

    /**
     * @return a snapshot of all the elements in the set. Not expected to track changes to the underlying data. May be
     * expensive due to the necessity to maintain a consistent view while retrieving the data.
     */
    Set<E> elements();
}
