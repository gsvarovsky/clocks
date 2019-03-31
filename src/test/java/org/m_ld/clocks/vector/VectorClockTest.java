/*
 * Copyright (c) George Svarovsky 2019. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package org.m_ld.clocks.vector;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.m_ld.clocks.vector.WireVectorClock.clock;

public class VectorClockTest
{
    @Test
    public void testUnknownPid()
    {
        assertEquals(0L, clock("1", 1L).ticks("2"));
    }

    @Test
    public void testSelfNeverLt()
    {
        assertFalse(clock("1", 1L).anyLt(clock("1", 2L)));
    }

    @Test
    public void testPartiesNeverLt()
    {
        assertFalse(clock("1", 1L).with("2", 1L)
                        .anyLt(clock("2", 2L).with("1", 2L)));
    }

    @Test
    public void testThirdPartyNotLt()
    {
        assertFalse(clock("1", 1L).with("2", 1L).with("3", 1L)
                        .anyLt(clock("2", 2L).with("1", 2L).with("3", 1L)));
    }

    @Test
    public void testThirdPartyLt()
    {
        assertTrue(clock("1", 1L).with("2", 1L).with("3", 1L)
                       .anyLt(clock("2", 2L).with("1", 2L).with("3", 2L)));
    }
}