package org.m_ld.clocks;

public interface Message<M, D>
{
    M metadata();

    D data();

    static <M, D> Message<M, D> message(M metadata, D data)
    {
        return new Message<M, D>()
        {
            @Override
            public M metadata()
            {
                return metadata;
            }

            @Override
            public D data()
            {
                return data;
            }
        };
    }
}
