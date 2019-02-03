package org.example;

import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

public abstract class ConvergenceTest<P>
{
    private final Set<P> processes;
    protected final int iterationTarget, tickMillis;

    protected final Timer timer = new Timer(this.getClass().getSimpleName());
    protected final Random random = new Random(); // Happy with contention here
    protected final CountDownLatch done;

    public ConvergenceTest(Set<P> processes, int iterationTarget, int tickMillis, int countsPerProxyTask)
    {
        if (countsPerProxyTask < 1)
            throw new IllegalArgumentException("At least one count per process task");

        this.processes = processes;
        this.iterationTarget = iterationTarget;
        this.tickMillis = tickMillis;
        this.done = new CountDownLatch(processes.size() * countsPerProxyTask * iterationTarget);
    }

    public ConvergenceTest(Set<P> processes, int countsPerProxyTask)
    {
        this(processes, 50, 10, countsPerProxyTask);
    }

    public void run() throws InterruptedException
    {
        processes.forEach(p -> timer.schedule(new ProcessTask(p, 1), random.nextInt(tickMillis)));
        done.await();
    }

    protected abstract Runnable iteration(P process);

    private class ProcessTask extends TimerTask
    {
        final P process;
        final Runnable iteration;
        final int i;

        public ProcessTask(P process, int i)
        {
            this.process = process;
            this.iteration = iteration(process);
            this.i = i;
        }

        @Override
        public void run()
        {
            iteration.run();

            if (i < iterationTarget)
                timer.schedule(new ProcessTask(process, i + 1), random.nextInt(tickMillis));

            done.countDown();
        }
    }
}
