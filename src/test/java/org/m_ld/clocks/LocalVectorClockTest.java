package org.m_ld.clocks;

import org.junit.Test;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.m_ld.clocks.WireVectorClock.clock;

public class LocalVectorClockTest
{
    @Test
    public void testFirstMessageSend()
    {
        final LocalVectorClock<String> p1Clock = new SyncLocalVectorClock<>("P1");
        p1Clock.onSend();
        assertEquals(1L, (long) p1Clock.vector().get("P1"));
    }

    @Test
    public void testFirstMessageReceive()
    {
        final LocalVectorClock<String> p1Clock = new SyncLocalVectorClock<>("P1");
        final AtomicReference<String> data = new AtomicReference<>();
        p1Clock.onReceive(testMessage(clock("P2", 2L), "1"), new LinkedList<>(), data::set);

        assertEquals(1L, (long) p1Clock.vector().get("P1"));
        assertEquals(2L, (long) p1Clock.vector().get("P2"));
        assertEquals("1", data.get());
    }

    @Test
    public void testFirstMessageBuffer()
    {
        final LocalVectorClock<String> p1Clock = new SyncLocalVectorClock<>("P1");
        final AtomicReference<String> data = new AtomicReference<>();
        final LinkedList<Message<VectorClock<String>, String>> buffer = new LinkedList<>();
        p1Clock.onReceive(testMessage(clock("P2", 2L).with("P3", 1L), "2"),
                          buffer, data::set);

        assertEquals(0L, (long) p1Clock.vector().get("P1"));
        assertNull(p1Clock.vector().get("P2"));
        assertEquals(1, buffer.size());
        assertNull(data.get());
    }

    @Test
    public void testFirstMessageUnbuffer()
    {
        final LocalVectorClock<String> p1Clock = new SyncLocalVectorClock<>("P1");
        final AtomicReference<String> data = new AtomicReference<>();
        final LinkedList<Message<VectorClock<String>, String>> buffer = new LinkedList<>();
        p1Clock.onReceive(testMessage(clock("P2", 2L).with("P3", 1L), "2"),
                          buffer, data::set);
        p1Clock.onReceive(testMessage(clock("P3", 1L), "1"), buffer, data::set);

        assertEquals(2L, (long) p1Clock.vector().get("P1"));
        assertEquals(2L, (long) p1Clock.vector().get("P2"));
        assertEquals(1L, (long) p1Clock.vector().get("P3"));
        assertEquals("2", data.get());
    }

    private static Message<VectorClock<String>, String> testMessage(VectorClock<String> clock, String data)
    {
        return new Message<VectorClock<String>, String>()
        {
            @Override
            public VectorClock<String> metadata()
            {
                return clock;
            }

            @Override
            public String data()
            {
                return data;
            }
        };
    }
}
