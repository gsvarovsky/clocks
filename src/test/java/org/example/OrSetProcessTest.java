package org.example;

import org.example.OrSetProcess.OrSetOperation;
import org.junit.Test;
import org.m_ld.clocks.Message;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.example.OrSetProcess.OrSetOperation.Type.ADD;
import static org.example.OrSetProcess.OrSetOperation.Type.REMOVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class OrSetProcessTest<M>
{
    public abstract OrSetProcess<M, Integer> createProcess();

    @Test
    public void testUnlinkedConvergence()
    {
        OrSetProcess<M, Integer>
            p1 = createProcess(),
            p2 = createProcess(),
            p3 = createProcess();

        final Message<M, OrSetOperation<Integer>> m2 =
            p2.update(new OrSetOperation<>(ADD, "m2", 2));

        final Message<M, OrSetOperation<Integer>> m1 =
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
        OrSetProcess<M, Integer>
            p1 = createProcess(),
            p2 = createProcess(),
            p3 = createProcess();

        final Message<M, OrSetOperation<Integer>> m1 =
            p1.update(new OrSetOperation<>(ADD, "m1", 1));

        p2.receive(m1);

        final Message<M, OrSetOperation<Integer>> m2 =
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
        final Set<OrSetProcess<M, Integer>> processes =
            Stream.generate(this::createProcess).limit(processCount).collect(toSet());
        final Timer timer = new Timer("Pandemonium");
        final Random random = new Random(); // Happy with contention here
        // Every iteration for every process produces one ProcessTask#run and (processCount - 1) ProcessTask#Deliver
        final CountDownLatch done = new CountDownLatch(processCount * processCount * iterationTarget);

        class ProcessTask extends TimerTask
        {
            private final OrSetProcess<M, Integer> process;
            private int iterations = 0;

            private ProcessTask(OrSetProcess<M, Integer> process)
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

            private void send(List<Message<M, OrSetOperation<Integer>>> messages)
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
                private final List<Message<M, OrSetOperation<Integer>>> messages;
                private final OrSetProcess<M, Integer> recipient;

                Delivery(List<Message<M, OrSetOperation<Integer>>> messages,
                         OrSetProcess<M, Integer> recipient)
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
