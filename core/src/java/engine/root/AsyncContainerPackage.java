package engine.root;

public abstract class AsyncContainerPackage extends InstancePackage {

    /*
     * Per-thread scratch container. Each worker thread lazily gets its own
     * instance through getInstance(), created and run through CREATE, GET and
     * AWAKE like any instance, so concurrent tasks share no mutable state and
     * need no locks. reset() prepares an instance for its thread's next task.
     */

    // Internal
    private final ThreadLocal<AsyncContainerPackage> threadLocalInstance;

    // Constructor \\

    protected AsyncContainerPackage() {
        super();
        this.threadLocalInstance = ThreadLocal.withInitial(this::createThreadInstance);
    }

    // Thread Instance \\

    private AsyncContainerPackage createThreadInstance() {

        try {
            InstancePackage.setupConstructor(this.internal, this.owner);

            var constructor = this.getClass().getDeclaredConstructor();
            constructor.setAccessible(true);

            AsyncContainerPackage instance = constructor.newInstance();
            instance.internalCreate();
            instance.internalGet();
            instance.internalAwake();

            return instance;
        } catch (Exception e) {
            return throwException("Failed to create thread instance: " + getClass().getSimpleName(), e);
        } finally {
            InstancePackage.CREATION_STRUCT.remove();
        }
    }

    // Reset \\

    public void reset() {
    }

    // Accessible \\

    @SuppressWarnings("unchecked")
    public final <T extends AsyncContainerPackage> T getInstance() {
        return (T) threadLocalInstance.get();
    }
}
