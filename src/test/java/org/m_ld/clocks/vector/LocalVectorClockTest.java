package org.m_ld.clocks.vector;

import org.junit.Test;
import org.m_ld.clocks.Message;
import org.m_ld.clocks.MessageService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.m_ld.clocks.Message.message;
import static org.m_ld.clocks.vector.WireVectorClock.clock;

/**
 * Note that integer sum is a CRDT that does not require causal delivery,
 * so the data used in these tests are simply to give the processes some state and provide a weak cross-check.
 */
public class LocalVectorClockTest
{
    @Test
    public void testFirstMessageSend()
    {
        final LocalVectorClock<String> p1Clock = new SyncLocalVectorClock<>("P1");
        p1Clock.send();
        assertEquals(1L, (long) p1Clock.vector().get("P1"));
    }

    @Test
    public void testFirstMessageReceive()
    {
        final LocalVectorClock<String> p1Clock = new SyncLocalVectorClock<>("P1");
        final List<String> data = new ArrayList<>();
        p1Clock.receive(message(clock("P2", 2L), "1"), new LinkedList<>(), data::add);

        assertEquals(1L, (long) p1Clock.vector().get("P1"));
        assertEquals(2L, (long) p1Clock.vector().get("P2"));
        assertEquals(singletonList("1"), data);
    }

    @Test
    public void testFirstMessageBuffer()
    {
        final LocalVectorClock<String> p1Clock = new SyncLocalVectorClock<>("P1");
        final List<String> data = new ArrayList<>();
        final LinkedList<Message<VectorClock<String>, String>> buffer = new LinkedList<>();
        p1Clock.receive(message(clock("P2", 2L).with("P3", 1L), "2"),
                        buffer, data::add);

        assertEquals(0L, (long) p1Clock.vector().get("P1"));
        assertNull(p1Clock.vector().get("P2"));
        assertEquals(1, buffer.size());
        assertTrue(data.isEmpty());
    }

    @Test
    public void testFirstMessageUnbuffer()
    {
        final LocalVectorClock<String> p1Clock = new SyncLocalVectorClock<>("P1");
        final List<String> data = new ArrayList<>();
        final LinkedList<Message<VectorClock<String>, String>> buffer = new LinkedList<>();
        p1Clock.receive(message(clock("P2", 2L).with("P3", 1L), "2"),
                        buffer, data::add);
        p1Clock.receive(message(clock("P3", 1L), "1"), buffer, data::add);

        assertEquals(2L, (long) p1Clock.vector().get("P1"));
        assertEquals(2L, (long) p1Clock.vector().get("P2"));
        assertEquals(1L, (long) p1Clock.vector().get("P3"));
        assertEquals(asList("1", "2"), data);
    }

    @Test
    public void testUnlinkedDeliveries()
    {
        AtomicInteger p1Sum = new AtomicInteger(),
            p2Sum = new AtomicInteger(), p3Sum = new AtomicInteger();
        final LinkedList<Message<VectorClock<String>, Integer>> buffer1 = new LinkedList<>(),
            buffer2 = new LinkedList<>(), buffer3 = new LinkedList<>();

        final MessageService<VectorClock<String>> p1Clock = new SyncLocalVectorClock<>("P1");
        final MessageService<VectorClock<String>> p2Clock = new SyncLocalVectorClock<>("P2");
        final MessageService<VectorClock<String>> p3Clock = new SyncLocalVectorClock<>("P3");

        p2Sum.addAndGet(1);
        final Message<VectorClock<String>, Integer> m2 = message(p2Clock.send(), 1);
        assertEquals(clock("P2", 1L), p2Clock.peek());

        p1Sum.addAndGet(2);
        final Message<VectorClock<String>, Integer> m1 = message(p1Clock.send(), 2);
        assertEquals(clock("P1", 1L), p1Clock.peek());

        p3Clock.receive(m1, buffer3, p3Sum::addAndGet);
        assertEquals(clock("P3", 1L).with("P1", 1L), p3Clock.peek());
        p1Clock.receive(m2, buffer1, p1Sum::addAndGet);
        assertEquals(clock("P1", 2L).with("P2", 1L), p1Clock.peek());
        p2Clock.receive(m1, buffer2, p2Sum::addAndGet);
        assertEquals(clock("P2", 2L).with("P1", 1L), p2Clock.peek());
        p3Clock.receive(m2, buffer3, p3Sum::addAndGet);
        assertEquals(clock("P3", 2L).with("P1", 1L).with("P2", 1L), p3Clock.peek());

        assertEquals(3, p1Sum.get());
        assertEquals(3, p2Sum.get());
        assertEquals(3, p3Sum.get());
    }

    @Test
    public void testLinkedDeliveries()
    {
        AtomicInteger p1Sum = new AtomicInteger(),
            p2Sum = new AtomicInteger(), p3Sum = new AtomicInteger();
        final LinkedList<Message<VectorClock<String>, Integer>> buffer1 = new LinkedList<>(),
            buffer2 = new LinkedList<>(), buffer3 = new LinkedList<>();

        final MessageService<VectorClock<String>> p1Clock = new SyncLocalVectorClock<>("P1");
        final MessageService<VectorClock<String>> p2Clock = new SyncLocalVectorClock<>("P2");
        final MessageService<VectorClock<String>> p3Clock = new SyncLocalVectorClock<>("P3");

        p1Sum.addAndGet(1);
        final Message<VectorClock<String>, Integer> m1 = message(p1Clock.send(), 1);
        p2Clock.receive(m1, buffer2, p2Sum::addAndGet);

        p2Sum.addAndGet(2);
        final Message<VectorClock<String>, Integer> m2 = message(p2Clock.send(), 2);
        p3Clock.receive(m2, buffer3, p3Sum::addAndGet);

        assertTrue(buffer3.contains(m2));

        p1Clock.receive(m2, buffer1, p1Sum::addAndGet);
        p3Clock.receive(m1, buffer3, p3Sum::addAndGet);

        assertTrue(buffer3.isEmpty());

        assertEquals(3, p1Sum.get());
        assertEquals(3, p2Sum.get());
        assertEquals(3, p3Sum.get());
    }
}