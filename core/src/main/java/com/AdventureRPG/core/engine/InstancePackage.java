package com.AdventureRPG.core.engine;

public abstract class InstancePackage extends EngineUtility {

    /*
     * InstancePackages are lightweight, engine-managed objects. They are
     * intended as small, general-purpose data containers and helpers used
     * by SystemPackages and their extensions, without owning lifecycle or
     * global state.
     *
     * By design, InstancePackages may only be instantiated via the engine
     * `create` method, either here or in SystemPackage. Any attempt to
     * construct an InstancePackage outside of this mechanism will result
     * in an immediate exception.
     */

    // Internal
    static final ThreadLocal<CreationStruct> CREATION_STRUCT = new ThreadLocal<>();

    protected final EnginePackage internal;
    protected final SystemPackage owner;

    SystemContext systemContext;

    // Internal //

    public InstancePackage() {

        // Internal
        CreationStruct creationData = CREATION_STRUCT.get();

        if (creationData == null)
            throwException("Instances must be created via internal engine `create` method");

        if (creationData.internal == null || creationData.owner == null)
            throwException("Cannot create instance without a proper internal engine or owner reference");

        this.internal = creationData.internal;
        this.owner = creationData.owner;

        this.systemContext = SystemContext.CREATE;
    }

    static final class CreationStruct extends StructPackage {

        /*
         * A container used to ensure proper instance creation at any point in the
         * internal engines lifecycle. Mainly serves as a temporary data transfer
         * mechanism.
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
        return internal.createInstance(instanceClass);
    }

    // System Retrieval \\

    @SuppressWarnings("unchecked")
    protected final <T> T get(Class<T> type) {

        if (this.systemContext != SystemContext.GET)
            throwException(
                    "Get called outside GET phase.\n" +
                            "Requested: " + type.getSimpleName() + "\n" +
                            "Current process: " + getContext());

        return internal.get(true, type);
    }

    // Get \\

    protected void internalGet() {

        if (!this.verifyProcess(SystemContext.GET))
            return;

        get();

        // Set the internal process higher to avoid calling `get` illegally.
        requestContext(SystemContext.AWAKE);
    }

    protected void get() {
    }
}