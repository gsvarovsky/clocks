package org.m_ld.clocks;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class CausalCrdtProcessTest
{
    // TODO: Change to SU-Set, or another CRDT requiring causal delivery
    private class IntegerSumProcess extends CausalCrdtProcess<Integer>
    {
        int data = 0;

        @Override
        protected void merge(Integer data)
        {
            this.data += data;
        }
    }

    @Test
    public void testUnlinkedIntegerSum()
    {
        IntegerSumProcess p1 = new IntegerSumProcess(), p2 = new IntegerSumProcess(), p3 = new IntegerSumProcess();

        final Message<VectorClock<UUID>, Integer> m2 = p2.update(1);
        assertEquals(1, p2.data);

        final Message<VectorClock<UUID>, Integer> m1 = p1.update(2);
        assertEquals(2, p1.data);

        p3.receive(m1);
        p1.receive(m2);
        p2.receive(m1);
        p3.receive(m2);

        assertEquals(3, p1.data);
        assertEquals(3, p2.data);
        assertEquals(3, p3.data);
    }
}
