package com.AdventureRPG.core.engine;

import com.AdventureRPG.core.engine.settings.Settings;

public abstract class SystemPackage extends EngineUtility {

    // Core
    protected Settings settings = null;
    protected EnginePackage internal = null;
    protected ManagerPackage local = null;

    // Internal
    InternalContext internalContext = null;

    // Core \\

    final void register(
            Settings settings,
            EnginePackage internal,
            ManagerPackage local) {

        // Core
        this.settings = settings;
        this.internal = internal;
        this.local = local;

        // Internal
        this.internalContext = InternalContext.NULL;
    }

    // Internal Context \\

    final InternalContext getContext() {
        return internalContext;
    }

    void setContext(InternalContext targetContext) {

        if (!targetContext.canEnterFrom(this.internalContext.order))
            throwException("Firing order issue. Internal engine attempted to perform an illegal context set.");

        this.internalContext = targetContext;
    }

    boolean verifyProcess(InternalContext targetContext) {

        if (internal.internalContext != targetContext
                || !targetContext.canEnterFrom(this.internalContext.order))
            return false;

        if (internal.internalState != InternalState.CONSTRUCTOR)
            debug("This works too");

        this.setContext(targetContext);
        return true;
    }

    // Create \\

    void internalCreate() {

        verifySystem();

        if (!this.verifyProcess(InternalContext.CREATE))
            return;

        create();
    }

    protected void create() {
    }

    // Init \\

    void internalInit() {

        if (!this.verifyProcess(InternalContext.INIT))
            return;

        init();
    }

    protected void init() {
    }

    // Awake \\

    void internalAwake() {

        if (!this.verifyProcess(InternalContext.AWAKE))
            return;

        awake();
    }

    protected void awake() {
    }

    // FreeMemory \\

    void internalFreeMemory() {

        if (!this.verifyProcess(InternalContext.FREE_MEMORY))
            return;

        freeMemory();
    }

    protected void freeMemory() {
    }

    // Start \\

    void internalStart() {

        if (!this.verifyProcess(InternalContext.START))
            return;

        start();
    }

    protected void start() {
    }

    // Update \\

    void internalUpdate() {

        if (!this.verifyProcess(InternalContext.UPDATE))
            return;

        update();
    }

    protected void update() {
    }

    // Menu Exclusive Update \\

    void internalMenuExclusiveUpdate() {

        if (!this.verifyProcess(InternalContext.MENU_EXCLUSIVE))
            return;

        menuExclusiveUpdate();
    }

    protected void menuExclusiveUpdate() {
    }

    // Game Exclusive Update \\

    void internalGameExclusiveUpdate() {

        if (!this.verifyProcess(InternalContext.GAME_EXCLUSIVE))
            return;

        gameExclusiveUpdate();
    }

    protected void gameExclusiveUpdate() {
    }

    // Fixed Update \\

    void internalFixedUpdate() {

        if (!this.verifyProcess(InternalContext.FIXED_UPDATE))
            return;

        fixedUpdate();
    }

    protected void fixedUpdate() {
    }

    // Late Update \\

    void internalLateUpdate() {

        if (!this.verifyProcess(InternalContext.LATE_UPDATE))
            return;

        lateUpdate();
    }

    protected void lateUpdate() {
    }

    // Render \\

    void internalRender() {

        if (!this.verifyProcess(InternalContext.RENDER))
            return;

        render();
    }

    protected void render() {
    }

    // Dispose \\

    void internalDispose() {

        if (!this.verifyProcess(InternalContext.DISPOSE))
            return;

        dispose();
    }

    protected void dispose() {
    }

    // Utility \\

    private void verifySystem() {
        if (settings == null ||
                internal == null ||
                local == null ||
                internalContext == null)
            throwException("System was created without proper registration");
    }

    // Accessible \\

    protected final void debugProcess() {
        debugProcess("");
    }

    protected final void debugProcess(Object input) {
        debug("[" + internalContext.toString() + "] " + String.valueOf(input));
    }

    protected final <T extends InstancePackage> T create(Class<T> instanceClass) {
        return internalCreate(instanceClass);
    }

    final <T extends InstancePackage> T internalCreate(Class<T> instanceClass) {

        InstancePackage.setupCreation(internal, this);

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
        return internal.get(false, type);
    }
}
