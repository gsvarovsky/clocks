/*
 * Copyright (c) George Svarovsky 2019. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package org.example;

import org.example.RandomSetProxyIteration.RandomIntegerSetProxyIteration;
import org.junit.Test;
import org.m_ld.clocks.CausalClock;
import org.m_ld.clocks.Message;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class OrSetProcessTest<C extends CausalClock<C>, P extends OrSetProcess<C, Integer>>
{
    public abstract P createProcess();

    @Test
    public void testUnlinkedConvergence()
    {
        P p1 = createProcess(), p2 = createProcess(), p3 = createProcess();

        final Message<C, List<OrSet.Operation<Integer>>> m2 = p2.add(2).orElseThrow(AssertionError::new);
        final Message<C, List<OrSet.Operation<Integer>>> m1 = p1.add(1).orElseThrow(AssertionError::new);

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

    @Test
    public void testLinkedConvergence()
    {
        P p1 = createProcess(), p2 = createProcess(), p3 = createProcess();

        final Message<C, List<OrSet.Operation<Integer>>> m1 = p1.add(1).orElseThrow(AssertionError::new);

        p2.receive(m1);

        final Message<C, List<OrSet.Operation<Integer>>> m2 = p2.remove(1).orElseThrow(AssertionError::new);

        p3.receive(m2); // Should be ignored
        p1.receive(m2);
        p3.receive(m1); // Should add, then remove (m2)

        assertEquals(emptySet(), p1.elements());
        assertEquals(emptySet(), p2.elements());
        assertEquals(emptySet(), p3.elements());
    }

    @Test
    public void testPandemonium() throws InterruptedException
    {
        final Set<P> processes = Stream.generate(this::createProcess).limit(5).collect(toSet());
        final Map<P, Map<P, Queue<Message<C, List<OrSet.Operation<Integer>>>>>> channels = new HashMap<>();

        // Every iteration for every process produces one ProcessTask#run and (processCount - 1) ProcessTask#Deliver
        final ConvergenceTest convergenceTest = new ConvergenceTest(processes.size(), processes.size());
        convergenceTest.run(processes.stream().map(process -> new RandomIntegerSetProxyIteration<P,
            Optional<Message<C, List<OrSet.Operation<Integer>>>>>(process, convergenceTest.random())
        {
            @Override public void send(Optional<Message<C, List<OrSet.Operation<Integer>>>> operation)
            {
                operation.ifPresent(message -> {
                    // Send the messages to all the processes except us at random intervals
                    processes.forEach(otherProcess -> {
                        if (otherProcess != process)
                        {
                            final Queue<Message<C, List<OrSet.Operation<Integer>>>> channel = channelTo(otherProcess);
                            channel.add(message);
                            convergenceTest.schedule(() -> otherProcess.receive(channel.poll()));
                        }
                    });
                });
            }

            private Queue<Message<C, List<OrSet.Operation<Integer>>>> channelTo(P otherProcess)
            {
                return channels.computeIfAbsent(process, p -> new HashMap<>())
                    .computeIfAbsent(otherProcess, p -> new ConcurrentLinkedQueue<>());
            }
        }));

        assertNotNull(processes.stream().reduce((p1, p2) -> {
            assertEquals(p1.elements(), p2.elements());
            return p2;
        }));
    }
}
