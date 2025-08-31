package com.AdventureRPG.ThreadManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;

public class ThreadManager {

    // Game Manager
    private final Settings settings;

    // Settings
    private final int AI_AVAILABLE_THREADS;
    private final int GENERATION_AVAILABLE_THREADS;
    private final int GENERAL_AVAILABLE_THREADS;

    public final boolean hasAiThread;
    public final boolean hasGenerationThread;
    public final boolean hasGeneralPurposeThread;

    // Threads
    private ExecutorService aiExecutor;
    private ExecutorService generationExecutor;
    private ExecutorService generalPurposeExecutor;

    // Base \\

    public ThreadManager(GameManager gameManager) {

        // Game Manager
        this.settings = gameManager.settings;

        // Settings
        this.AI_AVAILABLE_THREADS = settings.AI_AVAILABLE_THREADS;
        this.GENERATION_AVAILABLE_THREADS = settings.GENERATION_AVAILABLE_THREADS;
        this.GENERAL_AVAILABLE_THREADS = settings.GENERAL_AVAILABLE_THREADS;

        this.hasAiThread = AI_AVAILABLE_THREADS > 0;
        this.hasGenerationThread = GENERATION_AVAILABLE_THREADS > 0;
        this.hasGeneralPurposeThread = GENERAL_AVAILABLE_THREADS > 0;

        dedicateThreads();
    }

    private void dedicateThreads() {

        // AI threads
        if (!hasAiThread)
            return;

        this.aiExecutor = Executors.newFixedThreadPool(

                AI_AVAILABLE_THREADS,

                new java.util.concurrent.ThreadFactory() {

                    private int count = 1;

                    @Override
                    public Thread newThread(Runnable r) {

                        Thread t = new Thread(r, "AI-Thread-" + count++);
                        t.setPriority(Thread.NORM_PRIORITY);

                        return t;
                    }
                });

        // Generation threads
        if (!hasGenerationThread)
            return;

        this.generationExecutor = Executors.newFixedThreadPool(

                GENERATION_AVAILABLE_THREADS,

                new java.util.concurrent.ThreadFactory() {

                    private int count = 1;

                    @Override
                    public Thread newThread(Runnable r) {

                        Thread t = new Thread(r, "Generation-Thread-" + count++);
                        t.setPriority(Thread.NORM_PRIORITY);

                        return t;
                    }
                });

        // General-purpose threads
        if (!hasGeneralPurposeThread)
            return;

        this.generalPurposeExecutor = Executors.newFixedThreadPool(

                GENERAL_AVAILABLE_THREADS,

                new java.util.concurrent.ThreadFactory() {

                    private int count = 1;

                    @Override
                    public Thread newThread(Runnable r) {

                        Thread t = new Thread(r, "General-Thread-" + count++);
                        t.setPriority(Thread.NORM_PRIORITY);

                        return t;
                    }
                });
    }

    public void awake() {

    }

    public void start() {

    }

    public void update() {

    }

    public void dispose() {

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

    // Threads \\

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

    // Utility \\

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
