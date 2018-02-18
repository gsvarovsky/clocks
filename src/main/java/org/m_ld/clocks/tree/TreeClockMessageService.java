package org.m_ld.clocks.tree;

import org.m_ld.clocks.MessageService;

public class TreeClockMessageService extends MessageService<TreeClock>
{
    private TreeClock localClock;

    public TreeClockMessageService(TreeClock clock)
    {
        this.localClock = clock;
    }

    @Override
    public TreeClock peek()
    {
        return localClock;
    }

    @Override
    public void event()
    {
        localClock = localClock.tick();
    }

    @Override
    public void join(TreeClock metadata)
    {
        localClock = localClock.update(metadata);
    }

    @Override
    public boolean readyFor(TreeClock metadata)
    {
        // do the sender and receiver agree on the state of all other processes?
        // If the sender has a higher state value for any of these others, the receiver is missing
        // a message so buffer the message
        final Long senderOtherTicks = localClock.update(metadata).mergeId(metadata).ticks(false);
        final Long receiverOtherTicks = localClock.mergeId(metadata).ticks(false);
        return receiverOtherTicks >= senderOtherTicks;
    }
}
