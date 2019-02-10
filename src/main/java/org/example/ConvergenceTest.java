package org.example;

import java.util.Collection;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

public abstract class ConvergenceTest<P>
{
    private final Collection<P> processes;
    protected final int iterationTarget, tickMillis;

    protected final Timer timer = new Timer(this.getClass().getSimpleName());
    protected final Random random = new Random(); // Happy with contention here
    protected final CountDownLatch done;

    public ConvergenceTest(Collection<P> processes, int iterationTarget, int tickMillis, int countsPerProcess)
    {
        if (countsPerProcess < 1)
            throw new IllegalArgumentException("At least one count per process task");

        this.processes = processes;
        this.iterationTarget = iterationTarget;
        this.tickMillis = tickMillis;
        this.done = new CountDownLatch(processes.size() * countsPerProcess * iterationTarget);
    }

    public ConvergenceTest(Collection<P> processes, int countsPerProcess)
    {
        this(processes, 50, 10, countsPerProcess);
    }

    public void run() throws InterruptedException
    {
        processes.forEach(p -> timer.schedule(new ProcessTask(iteration(p), 1), random.nextInt(tickMillis)));
        done.await();
    }

    protected abstract Runnable iteration(P process);

    private class ProcessTask extends TimerTask
    {
        final Runnable iteration;
        final int i;

        public ProcessTask(Runnable iteration, int i)
        {
            this.iteration = iteration;
            this.i = i;
        }

        @Override
        public void run()
        {
            iteration.run();

            if (i < iterationTarget)
                timer.schedule(new ProcessTask(iteration, i + 1), random.nextInt(tickMillis));

            done.countDown();
        }
    }
}
