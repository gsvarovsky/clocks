package org.m_ld.clocks;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.m_ld.clocks.CausalCrdtProcessTest.OrSetOperation.Type.ADD;
import static org.m_ld.clocks.CausalCrdtProcessTest.OrSetOperation.Type.REMOVE;

public class CausalCrdtProcessTest
{
    static class OrSetOperation<E>
    {
        enum Type
        {
            ADD, REMOVE
        }

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

    static class OrSetProcess<E> extends CausalCrdtProcess<OrSetOperation<E>>
    {
        final Map<E, Set<Object>> elementIds = new HashMap<>();

        @Override
        protected void merge(OrSetOperation<E> op)
        {
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
        }

        synchronized List<Message<VectorClock<UUID>, OrSetOperation<E>>> add(E element)
        {
            return elementIds.containsKey(element) ? emptyList() :
                singletonList(update(new OrSetOperation<>(ADD, UUID.randomUUID(), element)));
        }

        synchronized List<Message<VectorClock<UUID>, OrSetOperation<E>>> remove(E element)
        {
            return !elementIds.containsKey(element) ? emptyList() :
                elementIds.get(element).stream()
                    .map(id -> new OrSetOperation<>(REMOVE, id, element))
                    .collect(toList()).stream()
                    .map(this::update)
                    .collect(toList());
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

        assertEquals(singleton(1), p1.elementIds.keySet());
        assertEquals(singleton(2), p2.elementIds.keySet());
        assertEquals(emptySet(), p3.elementIds.keySet());

        p3.receive(m1);
        p1.receive(m2);
        p2.receive(m1);
        p3.receive(m2);

        final Set<Integer> converged = Stream.of(1, 2).collect(toSet());
        assertEquals(converged, p1.elementIds.keySet());
        assertEquals(converged, p2.elementIds.keySet());
        assertEquals(converged, p3.elementIds.keySet());
    }

    @Test
    public void testLinkedConvergence()
    {
        OrSetProcess<Integer> p1 = new OrSetProcess<>(), p2 = new OrSetProcess<>(), p3 = new OrSetProcess<>();

        final Message<VectorClock<UUID>, OrSetOperation<Integer>> m1 =
            p1.update(new OrSetOperation<>(ADD, "m1", 1));

        p2.receive(m1);

        final Message<VectorClock<UUID>, OrSetOperation<Integer>> m2 =
            p2.update(new OrSetOperation<>(REMOVE, "m1", 1));

        p3.receive(m2); // Should be ignored
        p1.receive(m2);
        p3.receive(m1); // Should add, then remove (m2)

        assertEquals(emptySet(), p1.elementIds.keySet());
        assertEquals(emptySet(), p2.elementIds.keySet());
        assertEquals(emptySet(), p3.elementIds.keySet());
    }

    @Test
    public void testPandemonium() throws InterruptedException
    {
        final int processCount = 5, iterationTarget = 50, tickMillis = 10;
        final Set<OrSetProcess<Integer>> processes =
            Stream.<OrSetProcess<Integer>>generate(OrSetProcess::new).limit(processCount).collect(toSet());
        final Timer timer = new Timer("Pandemonium");
        final Random random = new Random(); // Happy with contention here
        // Every iteration for every process produces one ProcessTask#run and (processCount - 1) ProcessTask#Deliver
        final CountDownLatch done = new CountDownLatch(processCount * processCount * iterationTarget);

        class ProcessTask extends TimerTask
        {
            private final OrSetProcess<Integer> process;
            private int iterations = 0;

            private ProcessTask(OrSetProcess<Integer> process)
            {
                this.process = process;
            }

            @Override
            public void run()
            {
                // Every iteration make a random decision whether to add or remove from the set, favouring add
                final OrSetOperation.Type type = process.elementIds.isEmpty() ? ADD :
                    OrSetOperation.Type.values()[max(0, (int) ceil(random.nextDouble() * 3) - 2/*(-2..1]*/)];
                switch (type)
                {
                    case ADD: // Add a new random value
                        send(process.add(random.nextInt()));
                        break;
                    case REMOVE: // Remove an existing random value
                        send(process.remove(randomElement()));
                }

                if (++iterations == iterationTarget)
                    cancel();

                done.countDown();
            }

            private void send(List<Message<VectorClock<UUID>, OrSetOperation<Integer>>> messages)
            {
                // Send the messages to all the processes except us at random intervals
                processes.forEach(p -> {
                    if (p != process)
                        timer.schedule(new Delivery(messages, p), random.nextInt(tickMillis));
                });
            }

            private Integer randomElement()
            {
                return process.elementIds.keySet().stream()
                    .skip(random.nextInt(process.elementIds.keySet().size()))
                    .findFirst().orElseThrow(AssertionError::new);
            }

            class Delivery extends TimerTask
            {
                private final List<Message<VectorClock<UUID>, OrSetOperation<Integer>>> messages;
                private final OrSetProcess<Integer> recipient;

                Delivery(List<Message<VectorClock<UUID>, OrSetOperation<Integer>>> messages,
                         OrSetProcess<Integer> recipient)
                {
                    this.messages = messages;
                    this.recipient = recipient;
                }

                @Override
                public void run()
                {
                    messages.forEach(recipient::receive);
                    done.countDown();
                }
            }
        }

        processes.forEach(p -> timer.schedule(new ProcessTask(p), random.nextInt(tickMillis), tickMillis));
        done.await();

        assertNotNull(processes.stream().reduce((p1, p2) -> {
            assertEquals(p1.elementIds.keySet(), p2.elementIds.keySet());
            return p2;
        }));
    }
}
