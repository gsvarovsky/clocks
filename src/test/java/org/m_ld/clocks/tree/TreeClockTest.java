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
    public void tesBadUpdate()
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
}
