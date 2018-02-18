package org.m_ld.clocks.tree;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TreeClockTest
{
    @Test
    public void testGenesisHasNoTicks()
    {
        assertEquals(0L, TreeClock.GENESIS.ticks());
    }

    @Test
    public void testForkNotEqual()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        assertNotEquals(fork.left, fork.right);
    }

    @Test
    public void testForkTickNotEqual()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork().left.fork();
        assertNotEquals(fork.left, fork.right);
    }

    @Test
    public void testGenesisTick()
    {
        assertEquals(1L, TreeClock.GENESIS.tick().ticks());
        assertEquals(2L, TreeClock.GENESIS.tick().tick().ticks());
    }

    @Test
    public void testForkTick()
    {
        assertEquals(1L, TreeClock.GENESIS.fork().left.tick().ticks());
        assertEquals(2L, TreeClock.GENESIS.fork().left.tick().tick().ticks());
    }

    @Test
    public void testForkTickTick()
    {
        assertEquals(1L, TreeClock.GENESIS.fork().left.fork().left.tick().ticks());
        assertEquals(2L, TreeClock.GENESIS.fork().left.fork().left.tick().tick().ticks());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadUpdate()
    {
        TreeClock.GENESIS.update(TreeClock.GENESIS);
    }

    @Test
    public void testForkedNoUpdate()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        assertEquals(fork.left, fork.left.update(fork.right));
    }

    @Test
    public void testForkedLeftTickNoUpdate()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        final TreeClock newLeft = fork.left.tick();
        assertEquals(newLeft, newLeft.update(fork.right));
    }

    @Test
    public void testForkedRightTickNoUpdate()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        final TreeClock newRight = fork.right.tick();
        assertEquals(newRight, newRight.update(fork.left));
    }

    @Test
    public void testForkedLeftTickUpdateFromRight()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        assertNotEquals(fork.left, fork.left.update(fork.right.tick()));
        assertEquals(fork.left.ticks(), fork.left.update(fork.right.tick()).ticks());
    }

    @Test
    public void testForkedRightTickUpdateFromLeft()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        assertNotEquals(fork.right, fork.right.update(fork.left.tick()));
        assertEquals(fork.right.ticks(), fork.right.update(fork.left.tick()).ticks());
    }

    @Test
    public void testTickFork()
    {
        final TreeClock ticked = TreeClock.GENESIS.tick();
        final TreeClock.Fork forked = ticked.fork();
        assertEquals(1L, forked.left.ticks());
        assertEquals(1L, forked.right.ticks());
    }

    @Test
    public void testTickForkTick()
    {
        final TreeClock ticked = TreeClock.GENESIS.tick();
        final TreeClock.Fork forked = ticked.fork();
        assertEquals(2L, forked.left.tick().ticks());
        assertEquals(1L, forked.right.ticks());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadMerge()
    {
        TreeClock.merge(TreeClock.GENESIS, TreeClock.GENESIS);
    }

    @Test
    public void testMergeForked()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        assertEquals(TreeClock.GENESIS, TreeClock.merge(fork.left, fork.right));
    }

    @Test
    public void testMergeTickedForked()
    {
        final TreeClock ticked = TreeClock.GENESIS.tick();
        final TreeClock.Fork fork = ticked.fork();
        assertEquals(ticked, TreeClock.merge(fork.left, fork.right));
    }

    @Test
    public void testMergeForkedTicked()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        assertEquals(1L, TreeClock.merge(fork.left.tick(), fork.right).ticks());
    }

    @Test
    public void testMergeForkedTickedTicked()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        assertEquals(2L, TreeClock.merge(fork.left.tick(), fork.right.tick()).ticks());
    }

    @Test
    public void testNonContiguousMerge()
    {
        final TreeClock.Fork fork1 = TreeClock.GENESIS.fork();
        final TreeClock.Fork fork2 = fork1.right.fork();

        final TreeClock clock1 = fork1.left.tick();
        final TreeClock clock3 = fork2.right.tick();

        assertEquals(2L, TreeClock.merge(clock1, clock3).ticks());
    }

    @Test
    public void testMergeAll()
    {
        final TreeClock.Fork fork1 = TreeClock.GENESIS.fork();
        final TreeClock.Fork fork2 = fork1.right.fork();

        final TreeClock clock1 = fork1.left.tick();
        final TreeClock clock2 = fork2.left.tick();
        final TreeClock clock3 = fork2.right.tick();

        final TreeClock clock4 = TreeClock.merge(clock1, clock3);
        final TreeClock clock5 = TreeClock.merge(clock2, clock4);

        assertEquals(3L, clock5.ticks());
    }
}
