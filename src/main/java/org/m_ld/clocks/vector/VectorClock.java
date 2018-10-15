package org.m_ld.clocks.vector;

import org.m_ld.clocks.CausalClock;

import java.util.Map;

public interface VectorClock<PID> extends CausalClock<VectorClock<PID>>
{
    /**
     * @return the ID of the process from which this vector clock originated.
     */
    PID processId();

    /**
     * @return the vector of process clocks. Must not include null values.
     */
    Map<PID, Long> vector();

    /**
     * Obtains the ticks for a process. If the process is unknown, the default return value of <code>-1</code>
     * indicates that all messages relating to that process should be buffered until a message arrives which originated
     * from it, thus providing its initial state.
     * @param pid a Process ID
     * @return the number of ticks on that process's clock
     */
    default long ticks(PID pid)
    {
        return vector().getOrDefault(pid, -1L);
    }

    @Override default boolean anyLt(VectorClock<PID> other)
    {
        return other.vector().entrySet().stream()
            .filter(e -> !pid(e).equals(processId()) && !pid(e).equals(other.processId()))
            .anyMatch(e -> ticks(pid(e)) < e.getValue());
    }

    static <PID> PID pid(Map.Entry<PID, Long> e)
    {
        return e.getKey();
    }
}
