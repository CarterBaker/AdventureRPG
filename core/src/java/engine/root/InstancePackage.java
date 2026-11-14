package engine.root;

public abstract class InstancePackage extends UtilityPackage {

    /*
     * Lightweight engine-managed object owned by a system. Runs a short
     * CREATE, GET, AWAKE lifecycle and resolves dependencies through its
     * owner's context. Only ever instantiated through create(); direct
     * construction throws immediately.
     */

    // Internal
    static final ThreadLocal<CreationStruct> CREATION_STRUCT = new ThreadLocal<>();

    protected final EnginePackage internal;
    protected final SystemPackage owner;

    SystemContext systemContext;

    // Internal \\

    public InstancePackage() {

        // Internal
        CreationStruct creationData = CREATION_STRUCT.get();

        if (creationData == null)
            throwException("Instances must be created via internal engine `create` method");

        if (creationData.internal == null || creationData.owner == null)
            throwException("Cannot create instance without a proper internal engine or owner reference");

        this.internal = creationData.internal;
        this.owner = creationData.owner;

        this.systemContext = SystemContext.NULL;
    }

    static final class CreationStruct extends StructPackage {

        /*
         * Carries the engine and owning system through reflective construction.
         */

        // Internal
        final EnginePackage internal;
        final SystemPackage owner;

        // Internal \\

        CreationStruct(
                EnginePackage internal,
                SystemPackage owner) {

            // Internal
            this.internal = internal;
            this.owner = owner;
        }
    }

    static void setupConstructor(
            EnginePackage internal,
            SystemPackage owner) {
        CREATION_STRUCT.set(new CreationStruct(internal, owner));
    }

    // System Context \\

    final SystemContext getContext() {
        return systemContext;
    }

    final boolean verifyProcess(SystemContext targetContext) {

        if (!targetContext.canEnterFrom(this.systemContext.order))
            return false;

        this.requestContext(targetContext);
        return true;
    }

    final void requestContext(SystemContext targetContext) {

        if (!targetContext.canEnterFrom(this.systemContext.order))
            throwException("Instance attempted to perform an illegal context set.");

        this.systemContext = targetContext;
    }

    // System Registry \\

    protected final <T extends InstancePackage> T create(Class<T> instanceClass) {
        return owner.createInstance(instanceClass);
    }

    // System Retrieval \\

    protected final <T> T get(Class<T> instanceClass) {

        if (this.systemContext != SystemContext.GET)
            throwException(
                    "Get called outside GET phase.\n" +
                            "Requested: " + instanceClass.getSimpleName() + "\n" +
                            "Current process: " + getContext());

        return internal.getUnchecked(owner.context, instanceClass);
    }

    // Create \\

    void internalCreate() {

        if (!this.verifyProcess(SystemContext.CREATE))
            return;

        this.create();
    }

    protected void create() {
    }

    // Get \\

    void internalGet() {

        if (!this.verifyProcess(SystemContext.GET))
            return;

        this.get();
    }

    protected void get() {
    }

    // Awake \\

    void internalAwake() {

        if (!this.verifyProcess(SystemContext.AWAKE))
            return;

        this.awake();
    }

    protected void awake() {
    }
}