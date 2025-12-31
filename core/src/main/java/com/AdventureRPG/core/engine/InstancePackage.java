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
    static final ThreadLocal<CreationData> CREATION_DATA = new ThreadLocal<>();

    protected final EnginePackage internal;
    protected final SystemPackage owner;

    InternalContext internalContext;

    // Internal //

    public InstancePackage() {

        // Internal
        CreationData creationData = CREATION_DATA.get();

        if (creationData == null)
            throwException("Instances must be created via internal engine `create` method");

        if (creationData.internal == null || creationData.owner == null)
            throwException("Cannot create instance without a proper internal engine or owner reference");

        this.internal = creationData.internal;
        this.owner = creationData.owner;

        this.internalContext = InternalContext.NULL;
    }

    // Init \\

    protected void internalCreate() {

        if (!this.verifyProcess(InternalContext.CREATE))
            return;

        create();

        // Set the internal process higher to avoid calling `get` illegally.
        requestContext(InternalContext.INIT);
    }

    protected void create() {
    }

    // Internal Context \\

    final InternalContext getContext() {
        return internalContext;
    }

    final boolean verifyProcess(InternalContext targetContext) {

        if (!targetContext.canEnterFrom(this.internalContext.order))
            return false;

        this.requestContext(targetContext);
        return true;
    }

    final void requestContext(InternalContext targetContext) {

        if (!targetContext.canEnterFrom(this.internalContext.order))
            throwException("Instance attempted to perform an illegal context set.");

        this.internalContext = targetContext;
    }

    // Utility \\

    static final class CreationData extends DataPackage {
        final EnginePackage internal;
        final SystemPackage owner;

        CreationData(EnginePackage internal, SystemPackage owner) {
            this.internal = internal;
            this.owner = owner;
        }
    }

    static void setupCreation(EnginePackage internal, SystemPackage owner) {
        CREATION_DATA.set(new CreationData(internal, owner));
    }

    // Accessible \\

    @SuppressWarnings("unchecked")
    protected final <T> T get(Class<T> type) {

        if (this.internalContext != InternalContext.CREATE)
            throwException(
                    "Get called outside CREATE phase.\n" +
                            "Requested: " + type.getSimpleName() + "\n" +
                            "Current process: " + getContext());

        return internal.get(true, type);
    }

    protected final <T extends InstancePackage> T create(Class<T> instanceClass) {
        return internal.internalCreate(instanceClass);
    }

}