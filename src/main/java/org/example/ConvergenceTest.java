package org.example;

import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

public abstract class ConvergenceTest<P extends SetProxy<E, O>, E, O>
{
    private final Set<P> proxies;
    protected final int iterationTarget, tickMillis;

    protected final Timer timer = new Timer(this.getClass().getSimpleName());
    protected final Random random = new Random(); // Happy with contention here
    // Every iteration for every process produces one SetProxyTask#run and (processCount - 1) SetProxyTask#Deliver
    protected final CountDownLatch done;

    public ConvergenceTest(Set<P> proxies, int iterationTarget, int tickMillis, int countsPerProxyTask)
    {
        if (countsPerProxyTask < 1)
            throw new IllegalArgumentException("At least one count per process task");

        this.proxies = proxies;
        this.iterationTarget = iterationTarget;
        this.tickMillis = tickMillis;
        this.done = new CountDownLatch(proxies.size() * countsPerProxyTask * iterationTarget);
    }

    public ConvergenceTest(Set<P> proxies, int countsPerProxyTask)
    {
        this(proxies, 50, 10, countsPerProxyTask);
    }

    public void run() throws InterruptedException
    {
        proxies.forEach(p -> timer.schedule(new SetProxyTask(p, 1), random.nextInt(tickMillis)));
        done.await();
    }

    public interface OperationSpec<E, O>
    {
        E randomNewElement();

        default void send(O operation)
        {
            // Hook to send an operation to a replica. May not be required.
        }
    }

    protected abstract OperationSpec<E, O> operationSpec(P setProxy);

    private class SetProxyTask extends TimerTask
    {
        final P setProxy;
        final OperationSpec<E, O> operationSpec;
        final int iteration;

        public SetProxyTask(P setProxy, int iteration)
        {
            this.setProxy = setProxy;
            this.operationSpec = operationSpec(setProxy);
            this.iteration = iteration;
        }

        @Override
        public void run()
        {
            // Every iteration make a random decision whether to add or remove from the set, favouring add
            final Set<E> elements = setProxy.elements();
            if (elements.isEmpty() || random.nextDouble() * 3 - 2/*(-2..1]*/ < 0)
                operationSpec.send(setProxy.add(operationSpec.randomNewElement()));
            else
                operationSpec.send(setProxy.remove(randomElement(elements)));

            if (iteration < iterationTarget)
                timer.schedule(new SetProxyTask(setProxy, iteration + 1), random.nextInt(tickMillis));

            done.countDown();
        }

        private E randomElement(Set<E> elements)
        {
            return elements.stream()
                .skip(random.nextInt(elements.size()))
                .findFirst().orElseThrow(AssertionError::new);
        }
    }
}
