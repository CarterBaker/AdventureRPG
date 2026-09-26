package application.kernel.threadpipeline.threadmanager;

import java.util.concurrent.Future;

import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.EngineUtility;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class ThreadManager extends ManagerPackage {

    /*
     * Owns every named thread pool and is the single dispatch point for async
     * work. executeAsync() tracks each task in flight for backpressure and
     * carries the submitting context's crash boundary onto the worker.
     */

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, ThreadHandle> threadName2ThreadHandle;

    // Base \\

    @Override
    protected void create() {
        create(ThreadLoader.class);
        this.threadName2ThreadHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void awake() {
        EngineUtility.assignThreadManager(this);
    }

    @Override
    protected void dispose() {
        for (ThreadHandle handle : threadName2ThreadHandle.values())
            handle.dispose();
        threadName2ThreadHandle.clear();
    }

    // On-Demand Loading \\

    public void request(String resourceName) {
        ((ThreadLoader) internalLoader).request(resourceName);
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
        Runnable isolatedTask = internal.isolateAsync(task);
        handle.beginTask();
        return handle.getExecutor().submit(() -> {
            try {
                isolatedTask.run();
            } finally {
                handle.endTask();
            }
        });
    }
}