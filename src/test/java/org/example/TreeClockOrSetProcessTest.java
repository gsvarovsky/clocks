/*
 * Copyright (c) George Svarovsky 2019. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package org.example;

import org.m_ld.clocks.tree.TreeClock;
import org.m_ld.clocks.tree.TreeClockMessageService;

public class TreeClockOrSetProcessTest extends OrSetProcessTest<TreeClock, OrSetProcess<TreeClock, Integer>>
{
    private TreeClock previous = TreeClock.GENESIS;

    public OrSetProcess<TreeClock, Integer> createProcess()
    {
        final TreeClock.Fork fork = previous.fork();
        previous = fork.left;
        return new OrSetProcess<>(new TreeClockMessageService(fork.right));
    }
}
