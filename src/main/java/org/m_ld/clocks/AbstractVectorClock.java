package org.m_ld.clocks;

import java.util.Objects;

import static java.lang.String.format;

public abstract class AbstractVectorClock<PID> implements VectorClock<PID>
{
    @Override
    public int hashCode()
    {
        return Objects.hash(processId(), vector());
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof VectorClock &&
            Objects.equals(this.processId(), ((VectorClock)obj).processId()) &&
            Objects.equals(this.vector(), ((VectorClock)obj).vector());
    }

    @Override
    public String toString()
    {
        return format("VectorClock PID=%s, vector=%s", processId(), vector());
    }
}
