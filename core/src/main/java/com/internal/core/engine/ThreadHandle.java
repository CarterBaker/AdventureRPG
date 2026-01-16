package com.internal.core.engine;

import java.util.concurrent.ExecutorService;

public class ThreadHandle extends HandlePackage {

    // Internal
    private String threadName;
    private int threadSize;
    private ExecutorService executor;

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