package org.m_ld.clocks.intervaltree;

import itc4j.Stamp;
import org.m_ld.clocks.MessageService;

public class IntervalTreeClockService extends MessageService<Stamp>
{
    private Stamp localStamp;

    public IntervalTreeClockService(Stamp start)
    {
        this.localStamp = start;
    }

    @Override
    public Stamp peek()
    {
        final Stamp[] stamps = localStamp.peek();
        // Different order between paper and implementation
        assert localStamp.equals(stamps[0]);
        return stamps[1];
    }

    @Override
    public void event()
    {
        localStamp = localStamp.event();
    }

    @Override
    public void join(Stamp metadata)
    {
        localStamp = localStamp.join(metadata);
    }

    @Override
    public boolean laterThan(Stamp metadata)
    {
        System.out.printf("Local: %s, Message: %s\n", localStamp, metadata);
        boolean leq = metadata.leq(localStamp);
        System.out.println(localStamp.leq(metadata));
        return leq;
    }
}
