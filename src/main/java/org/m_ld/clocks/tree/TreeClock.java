package org.m_ld.clocks.tree;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class TreeClock
{
    private final boolean isId;
    private final long ticks;
    private final Fork fork;

    public static class Fork
    {
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
            return o instanceof Fork && left.equals(((Fork) o).left) && right.equals(((Fork) o).right);
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

    public static final TreeClock GENESIS = new TreeClock(true, 0, null);
    private static final TreeClock HALLOWS = new TreeClock(false, 0, null);

    public long ticks()
    {
        return zeroIfNull(ticks(true));
    }

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

    public TreeClock update(TreeClock other)
    {
        if (isId)
        {
            if (other.isId)
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

    @Override
    public boolean equals(Object o)
    {
        return o instanceof TreeClock &&
            isId == ((TreeClock) o).isId &&
            ticks == ((TreeClock) o).ticks &&
            Objects.equals(fork, ((TreeClock) o).fork);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(isId, ticks, fork);
    }

    @Override
    public String toString()
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