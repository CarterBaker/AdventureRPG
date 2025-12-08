package com.AdventureRPG.core.threadpipeline;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.AdventureRPG.core.kernel.SystemFrame;

public class ThreadSystem extends SystemFrame {

    // Settings
    private int AI_AVAILABLE_THREADS;
    private int GENERATION_AVAILABLE_THREADS;
    private int GENERAL_AVAILABLE_THREADS;

    private boolean hasAiThread;
    private boolean hasGenerationThread;
    private boolean hasGeneralPurposeThread;

    // Threads
    private ExecutorService aiExecutor;
    private ExecutorService generationExecutor;
    private ExecutorService generalPurposeExecutor;

    @Override
    public void create() {

        // Settings
        this.AI_AVAILABLE_THREADS = settings.AI_AVAILABLE_THREADS;
        this.GENERATION_AVAILABLE_THREADS = settings.GENERATION_AVAILABLE_THREADS;
        this.GENERAL_AVAILABLE_THREADS = settings.GENERAL_AVAILABLE_THREADS;

        this.hasAiThread = AI_AVAILABLE_THREADS > 0;
        this.hasGenerationThread = GENERATION_AVAILABLE_THREADS > 0;
        this.hasGeneralPurposeThread = GENERAL_AVAILABLE_THREADS > 0;
    }

    @Override
    protected void awake() {

        // Threads
        initAIExecutor();
        initGenerationExecutor();
        initGeneralExecutor();
    }

    private void initAIExecutor() {

        if (!hasAiThread)
            return;

        this.aiExecutor = Executors.newFixedThreadPool(
                AI_AVAILABLE_THREADS,
                new NamedThreadFactory("AI-Thread-"));
    }

    private void initGenerationExecutor() {

        if (!hasGenerationThread)
            return;

        this.generationExecutor = Executors.newFixedThreadPool(
                GENERATION_AVAILABLE_THREADS,
                new NamedThreadFactory("Generation-Thread-"));
    }

    private void initGeneralExecutor() {

        if (!hasGeneralPurposeThread)
            return;

        this.generalPurposeExecutor = Executors.newFixedThreadPool(
                GENERAL_AVAILABLE_THREADS,
                new NamedThreadFactory("General-Thread-"));
    }

    static class NamedThreadFactory implements java.util.concurrent.ThreadFactory {

        private final String baseName;
        private int count = 1;

        NamedThreadFactory(String baseName) {
            this.baseName = baseName;
        }

        @Override
        public Thread newThread(Runnable r) {

            Thread t = new Thread(r, baseName + count++);
            t.setPriority(Thread.NORM_PRIORITY);

            return t;
        }
    }

    @Override
    protected void dispose() {

        shutdownExecutor(aiExecutor);
        shutdownExecutor(generationExecutor);
        shutdownExecutor(generalPurposeExecutor);
    }

    private void shutdownExecutor(ExecutorService executor) {

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

    // Accessible \\

    // AI tasks
    public Future<?> submitAI(Runnable task) {
        return submitTask(hasAiThread, aiExecutor, task);
    }

    // Chunk generation tasks
    public Future<?> submitGeneration(Runnable task) {
        return submitTask(hasGenerationThread, generationExecutor, task);
    }

    // General tasks
    public Future<?> submitGeneral(Runnable task) {
        return submitTask(hasGeneralPurposeThread, generalPurposeExecutor, task);
    }

    private Future<?> submitTask(boolean hasThread, ExecutorService executor, Runnable task) {

        if (!isExecutorAvailable(hasThread, executor)) {

            task.run(); // fallback to main thread
            return CompletableFuture.completedFuture(null);
        }

        return executor.submit(task);
    }

    private boolean isExecutorAvailable(boolean hasThreadFlag, ExecutorService executor) {
        return hasThreadFlag && executor != null && !executor.isShutdown() && !executor.isTerminated();
    }
}
