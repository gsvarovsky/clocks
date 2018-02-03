package org.m_ld.clocks;

public interface Message<M extends Message.Metadata, D>
{
    interface Metadata
    {
    }

    M metadata();

    D data();

    static <M extends Metadata, D> Message<M, D> message(M metadata, D data)
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
