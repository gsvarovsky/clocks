package org.m_ld.clocks;

public interface CausalClock<T>
{
    /**
     * Are any of the ticks for this clock less than the equivalent ticks for the other clock?
     *
     * @param other another clock
     * @return {@code true} if any of the ticks for this clock are less than the ticks for the other clock.
     */
    boolean anyLt(T other);
}
