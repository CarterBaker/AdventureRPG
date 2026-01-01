package com.AdventureRPG.core.engine;

import com.AdventureRPG.core.engine.settings.Settings;

public abstract class SystemPackage extends EngineUtility {

    /*
     * This is the base class for all systems within the engine.
     * SystemPackages follow a strict lifecycle with automatic phase
     * verification to ensure proper execution order. Each phase can
     * be overridden by child classes to implement custom behavior.
     *
     * Lifecycle order:
     * CREATE → INIT → AWAKE → FREE_MEMORY → START →
     * UPDATE → FIXED_UPDATE → LATE_UPDATE → RENDER → DISPOSE
     *
     * Systems must be registered through a ManagerPackage or
     * EnginePackage to function properly.
     */

    // Core
    protected Settings settings;
    protected EnginePackage internal;
    protected ManagerPackage local;

    // Internal
    InternalContext internalContext;

    // Internal \\

    public SystemPackage() {

        // Core
        this.settings = null;
        this.internal = null;
        this.local = null;

        // Internal
        this.internalContext = null;
    }

    // Core \\

    final SystemPackage register(
            Settings settings,
            EnginePackage internal,
            ManagerPackage local) {

        // Core
        this.settings = settings;
        this.internal = internal;
        this.local = local;

        // Internal
        this.internalContext = InternalContext.NULL;

        return this;
    }

    // Internal Context \\

    final InternalContext getContext() {
        return this.internalContext;
    }

    void setContext(InternalContext targetContext) {

        if (!targetContext.canEnterFrom(this.internalContext.order))
            throwException("Firing order issue. Internal engine attempted to perform an illegal context set.");

        this.internalContext = targetContext;
    }

    boolean verifyProcess(InternalContext targetContext) {

        if (this.internal.internalContext != targetContext
                || !targetContext.canEnterFrom(this.internalContext.order))
            return false;

        this.setContext(targetContext);
        return true;
    }

    // Create \\

    void internalCreate() {

        this.verifySystem();

        if (!this.verifyProcess(InternalContext.CREATE))
            return;

        this.create();
    }

    protected void create() {
    }

    // Init \\

    void internalInit() {

        if (!this.verifyProcess(InternalContext.INIT))
            return;

        this.init();
    }

    protected void init() {
    }

    // Awake \\

    void internalAwake() {

        if (!this.verifyProcess(InternalContext.AWAKE))
            return;

        this.awake();
    }

    protected void awake() {
    }

    // FreeMemory \\

    void internalFreeMemory() {

        if (!this.verifyProcess(InternalContext.FREE_MEMORY))
            return;

        this.freeMemory();
    }

    protected void freeMemory() {
    }

    // Start \\

    void internalStart() {

        if (!this.verifyProcess(InternalContext.START))
            return;

        this.start();
    }

    protected void start() {
    }

    // Update \\

    void internalUpdate() {

        if (!this.verifyProcess(InternalContext.UPDATE))
            return;

        this.update();
    }

    protected void update() {
    }

    // Fixed Update \\

    void internalFixedUpdate() {

        if (!this.verifyProcess(InternalContext.FIXED_UPDATE))
            return;

        this.fixedUpdate();
    }

    protected void fixedUpdate() {
    }

    // Late Update \\

    void internalLateUpdate() {

        if (!this.verifyProcess(InternalContext.LATE_UPDATE))
            return;

        this.lateUpdate();
    }

    protected void lateUpdate() {
    }

    // Render \\

    void internalRender() {

        if (!this.verifyProcess(InternalContext.RENDER))
            return;

        this.render();
    }

    protected void render() {
    }

    // Dispose \\

    void internalDispose() {

        if (!this.verifyProcess(InternalContext.DISPOSE))
            return;

        this.dispose();
    }

    protected void dispose() {
    }

    // Utility \\

    private final void verifySystem() {
        if (settings == null ||
                internal == null ||
                local == null ||
                internalContext == null)
            throwException("System was created without proper registration");
    }

    // Accessible \\

    protected final void debugProcess() {
        this.debugProcess("");
    }

    protected final void debugProcess(Object input) {
        this.debug("[" + this.internalContext.toString() + "] " + String.valueOf(input));
    }

    protected final <T extends InstancePackage> T create(Class<T> instanceClass) {
        return this.internalCreate(instanceClass);
    }

    final <T extends InstancePackage> T internalCreate(Class<T> instanceClass) {

        InstancePackage.setupCreation(this.internal, this);

        if (!InstancePackage.class.isAssignableFrom(instanceClass))
            throwException("Cannot create non-InstancePackage class: " + instanceClass.getName());

        try {

            T instance = instanceClass.getDeclaredConstructor().newInstance();
            instance.internalCreate();

            return instance;
        }

        catch (Exception e) {

            throwException("Failed to create instance: " + e.getMessage());

            return null;
        }

        finally {
            InstancePackage.CREATION_DATA.remove();
        }
    }

    @SuppressWarnings("unchecked")
    protected final <T> T get(Class<T> type) {
        return this.internal.get(false, type);
    }
}
