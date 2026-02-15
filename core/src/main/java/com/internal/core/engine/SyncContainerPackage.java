package com.internal.core.engine;

import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SyncContainerPackage extends InstancePackage {

    // Synchronization
    private final AtomicBoolean locked;

    // Construction \\

    protected SyncContainerPackage() {
        super();
        this.locked = new AtomicBoolean(false);
    }

    // Low-level Manual API \\

    public final boolean tryAcquire() {
        if (locked.compareAndSet(false, true)) {
            VarHandle.acquireFence(); // Memory barrier - see all updates from other threads
            return true;
        }
        return false;
    }

    public final void release() {
        VarHandle.releaseFence(); // Memory barrier - make all updates visible to other threads
        reset();
        locked.set(false);
    }

    // Status \\

    public final boolean isInUse() {
        return locked.get();
    }

    public final boolean isLocked() {
        return locked.get();
    }

    // Lifecycle \\

    public void reset() {
    }

    @SuppressWarnings("unchecked")
    public final <T extends SyncContainerPackage> T getInstance() {
        return (T) this;
    }
}