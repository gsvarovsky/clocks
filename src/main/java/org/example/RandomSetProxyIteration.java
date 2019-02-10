package org.example;

import java.util.Random;
import java.util.Set;

public abstract class RandomSetProxyIteration<P extends SetProxy<E, O>, E, O> implements Runnable
{
    protected final Random random;
    protected P process;

    public RandomSetProxyIteration(P process, Random random)
    {
        this.process = process;
        this.random = random;
    }

    protected abstract E randomNewElement();

    protected void send(O operation)
    {
        // Hook to send an operation to a process. May not be required.
    }

    @Override public void run()
    {
        // Every iteration make a random decision whether to add or remove from the set, favouring add
        final Set<E> elements = process.elements();
        if (elements.isEmpty() || random.nextDouble() * 3 - 2/*(-2..1]*/ < 0)
            send(process.add(randomNewElement()));
        else
            send(process.remove(randomElement(elements)));
    }

    private E randomElement(Set<E> elements)
    {
        return elements.stream()
            .skip(random.nextInt(elements.size()))
            .findFirst().orElseThrow(AssertionError::new);
    }

    public static class RandomIntegerSetProxyIteration<P extends SetProxy<Integer, O>, O>
        extends RandomSetProxyIteration<P, Integer, O>
    {
        public RandomIntegerSetProxyIteration(P process, Random random)
        {
            super(process, random);
        }

        @Override protected Integer randomNewElement()
        {
            return random.nextInt();
        }
    }
}
