package org.m_ld.clocks.vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;

/**
 * An immutable vector clock representation intended to be used on the wire.
 * Intended to be extended with appropriate serialization features.
 */
public class WireVectorClock<PID> implements VectorClock<PID>
{
    private final PID pid;
    private final Map<PID, Long> vector;

    private WireVectorClock(PID pid, Map<PID, Long> vector)
    {
        if (!vector.containsKey(pid))
            throw new IllegalStateException("Vector clock does not contain its own process ID");

        this.pid = pid;
        this.vector = vector;
    }

    public static <PID> WireVectorClock<PID> clock(PID pid, long ticks)
    {
        return new WireVectorClock<>(pid, singletonMap(pid, ticks));
    }

    public static <PID> WireVectorClock<PID> clock(PID pid, Map<PID, Long> vector)
    {
        return new WireVectorClock<>(pid, unmodifiableMap(new HashMap<>(vector)));
    }

    public WireVectorClock<PID> with(PID pid, long ticks)
    {
        final HashMap<PID, Long> vector = new HashMap<>(this.vector);
        vector.put(pid, ticks);
        return new WireVectorClock<>(this.pid, vector);
    }

    @Override
    public PID processId()
    {
        return pid;
    }

    @Override
    public Map<PID, Long> vector()
    {
        return vector;
    }

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
