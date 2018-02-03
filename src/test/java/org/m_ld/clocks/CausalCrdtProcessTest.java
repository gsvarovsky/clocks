package org.m_ld.clocks;

import org.junit.Test;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.m_ld.clocks.CausalCrdtProcessTest.OrSetOperation.Type.ADD;

public class CausalCrdtProcessTest
{
    static class OrSetOperation<E>
    {
        enum Type { ADD, REMOVE }
        final Type type;
        final Object id;
        final E element;

        OrSetOperation(Type type, Object id, E element)
        {
            this.type = type;
            this.id = id;
            this.element = element;
        }
    }

    class OrSetProcess<E> extends CausalCrdtProcess<OrSetOperation<E>>
    {
        final Map<E, Set> elementIds = new HashMap<>();

        Set<E> elements()
        {
            return elementIds.keySet();
        }

        @Override
        protected void merge(OrSetOperation<E> op)
        {
            final Set ids = elementIds.computeIfAbsent(op.element, e -> new HashSet());
            switch (op.type)
            {
                case ADD:
                    //noinspection unchecked
                    ids.add(op.id);
                    break;
                case REMOVE:
                    ids.remove(op.id);
                    if (ids.isEmpty())
                        elementIds.remove(op.element);
                    break;
            }
        }
    }

    @Test
    public void testUnlinkedConvergence()
    {
        OrSetProcess<Integer> p1 = new OrSetProcess<>(), p2 = new OrSetProcess<>(), p3 = new OrSetProcess<>();

        final Message<VectorClock<UUID>, OrSetOperation<Integer>> m2 =
            p2.update(new OrSetOperation<>(ADD, "m2", 2));

        final Message<VectorClock<UUID>, OrSetOperation<Integer>> m1 =
            p1.update(new OrSetOperation<>(ADD, "m1", 1));

        assertEquals(singleton(1), p1.elements());
        assertEquals(singleton(2), p2.elements());
        assertEquals(emptySet(), p3.elements());

        p3.receive(m1);
        p1.receive(m2);
        p2.receive(m1);
        p3.receive(m2);

        final Set<Integer> converged = Stream.of(1, 2).collect(toSet());
        assertEquals(converged, p1.elements());
        assertEquals(converged, p2.elements());
        assertEquals(converged, p3.elements());
    }
}
