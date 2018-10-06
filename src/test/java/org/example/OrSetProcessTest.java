package org.example;

import org.example.OrSetProcess.OrSetOperation;
import org.junit.Test;
import org.m_ld.clocks.Message;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toSet;
import static org.example.OrSetProcess.OrSetOperation.Type.ADD;
import static org.example.OrSetProcess.OrSetOperation.Type.REMOVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class OrSetProcessTest<C, P extends OrSetProcess<C, Integer>>
{
    public abstract P createProcess();

    @Test
    public void testUnlinkedConvergence()
    {
        P
            p1 = createProcess(),
            p2 = createProcess(),
            p3 = createProcess();

        final Message<C, List<OrSetOperation<Integer>>> m2 =
            p2.update(singletonList(new OrSetOperation<>(ADD, "m2", 2)));

        final Message<C, List<OrSetOperation<Integer>>> m1 =
            p1.update(singletonList(new OrSetOperation<>(ADD, "m1", 1)));

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
        P
            p1 = createProcess(),
            p2 = createProcess(),
            p3 = createProcess();

        final Message<C, List<OrSetOperation<Integer>>> m1 =
            p1.update(singletonList(new OrSetOperation<>(ADD, "m1", 1)));

        p2.receive(m1);

        final Message<C, List<OrSetOperation<Integer>>> m2 =
            p2.update(singletonList(new OrSetOperation<>(REMOVE, "m1", 1)));

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
        final Set<P> processes =
            Stream.generate(this::createProcess).limit(processCount).collect(toSet());
        final Map<P, Map<P, Queue<Message<C, List<OrSetOperation<Integer>>>>>> channels = new HashMap<>();
        final Timer timer = new Timer("Pandemonium");
        final Random random = new Random(); // Happy with contention here
        // Every iteration for every process produces one ProcessTask#run and (processCount - 1) ProcessTask#Deliver
        final CountDownLatch done = new CountDownLatch(processCount * processCount * iterationTarget);

        class ProcessTask extends TimerTask
        {
            private final P process;
            private final int iteration;

            private ProcessTask(P process, int iteration)
            {
                this.process = process;
                this.iteration = iteration;
            }

            @Override
            public void run()
            {
                // Every iteration make a random decision whether to add or remove from the set, favouring add
                if (process.elementIds.isEmpty() || random.nextDouble() * 3 - 2/*(-2..1]*/ < 0)
                    process.add(random.nextInt()).ifPresent(this::send);
                else
                    process.remove(randomElement()).ifPresent(this::send);

                if (iteration < iterationTarget)
                    timer.schedule(new ProcessTask(process, iteration + 1), random.nextInt(tickMillis));

                done.countDown();
            }

            private void send(Message<C, List<OrSetOperation<Integer>>> message)
            {
                // Send the messages to all the processes except us at random intervals
                processes.forEach(otherProcess -> {
                    if (otherProcess != process)
                    {
                        final Queue<Message<C, List<OrSetOperation<Integer>>>> channel = channelTo(otherProcess);
                        channel.add(message);
                        timer.schedule(new TimerTask()
                        {
                            @Override public void run()
                            {
                                otherProcess.receive(channel.poll());
                                done.countDown();
                            }
                        }, random.nextInt(tickMillis));
                    }
                });
            }

            private Queue<Message<C, List<OrSetOperation<Integer>>>> channelTo(P otherProcess)
            {
                return channels.computeIfAbsent(process, p -> new HashMap<>())
                    .computeIfAbsent(otherProcess, p -> new ConcurrentLinkedQueue<>());
            }

            private Integer randomElement()
            {
                return process.elementIds.keySet().stream()
                    .skip(random.nextInt(process.elementIds.keySet().size()))
                    .findFirst().orElseThrow(AssertionError::new);
            }
        }

        processes.forEach(p -> timer.schedule(new ProcessTask(p, 1), random.nextInt(tickMillis)));
        done.await();

        assertNotNull(processes.stream().reduce((p1, p2) -> {
            assertEquals(p1.elementIds.keySet(), p2.elementIds.keySet());
            return p2;
        }));
    }
}
