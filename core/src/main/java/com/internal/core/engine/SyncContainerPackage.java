package com.internal.core.engine;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
public abstract class SyncContainerPackage extends InstancePackage {
    /*
     * SyncContainerPackage is a synchronized data container for shared
     * singleton resources requiring exclusive access. Uses ReentrantLock
     * for fast, predictable mutual exclusion.
     *
     * Unlike AsyncContainerPackage which creates per-thread instances,
     * SyncContainerPackage maintains a single shared instance with
     * automatic acquire/release through execute methods.
     *
     * Ideal for:
     * - Pooled resources (connections, file handles)
     * - Expensive-to-create shared objects
     * - Resources requiring exclusive access
     * - Shared caches and buffers
     *
     * Usage:
     * class SharedBufferSync extends SyncContainerPackage {
     *     byte[] buffer;
     *     int position;
     *     
     *     protected void create() {
     *         this.buffer = new byte[4096];
     *     }
     *     
     *     protected void reset() {
     *         this.position = 0;
     *         Arrays.fill(buffer, (byte) 0);
     *     }
     *     
     *     public void write(byte[] data) {
     *         System.arraycopy(data, 0, buffer, position, data.length);
     *         position += data.length;
     *     }
     * }
     *
     * // In a system:
     * SyncContainerPackage sharedBuffer = create(SharedBufferSync.class);
     * 
     * // Simple usage with automatic safety:
     * sharedBuffer.executeIfAvailable(buffer -> {
     *     buffer.write(myData);
     * });
     */
    // Synchronization
    private final ReentrantLock lock;
    private volatile Thread currentThread;
    // Construction \\
    protected SyncContainerPackage() {
        super();
        this.lock = new ReentrantLock();
        this.currentThread = null;
    }
    // High-level Execute API \\
    public final <T extends SyncContainerPackage> boolean executeIfAvailable(
            SyncStructConsumer<T> consumer) {
        if (lock.tryLock()) {
            currentThread = Thread.currentThread();
            try {
                consumer.accept(getInstance());
                return true;
            }
            finally {
                reset();
                currentThread = null;
                lock.unlock();
            }
        }
        return false;
    }
    public final <T extends SyncContainerPackage> void execute(
            SyncStructConsumer<T> consumer) {
        lock.lock();
        currentThread = Thread.currentThread();
        try {
            consumer.accept(getInstance());
        }
        finally {
            reset();
            currentThread = null;
            lock.unlock();
        }
    }
    public final <T extends SyncContainerPackage> boolean executeWithTimeout(
            SyncStructConsumer<T> consumer, long timeoutMillis) {
        try {
            if (lock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS)) {
                currentThread = Thread.currentThread();
                try {
                    consumer.accept(getInstance());
                    return true;
                }
                finally {
                    reset();
                    currentThread = null;
                    lock.unlock();
                }
            }
            return false;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    // Low-level Manual API \\
    public final boolean tryAcquire() {
        if (lock.tryLock()) {
            currentThread = Thread.currentThread();
            return true;
        }
        return false;
    }
    public final void acquire() {
        lock.lock();
        currentThread = Thread.currentThread();
    }
    public final void release() {
        if (!lock.isHeldByCurrentThread()) {
            throwException("Attempting to release lock not held by current thread");
            return;
        }
        try {
            reset();
        }
        finally {
            currentThread = null;
            lock.unlock();
        }
    }
    // Status \\
    public final boolean isInUse() {
        return lock.isLocked();
    }
    public final Thread getCurrentThread() {
        return currentThread;
    }
    public final boolean isHeldByCurrentThread() {
        return lock.isHeldByCurrentThread();
    }
    // Lifecycle \\
    public void reset() {
    }
    @SuppressWarnings("unchecked")
    public final <T extends SyncContainerPackage> T getInstance() {
        return (T) this;
    }
    // Functional Interface \\
    @FunctionalInterface
    public interface SyncStructConsumer<T extends SyncContainerPackage> {
        void accept(T instance);
    }
}