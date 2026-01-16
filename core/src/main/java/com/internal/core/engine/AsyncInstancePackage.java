package com.internal.core.engine;

public abstract class AsyncInstancePackage extends InstancePackage {

    /*
     * AsyncInstancePackage is a thread-safe data container designed for
     * multi-threaded operations. Each thread automatically gets its own
     * isolated instance, preventing race conditions and eliminating the
     * need for synchronization locks.
     *
     * These are ideal for temporary buffers, calculation arrays, and any
     * mutable data structures used in concurrent tasks. After use, the
     * reset() method prepares the instance for reuse on the same thread.
     *
     * AsyncStructPackages must be created via the engine `create` method
     * and are typically used with submitWithReset() to ensure proper
     * lifecycle management and automatic cleanup.
     *
     * Usage:
     * class DynamicGeometryAsyncStruct extends AsyncInstancePackage {
     * float[] vertices;
     * 
     * protected void create() {
     * this.vertices = new float[1024];
     * }
     * 
     * protected void reset() {
     * Arrays.fill(vertices, 0);
     * }
     * }
     *
     * // In a system:
     * AsyncInstancePackage geometryData = create(DynamicGeometryAsyncStruct.class);
     * 
     * // Use with automatic reset:
     * threadManager.submitWithReset("Generation", geometryData, (data) -> {
     * DynamicGeometryAsyncStruct geo = (DynamicGeometryAsyncStruct) data;
     * // ... use geo
     * }); // reset() called automatically
     */

    // Internal
    private final ThreadLocal<Object> threadLocalInstance;

    // Internal \\

    protected AsyncInstancePackage() {

        super();

        this.threadLocalInstance = ThreadLocal.withInitial(() -> {
            try {
                return createThreadInstance();
            } catch (Exception e) {
                throwException("Failed to create thread-local instance: " + e.getMessage());
                return null;
            }
        });
    }

    // Thread Instance Creation \\

    private Object createThreadInstance() {

        try {

            // Create new instance for this thread
            InstancePackage.setupConstructor(this.internal, this.owner);

            var constructor = this.getClass().getDeclaredConstructor();
            constructor.setAccessible(true);

            AsyncInstancePackage instance = (AsyncInstancePackage) constructor.newInstance();

            // Run lifecycle
            instance.internalCreate();
            instance.internalGet();
            instance.internalAwake();

            return instance;
        }

        catch (Exception e) {
            throwException("Failed to create thread instance: " + e.getMessage());
            return null;
        }

        finally {
            InstancePackage.CREATION_STRUCT.remove();
        }
    }

    // reset \\

    public void reset() {
    }

    // Accessible \\

    /**
     * Get the instance for the current thread.
     * If this is the first access from this thread, a new instance is created
     * and goes through its lifecycle (create -> get -> awake).
     */
    @SuppressWarnings("unchecked")
    public final <T extends AsyncInstancePackage> T getInstance() {
        return (T) threadLocalInstance.get();
    }

    /**
     * Remove the current thread's instance.
     * Use when a thread is permanently done with this data.
     */
    public final void removeInstance() {
        threadLocalInstance.remove();
    }
}