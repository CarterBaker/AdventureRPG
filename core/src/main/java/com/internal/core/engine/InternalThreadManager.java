package com.internal.core.engine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalThreadManager extends ManagerPackage {

    // Internal
    private InternalThreadLoader internalThreadLoader;
    private InternalThreadBuilder internalThreadBuilder;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, ThreadHandle> threadName2ThreadHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalThreadLoader = create(InternalThreadLoader.class);
        this.internalThreadBuilder = create(InternalThreadBuilder.class);

        // Retrieval Mapping
        this.threadName2ThreadHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void awake() {
        compileThreadData();
    }

    @Override
    protected void release() {

        this.internalThreadLoader = release(InternalThreadLoader.class);
        this.internalThreadBuilder = release(InternalThreadBuilder.class);
    }

    @Override
    protected void dispose() {

        for (ThreadHandle handle : threadName2ThreadHandle.values())
            handle.dispose();

        threadName2ThreadHandle.clear();
    }

    // Thread Management \\

    void compileThreadData() {
        internalThreadLoader.loadThreadData();
    }

    void addThreadHandle(String threadName, ThreadHandle threadHandle) {
        threadName2ThreadHandle.put(threadName, threadHandle);
    }

    // Accessible \\

    public ThreadHandle getThreadHandleFromThreadName(String threadName) {
        return threadName2ThreadHandle.get(threadName);
    }

    public Future<?> executeAsync(ThreadHandle handle, Runnable task) {
        return handle.getExecutor().submit(task);
    }

    public <T extends AsyncStructPackage> Future<?> executeAsync(
            ThreadHandle handle,
            AsyncStructPackage asyncStruct,
            AsyncStructConsumer<T> consumer) {

        return executeAsync(handle, () -> {

            T instance = asyncStruct.getInstance();

            try {
                consumer.accept(instance);
            }

            finally {
                instance.internalReset();
            }
        });
    }

    public Future<?> executeAsync(
            ThreadHandle handle,
            AsyncStructConsumerMulti consumer,
            AsyncStructPackage... asyncStructs) {

        return executeAsync(handle, () -> {

            AsyncStructPackage[] instances = new AsyncStructPackage[asyncStructs.length];

            for (int i = 0; i < asyncStructs.length; i++)
                instances[i] = asyncStructs[i].getInstance();

            try {
                consumer.accept(instances);
            }

            finally {

                for (AsyncStructPackage instance : instances)
                    instance.internalReset();
            }
        });
    }

    // Functional Interfaces \\

    @FunctionalInterface
    public interface AsyncStructConsumer<T extends AsyncStructPackage> {
        void accept(T instance);
    }

    @FunctionalInterface
    public interface AsyncStructConsumerMulti {
        void accept(AsyncStructPackage[] instances);
    }
}

// Loader \\

class InternalThreadLoader extends SystemPackage {

    // Internal
    private File root;
    private InternalThreadManager threadManager;
    private InternalThreadBuilder threadBuilder;

    // Base \\

    @Override
    protected void create() {

        this.root = new File(EngineSetting.THREAD_DEFINITIONS);
    }

    @Override
    protected void get() {

        this.threadManager = get(InternalThreadManager.class);
        this.threadBuilder = get(InternalThreadBuilder.class);
    }

    // Thread Data Management \\

    void loadThreadData() {

        List<File> jsonFiles = collectJsonFiles();

        for (File file : jsonFiles)
            processJsonFile(file);
    }

    // File Collection \\

    private List<File> collectJsonFiles() {

        validateRootDirectory();

        Path basePath = root.toPath();

        try (var stream = Files.walk(basePath)) {

            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(this::isValidJsonFile)
                    .collect(Collectors.toList());
        }

        catch (IOException e) {
            throwException("Failed to collect thread definition files from: " + root.getAbsolutePath(), e);
        }

        return null;
    }

    private void validateRootDirectory() {

        if (!root.exists() || !root.isDirectory())
            throwException("Thread definitions directory not found: " + root.getAbsolutePath());
    }

    private boolean isValidJsonFile(File file) {
        return FileUtility.hasExtension(file, EngineSetting.JSON_FILE_EXTENSIONS);
    }

    // File Processing \\

    private void processJsonFile(File file) {

        JsonObject json = JsonUtility.loadJsonObject(file);

        if (!json.has("threads"))
            return;

        JsonArray threads = JsonUtility.validateArray(json, "threads");

        for (int i = 0; i < threads.size(); i++)
            processThreadDefinition(threads.get(i).getAsJsonObject());
    }

    private void processThreadDefinition(JsonObject threadDef) {

        String threadName = JsonUtility.validateString(threadDef, "name");
        int threadSize = threadDef.get("size").getAsInt();

        if (threadSize <= 0)
            return;

        ThreadHandle threadHandle = threadBuilder.buildThreadHandle(threadName, threadSize);

        if (threadHandle != null)
            createThreadData(threadName, threadHandle);
    }

    private void createThreadData(String threadName, ThreadHandle threadHandle) {
        threadManager.addThreadHandle(threadName, threadHandle);
    }
}

// Builder \\

class InternalThreadBuilder extends SystemPackage {

    // Build \\

    ThreadHandle buildThreadHandle(String threadName, int threadSize) {

        ExecutorService executor = Executors.newFixedThreadPool(
                threadSize,
                new NamedThreadFactory(threadName + "-"));

        ThreadHandle threadHandle = create(ThreadHandle.class);
        threadHandle.constructor(threadName, threadSize, executor);

        return threadHandle;
    }

    // Thread Factory \\

    private static class NamedThreadFactory implements java.util.concurrent.ThreadFactory {

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
}