package com.internal.core.kernel.threadmanager;

import java.util.concurrent.Future;
import com.internal.core.engine.AsyncContainerPackage;
import com.internal.core.engine.SyncContainerPackage;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.ThreadHandle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class InternalThreadManager extends ManagerPackage {
    // Internal
    private InternalLoadManager internalLoadManager;
    private InternalBuildSystem internalBuildSystem;
    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, ThreadHandle> threadName2ThreadHandle;

    // Base \\
    @Override
    protected void create() {
        // Internal
        this.internalLoadManager = create(InternalLoadManager.class);
        this.internalBuildSystem = create(InternalBuildSystem.class);
        // Retrieval Mapping
        this.threadName2ThreadHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void awake() {
        compileThreadData();
    }

    @Override
    protected void release() {
        this.internalLoadManager = release(InternalLoadManager.class);
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    @Override
    protected void dispose() {
        for (ThreadHandle handle : threadName2ThreadHandle.values())
            handle.dispose();
        threadName2ThreadHandle.clear();
    }

    // Thread Management \\
    void compileThreadData() {
        internalLoadManager.loadThreadData();
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

    public <T extends AsyncContainerPackage> Future<?> executeAsync(
            ThreadHandle handle,
            AsyncContainerPackage asyncStruct,
            AsyncStructConsumer<T> consumer) {
        return executeAsync(handle, () -> {
            T instance = asyncStruct.getInstance();
            try {
                consumer.accept(instance);
            } finally {
                instance.reset();
            }
        });
    }

    public Future<?> executeAsync(
            ThreadHandle handle,
            AsyncStructConsumerMulti consumer,
            AsyncContainerPackage... asyncStructs) {
        return executeAsync(handle, () -> {
            AsyncContainerPackage[] instances = new AsyncContainerPackage[asyncStructs.length];
            for (int i = 0; i < asyncStructs.length; i++)
                instances[i] = asyncStructs[i].getInstance();
            try {
                consumer.accept(instances);
            } finally {
                for (AsyncContainerPackage instance : instances)
                    instance.reset();
            }
        });
    }

    public <T extends SyncContainerPackage> Future<?> executeAsync(
            ThreadHandle handle,
            SyncContainerPackage syncStruct,
            SyncStructConsumer<T> consumer) {
        return executeAsync(handle, () -> {
            if (syncStruct.tryAcquire()) {
                try {
                    T instance = syncStruct.getInstance();
                    consumer.accept(instance);
                } finally {
                    syncStruct.release();
                }
            }
        });
    }

    public <T extends AsyncContainerPackage, S extends SyncContainerPackage> Future<?> executeAsync(
            ThreadHandle handle,
            AsyncContainerPackage asyncStruct,
            SyncContainerPackage syncStruct,
            BiSyncAsyncConsumer<T, S> consumer) {

        return executeAsync(handle, () -> {
            T asyncInstance = asyncStruct.getInstance();
            try {
                if (syncStruct.tryAcquire()) {
                    try {
                        S syncInstance = syncStruct.getInstance();
                        consumer.accept(asyncInstance, syncInstance);
                    } finally {
                        syncStruct.release();
                    }
                }
            } finally {
                asyncInstance.reset();
            }
        });
    }

    // Functional Interfaces \\

    @FunctionalInterface
    public interface AsyncStructConsumer<T extends AsyncContainerPackage> {
        void accept(T instance);
    }

    @FunctionalInterface
    public interface AsyncStructConsumerMulti {
        void accept(AsyncContainerPackage[] instances);
    }

    @FunctionalInterface
    public interface SyncStructConsumer<T extends SyncContainerPackage> {
        void accept(T instance);
    }

    @FunctionalInterface
    public interface BiSyncAsyncConsumer<T extends AsyncContainerPackage, S extends SyncContainerPackage> {
        void accept(T asyncInstance, S syncInstance);
    }
}