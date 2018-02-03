package org.m_ld.clocks;

public interface Message<M extends Message.Metadata, D>
{
    interface Metadata
    {
    }

    M metadata();

    D data();
}
