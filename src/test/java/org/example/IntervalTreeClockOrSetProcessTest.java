package org.example;

import itc4j.Stamp;
import org.junit.Before;
import org.m_ld.clocks.intervaltree.IntervalTreeClockService;

public class IntervalTreeClockOrSetProcessTest extends OrSetProcessTest<Stamp>
{
    private Stamp rootStamp;

    @Before
    public void setup()
    {
        rootStamp = new Stamp();
    }

    public OrSetProcess<Stamp, Integer> createProcess()
    {
        final Stamp[] stamps = rootStamp.fork();
        rootStamp = stamps[0];
        return new OrSetProcess<>(new IntervalTreeClockService(stamps[1]));
    }
}
