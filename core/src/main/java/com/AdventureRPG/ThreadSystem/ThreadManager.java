package com.AdventureRPG.ThreadSystem;

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
    private final int AVAILABLE_THREADS;

    // Threads
    public final boolean hasAiThread;
    public final boolean hasGenerationThread;
    public final boolean hasGeneralPurposeThread;

    private ExecutorService aiExecutor;
    private ExecutorService generationExecutor;
    private ExecutorService generalPurposeExecutor;

    // Base \\

    public ThreadManager(GameManager gameManager) {

        // Game Manager
        this.settings = gameManager.settings;

        // Settings
        this.AVAILABLE_THREADS = settings.AVAILABLE_THREADS;

        this.hasAiThread = AVAILABLE_THREADS > 0;
        this.hasGenerationThread = AVAILABLE_THREADS > 1;
        this.hasGeneralPurposeThread = AVAILABLE_THREADS > 2;

        dedicateThreads();
    }

    private void dedicateThreads() {

        // Dedicated AI thread
        if (AVAILABLE_THREADS < 1)
            return;

        this.aiExecutor = Executors.newSingleThreadExecutor(r -> {

            Thread t = new Thread(r, "AI-Thread");
            t.setPriority(Thread.NORM_PRIORITY);

            return t;
        });

        // Dedicated generation thread
        if (AVAILABLE_THREADS < 2)
            return;

        this.generationExecutor = Executors.newSingleThreadExecutor(r -> {

            Thread t = new Thread(r, "Generation-Thread");
            t.setPriority(Thread.NORM_PRIORITY);

            return t;
        });

        // General-purpose threads
        if (AVAILABLE_THREADS < 3)
            return;

        this.generalPurposeExecutor = Executors.newFixedThreadPool(

                settings.AVAILABLE_THREADS - 2,

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

    // Centralized Fallback Logic
    private Future<?> submitTask(boolean hasThread, ExecutorService executor, Runnable task) {

        if (!isExecutorAvailable(hasThread, executor)) {

            task.run(); // fallback to main thread
            return CompletableFuture.completedFuture(null);
        }

        return executor.submit(task);
    }

    // Utility \\

    private boolean isExecutorAvailable(boolean hasThreadFlag, ExecutorService executor) {
        return hasThreadFlag && executor != null && !executor.isShutdown() && !executor.isTerminated();
    }
}
