package org.m_ld.clocks.tree;

import org.m_ld.clocks.MessageService;

/**
 * A {@link MessageService} using a {@link TreeClock} to ensure causally-ordered message delivery.
 */
public class TreeClockMessageService extends MessageService<TreeClock>
{
    private TreeClock localTime;

    public void reset(TreeClock time)
    {
        this.localTime = time;
    }

    @Override
    public TreeClock peek()
    {
        return localTime;
    }

    @Override
    public synchronized void event()
    {
        localTime = localTime.tick();
    }

    @Override
    public synchronized void join(TreeClock metadata)
    {
        localTime = localTime.update(metadata);
    }
}
