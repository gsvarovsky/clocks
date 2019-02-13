package org.example;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

public class ConvergenceTest
{
    private final int iterationTarget, tickMillis;
    private final Timer timer = new Timer(this.getClass().getSimpleName());
    private final Random random = new Random(); // Happy with contention here
    private final CountDownLatch done;

    public ConvergenceTest(int processes, int iterationTarget, int tickMillis, int countsPerProcess)
    {
        if (countsPerProcess < 1)
            throw new IllegalArgumentException("At least one count per process task");

        this.iterationTarget = iterationTarget;
        this.tickMillis = tickMillis;
        this.done = new CountDownLatch(processes * countsPerProcess * iterationTarget);
    }

    public ConvergenceTest(int processes, int countsPerProcess)
    {
        this(processes, 50, 10, countsPerProcess);
    }

    public ConvergenceTest(int processes)
    {
        this(processes, 1);
    }

    public void run(Stream<? extends Runnable> processes) throws InterruptedException
    {
        processes.forEach(p -> schedule(new ProcessTask(p, 1)));
        done.await();
    }

    public void schedule(Runnable task)
    {
        timer.schedule(new TimerTask()
        {
            @Override public void run()
            {
                task.run();
                done.countDown();
            }
        }, random.nextInt(tickMillis));
    }

    public Random random()
    {
        return random;
    }

    private class ProcessTask implements Runnable
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
                schedule(new ProcessTask(iteration, i + 1));
        }
    }
}
