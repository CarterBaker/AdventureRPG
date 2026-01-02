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

        this.systemContext = SystemContext.NULL;
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

    // Init \\

    protected void internalCreate() {

        if (!this.verifyProcess(SystemContext.CREATE))
            return;

        create();

        // Set the internal process higher to avoid calling `get` illegally.
        requestContext(SystemContext.GET);
    }

    protected void create() {
    }

    // Internal Context \\

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

    // Accessible \\

    @SuppressWarnings("unchecked")
    protected final <T> T get(Class<T> type) {

        if (this.systemContext != SystemContext.CREATE)
            throwException(
                    "Get called outside GET phase.\n" +
                            "Requested: " + type.getSimpleName() + "\n" +
                            "Current process: " + getContext());

        return internal.get(true, type);
    }

    protected final <T extends InstancePackage> T create(Class<T> instanceClass) {
        return internal.createInstance(instanceClass);
    }

}