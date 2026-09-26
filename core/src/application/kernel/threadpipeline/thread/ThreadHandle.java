package application.kernel.threadpipeline.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import engine.root.HandlePackage;

public class ThreadHandle extends HandlePackage {

    /*
     * Wraps a named ExecutorService with an in-flight task budget. Callers
     * check hasCapacity() before dispatching per-frame work so a saturated pool
     * stalls new dispatch instead of growing an unbounded backlog; a capacity
     * of zero or less disables the check.
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
                if (!executor.awaitTermination(5, TimeUnit.SECONDS))
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