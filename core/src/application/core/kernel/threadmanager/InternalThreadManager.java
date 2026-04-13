package application.core.kernel.threadmanager;

import java.util.concurrent.Future;

import application.core.engine.AsyncContainerPackage;
import application.core.engine.ManagerPackage;
import application.core.engine.SyncContainerPackage;
import application.core.kernel.syncconsumer.AsyncStructConsumer;
import application.core.kernel.syncconsumer.AsyncStructConsumerMulti;
import application.core.kernel.syncconsumer.BiSyncAsyncConsumer;
import application.core.kernel.syncconsumer.SyncStructConsumer;
import application.core.kernel.thread.ThreadHandle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class InternalThreadManager extends ManagerPackage {

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, ThreadHandle> threadName2ThreadHandle;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoader.class);
        this.threadName2ThreadHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void dispose() {
        for (ThreadHandle handle : threadName2ThreadHandle.values())
            handle.dispose();
        threadName2ThreadHandle.clear();
    }

    // On-Demand Loading \\

    public void request(String resourceName) {
        ((InternalLoader) internalLoader).request(resourceName);
    }

    // Thread Management \\

    void addThreadHandle(String threadName, ThreadHandle threadHandle) {
        threadName2ThreadHandle.put(threadName, threadHandle);
    }

    // Accessible \\

    public ThreadHandle getThreadHandleFromThreadName(String threadName) {

        ThreadHandle handle = threadName2ThreadHandle.get(threadName);

        if (handle == null) {

            request(threadName);
            handle = threadName2ThreadHandle.get(threadName);

            if (handle == null)
                throwException("[InternalThreadManager] Thread not found after load: \"" + threadName + "\"");
        }

        return handle;
    }

    public Future<?> executeAsync(ThreadHandle handle, Runnable task) {
        return handle.getExecutor().submit(task);
    }

    public <T extends AsyncContainerPackage> Future<?> executeAsync(
            ThreadHandle handle,
            T asyncStruct,
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
                for (int i = 0; i < instances.length; i++)
                    instances[i].reset();
            }
        });
    }

    public <T extends SyncContainerPackage> Future<?> executeAsync(
            ThreadHandle handle,
            T syncStruct,
            SyncStructConsumer<T> consumer) {
        return executeAsync(handle, () -> {
            if (syncStruct.tryAcquire()) {
                try {
                    consumer.accept(syncStruct.getInstance());
                } finally {
                    syncStruct.release();
                }
            }
        });
    }

    public <T extends AsyncContainerPackage, S extends SyncContainerPackage> Future<?> executeAsync(
            ThreadHandle handle,
            T asyncStruct,
            S syncStruct,
            BiSyncAsyncConsumer<T, S> consumer) {
        return executeAsync(handle, () -> {
            T asyncInstance = asyncStruct.getInstance();
            try {
                if (syncStruct.tryAcquire()) {
                    try {
                        consumer.accept(asyncInstance, syncStruct.getInstance());
                    } finally {
                        syncStruct.release();
                    }
                }
            } finally {
                asyncInstance.reset();
            }
        });
    }
}