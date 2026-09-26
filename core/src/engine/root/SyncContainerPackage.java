package engine.root;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public abstract class SyncContainerPackage extends InstancePackage {

    /*
     * Shared state guarded by a single non-reentrant lock. Worker threads take
     * it with tryAcquire() or acquire() before touching the container and hand
     * it back with release(), which also runs reset(). The lock is one int on
     * the instance itself, so pooled containers carry no extra objects.
     */

    // Synchronization
    private static final VarHandle LOCKED;

    private volatile int locked;

    static {
        try {
            LOCKED = MethodHandles.lookup().findVarHandle(SyncContainerPackage.class, "locked", int.class);
        } catch (ReflectiveOperationException e) {
            throw new InternalException("Failed to bind SyncContainerPackage lock handle", e);
        }
    }

    // Constructor \\

    protected SyncContainerPackage() {
        super();
    }

    // Lock \\

    public final boolean tryAcquire() {
        return LOCKED.compareAndSet(this, 0, 1);
    }

    public final void acquire() {
        while (!tryAcquire())
            Thread.onSpinWait();
    }

    public final void release() {
        reset();
        this.locked = 0;
    }

    public final boolean isLocked() {
        return locked != 0;
    }

    // Lifecycle \\

    public void reset() {
    }
}
