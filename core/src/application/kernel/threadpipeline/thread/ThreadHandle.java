package application.kernel.threadpipeline.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import engine.root.HandlePackage;

public class ThreadHandle extends HandlePackage {

    /*
     * Wraps a named ExecutorService along with an in-flight task budget used
     * for pool-wide backpressure. inFlightCapacity bounds how many tasks may
     * be queued-or-running on this executor at once — callers are expected
     * to check hasCapacity() before submitting new async work through
     * ThreadManager.executeAsync, so the submitting side (typically the main
     * thread's per-frame streaming loop) naturally stalls new dispatch once
     * the pool is saturated instead of piling an unbounded backlog onto the
     * executor's own internal queue. A capacity of 0 or less disables the
     * check entirely (unbounded) — correct for pools never fed from a tight
     * per-frame loop.
     */

    // Internal
    private String threadName;
    private int threadSize;
    private ExecutorService executor;

    // Backpressure
    private int inFlightCapacity;
    private final AtomicInteger inFlightCount = new AtomicInteger(0);

    // Internal \\

    public void constructor(
            String threadName,
            int threadSize,
            ExecutorService executor) {

        // Internal
        this.threadName = threadName;
        this.threadSize = threadSize;
        this.executor = executor;
    }

    public void configureBackpressure(int inFlightCapacity) {
        this.inFlightCapacity = inFlightCapacity;
    }

    public void dispose() {

        if (executor != null && !executor.isShutdown()) {

            executor.shutdown();

            try {
                if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS))
                    executor.shutdownNow();
            }

            catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // Backpressure \\

    public boolean hasCapacity() {
        return inFlightCapacity <= 0 || inFlightCount.get() < inFlightCapacity;
    }

    public void beginTask() {
        inFlightCount.incrementAndGet();
    }

    public void endTask() {
        inFlightCount.decrementAndGet();
    }

    public int getInFlightCount() {
        return inFlightCount.get();
    }

    public int getInFlightCapacity() {
        return inFlightCapacity;
    }

    // Accessible \\

    public String getThreadName() {
        return threadName;
    }

    public int getThreadSize() {
        return threadSize;
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}