package org.m_ld.clocks;

public interface Message<C, D>
{
    /**
     * Message time according to some clock
     */
    C time();

    /**
     * Message data to be delivered to a process
     */
    D data();

    static <M, D> Message<M, D> message(M time, D data)
    {
        return new Message<M, D>()
        {
            @Override
            public M time()
            {
                return time;
            }

            @Override
            public D data()
            {
                return data;
            }

            @Override public String toString()
            {
                return data + " @ " + time;
            }
        };
    }
}
