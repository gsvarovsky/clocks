/*
 * Copyright (c) George Svarovsky 2019. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package org.m_ld.clocks.tree;

import org.junit.Test;

import static org.junit.Assert.*;

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
        TreeClock.GENESIS.update(TreeClock.GENESIS.tick());
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

    @Test
    public void testTicksForGenesisNotId()
    {
        final TreeClock ticked = TreeClock.GENESIS.tick();
        assertNull(ticked.ticks(false));
    }

    @Test
    public void testTicksForForkedNotId()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        assertEquals(0L, (long) fork.left.ticks(false));
    }

    @Test
    public void testTicksForUpdatedForkedNotId()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        assertEquals(1L, (long) fork.left.update(fork.right.tick()).ticks(false));
    }

    @Test
    public void testTicksForTickedUpdatedForkedNotId()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.tick().fork();
        final TreeClock updatedLeft = fork.left.update(fork.right.tick());
        assertEquals(2L, (long) updatedLeft.ticks(false));
    }

    @Test
    public void testNoOpMerge()
    {
        assertEquals(TreeClock.GENESIS, TreeClock.GENESIS.mergeId(TreeClock.GENESIS));
    }

    @Test
    public void testMergeForked()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        assertEquals(TreeClock.GENESIS, fork.left.mergeId(fork.right));
    }

    @Test
    public void testMergeTickedForked()
    {
        final TreeClock ticked = TreeClock.GENESIS.tick();
        final TreeClock.Fork fork = ticked.fork();
        assertEquals(ticked, fork.left.mergeId(fork.right));
    }

    @Test
    public void testMergeForkedTicked()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        assertEquals(1L, fork.left.tick().mergeId(fork.right).ticks());
    }

    @Test
    public void testMergeForkedTickedTicked()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        assertEquals(1L, fork.left.tick().mergeId(fork.right.tick()).ticks());
    }

    @Test
    public void testNonContiguousMerge()
    {
        final TreeClock.Fork fork1 = TreeClock.GENESIS.fork();
        final TreeClock.Fork fork2 = fork1.right.fork();

        final TreeClock clock1 = fork1.left.tick();
        final TreeClock clock3 = fork2.right.tick();

        assertEquals(1L, clock1.mergeId(clock3).ticks());
        assertEquals(2L, clock1.update(clock3).mergeId(clock3).ticks());
    }

    @Test
    public void testMergeAll()
    {
        final TreeClock.Fork fork1 = TreeClock.GENESIS.fork();
        final TreeClock.Fork fork2 = fork1.right.fork();

        final TreeClock clock1 = fork1.left.tick();
        final TreeClock clock2 = fork2.left.tick();
        final TreeClock clock3 = fork2.right.tick();

        final TreeClock clock4 = clock1.update(clock3).mergeId(clock3);
        final TreeClock clock5 = clock2.update(clock4).mergeId(clock4);

        assertEquals(3L, clock5.ticks());
    }

    @Test
    public void testLtGenesisSelf()
    {
        assertFalse(TreeClock.GENESIS.anyLt(TreeClock.GENESIS));
    }

    @Test
    public void testLtForked()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        assertFalse(fork.left.anyLt(fork.right));
    }

    @Test
    public void testLtForkedTicked()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        assertFalse(fork.left.anyLt(fork.right.tick()));
    }

    @Test
    public void testLtTwiceForkedTicked()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        final TreeClock.Fork rightFork = fork.right.fork();
        assertTrue(fork.left.anyLt(rightFork.left.update(rightFork.right.tick())));
    }

    @Test
    public void testLtThriceForkedTicked()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        final TreeClock.Fork rightFork = fork.right.fork();
        final TreeClock.Fork leftFork = fork.left.fork();
        assertTrue(leftFork.left.anyLt(rightFork.left.update(rightFork.right.tick())));
    }

    @Test
    public void testLtCompensatingTicks()
    {
        final TreeClock.Fork fork = TreeClock.GENESIS.fork();
        final TreeClock.Fork rightFork = fork.right.fork();
        final TreeClock.Fork leftFork = fork.left.fork();
        assertTrue(leftFork.left.update(leftFork.right.tick())
                       .anyLt(rightFork.left.update(rightFork.right.tick())));
    }
}
