/*
 * Copyright (c) George Svarovsky 2020. All rights reserved.
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package org.m_ld.clocks.tree;

import org.m_ld.clocks.CausalClock;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * A clock in which process identities are forked from each other, with the option to re-merge.
 * Like a {@link org.m_ld.clocks.vector.VectorClock}, a tree clock defines and carries its own process identity,
 * as well as being able to be updated with ticks for other process identities.
 * <p>
 * In comparison to Vector Clocks, this method results in much less data being sent on the wire.
 * This implementation is simplified from <a href="http://gsd.di.uminho.pt/members/cbm/ps/itc2008.pdf">
 * Interval Tree Clocks
 * </a>, which separate the Identity and Event portions of the clock and so offer even more opportunity for
 * compression, but make it harder to exclude the sender and receiver from consideration when comparing.
 * <p>
 * This implementation is immutable and so thread-safe.
 */
public class TreeClock implements CausalClock<TreeClock>, Serializable
{
    private static final long serialVersionUID = 1L;
    private final boolean isId;
    private final long ticks;
    private final Fork fork;

    /**
     * An immutable pair of related clocks
     */
    public static class Fork implements Serializable
    {
        private static final long serialVersionUID = 1L;
        public final TreeClock left, right;

        private Fork(TreeClock left, TreeClock right)
        {
            assert left != null && right != null;

            this.left = left;
            this.right = right;
        }

        @Override
        public boolean equals(Object o)
        {
            return o instanceof Fork && left.equals(((Fork)o).left) && right.equals(((Fork)o).right);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(left, right);
        }

        @Override
        public String toString()
        {
            return format("{ %s, %s }", left.briefString(), right.briefString());
        }
    }

    /**
     * A leaf clock to be used as a starting point.
     * In general, a real process will probably need to also carry around an identifier for its process group,
     * unless it can guarantee not to ever receive messages from other process groups.
     */
    public static final TreeClock GENESIS = new TreeClock(true, 0, null);
    /**
     * A leaf clock with no process identity, used to scrub out IDs when merging and updating.
     */
    private static final TreeClock HALLOWS = new TreeClock(false, 0, null);

    /**
     * @return {@code true} if this clock has never been forked
     */
    public boolean isId()
    {
        return isId;
    }

    /**
     * @return the ticks for this clock. This includes only ticks for this clock's ID
     * @see #ticks(Boolean)
     */
    public long ticks()
    {
        return zeroIfNull(ticks(true));
    }

    /**
     * @param forId {@code true} to gather ticks for this clock's process identity;
     *              {@code false} for the union of all other process identities (like an inverse)
     * @return ticks for this clock or all other clocks
     */
    public Long ticks(Boolean forId)
    {
        if (forId == null || forId.equals(isId))
        {
            return ticks + (fork == null ? 0L :
                zeroIfNull(fork.left.ticks(forId == null || forId ? null : false)) +
                    zeroIfNull(fork.right.ticks(forId == null || forId ? null : false)));
        }
        else if (fork != null)
        {
            final Long leftResult = fork.left.ticks(forId), rightResult = fork.right.ticks(forId);
            if (leftResult != null || rightResult != null)
                return ticks + zeroIfNull(leftResult) + zeroIfNull(rightResult);
        }
        return null;
    }

    /**
     * @return a new tree clock with this clock's process identity and one additional tick; thus,
     * <code>this.tick().ticks() == this.ticks() + 1</code>
     */
    public TreeClock tick()
    {
        if (isId)
        {
            return new TreeClock(true, ticks + 1L, fork);
        }
        else if (fork != null)
        {
            final TreeClock leftResult = fork.left.tick();
            if (leftResult != null)
                return new TreeClock(false, ticks, new Fork(leftResult, fork.right));

            final TreeClock rightResult = fork.right.tick();
            if (rightResult != null)
                return new TreeClock(false, ticks, new Fork(fork.left, rightResult));
        }
        return null;
    }

    /**
     * Forks this clock, copying its state into the result's left and right branches, which have distinct process
     * identities.
     * This clock should normally be discarded, and the process's state replaced with either the left or right
     * of the result. This is because a clock can never be updated from any of its forks.
     *
     * @return a Fork of this clock
     * @see #update(TreeClock)
     */
    public Fork fork()
    {
        if (isId)
        {
            return new Fork(
                new TreeClock(false, ticks, new Fork(
                    new TreeClock(true, 0, this.fork),
                    new TreeClock(false, 0, this.fork))),
                new TreeClock(false, ticks, new Fork(
                    new TreeClock(false, 0, this.fork),
                    new TreeClock(true, 0, this.fork)))
            );
        }
        else if (fork != null)
        {
            final Fork leftResult = fork.left.fork();
            if (leftResult != null)
                return new Fork(
                    new TreeClock(false, ticks, new Fork(leftResult.left, this.fork.right)),
                    new TreeClock(false, ticks, new Fork(leftResult.right, this.fork.right))
                );

            final Fork rightResult = fork.right.fork();
            if (rightResult != null)
                return new Fork(
                    new TreeClock(false, ticks, new Fork(this.fork.left, rightResult.left)),
                    new TreeClock(false, ticks, new Fork(this.fork.left, rightResult.right))
                );
        }
        return null;
    }

    /**
     * Update this clock with another clock's ticks.
     * This method requires that the other clock's process identity does not overlap this one's.
     *
     * @param other another clock with a non-overlapping process identity, i.e. from a distinct branch
     * @return a clock with this clock's process identity but including the other clock's ticks
     */
    public TreeClock update(TreeClock other)
    {
        if (isId)
        {
            if (other.isId && other.ticks > ticks)
                throw new IllegalArgumentException("Trying to update from overlapping clock");
            return this;
        }
        else
        {
            return new TreeClock(
                false, Math.max(ticks, other.ticks),
                other.fork == null ? fork :
                    new Fork((fork == null ? HALLOWS : fork.left).update(other.fork.left),
                             (fork == null ? HALLOWS : fork.right).update(other.fork.right)));
        }
    }

    /**
     * Merges this clock's process identity with another clock's process identity.
     * This method does <b>not</b> merge in the other clock's ticks. This gives the caller the opportunity
     * to compare merged clocks before and after update.
     *
     * @param other another clock
     * @return a clock with this clock's ticks and a merged process identity
     */
    public TreeClock mergeId(TreeClock other)
    {
        if (fork != null && other.fork != null)
        {
            final TreeClock left = fork.left.mergeId(other.fork.left), right = fork.right.mergeId(other.fork.right);
            if (left.isId && right.isId)
            {
                return new TreeClock(true, ticks + left.ticks() + right.ticks(), null);
            }
            else
            {
                return new TreeClock(isId || other.isId, ticks, new Fork(left, right));
            }
        }
        else if (fork != null)
        {
            return new TreeClock(isId || other.isId, ticks, fork);
        }
        else
        {
            return new TreeClock(isId || other.isId, ticks, other.fork == null ? null :
                new Fork(HALLOWS.mergeId(other.fork.left), HALLOWS.mergeId(other.fork.right)));
        }
    }

    /**
     * Are any of the ticks for this clock less than the equivalent ticks for the other clock?
     *
     * @param other another clock
     * @return {@code true} if any of the ticks for this clock are less than the ticks for the other clock.
     */
    @Override public boolean anyLt(TreeClock other)
    {
        if (fork == null || other.fork == null)
        {
            if (!isId && !other.isId)
            {
                return ticks(false) < other.ticks(false);
            }
            else
            {
                return false; // Either is an ID but we don't want IDs, or both not IDs and we want IDs
            }
        }
        else
        {
            return fork.left.anyLt(other.fork.left) || fork.right.anyLt(other.fork.right);
        }
    }

    @Override public boolean equals(Object o)
    {
        return o instanceof TreeClock &&
            isId == ((TreeClock)o).isId &&
            ticks == ((TreeClock)o).ticks &&
            Objects.equals(fork, ((TreeClock)o).fork);
    }

    @Override public int hashCode()
    {
        return Objects.hash(isId, ticks, fork);
    }

    @Override public String toString()
    {
        return "TreeClock " + briefString();
    }

    public String briefString()
    {
        final List<String> content = Stream.of(isId ? "ID" : null, ticks > 0 ? ticks : null, fork)
            .filter(Objects::nonNull).map(Object::toString).collect(toList());
        return content.size() == 1 ? content.get(0) : content.toString();
    }

    private TreeClock(boolean isId, long ticks, Fork fork)
    {
        this.isId = isId;
        this.ticks = ticks;
        this.fork = fork;
    }

    private static long zeroIfNull(Long value)
    {
        return value == null ? 0L : value;
    }
}