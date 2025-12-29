package com.AdventureRPG.core.engine;

import com.AdventureRPG.core.engine.settings.Settings;

public abstract class SystemPackage extends EngineUtility {

    // Core
    protected Settings settings = null;
    protected EnginePackage internal = null;
    protected ManagerPackage localManager = null;

    // Internal
    InternalProcess internalProcess = null;

    // Core \\

    final void register(
            Settings settings,
            EnginePackage internal,
            ManagerPackage localManager) {

        // Core
        this.settings = settings;
        this.internal = internal;
        this.localManager = localManager;

        // Internal
        this.internalProcess = InternalProcess.CREATE;
    }

    // Internal Process \\

    final InternalProcess getInternalProcess() {
        return internalProcess;
    }

    final void setInternalProcess(InternalProcess target) {
        this.internalProcess = target;
    }

    final boolean verifyProcess(InternalProcess target) {

        InternalProcess rootProcess = internal.getInternalProcess();

        if (!target.isUpdateProcess() &&
                (target.getOrder() < rootProcess.getOrder() || target.getOrder() < this.internalProcess.getOrder()))
            return false;

        this.setInternalProcess(target);
        return true;
    }

    // Create \\

    void internalCreate() {

        verifySystem();

        if (!this.verifyProcess(InternalProcess.CREATE))
            return;

        create();
    }

    protected void create() {
    }

    // Init \\

    void internalInit() {

        if (!this.verifyProcess(InternalProcess.INIT))
            return;

        init();
    }

    protected void init() {
    }

    // Awake \\

    void internalAwake() {

        if (!this.verifyProcess(InternalProcess.AWAKE))
            return;

        awake();
    }

    protected void awake() {
    }

    // FreeMemory \\

    void internalFreeMemory() {

        if (!this.verifyProcess(InternalProcess.FREE_MEMORY))
            return;

        freeMemory();
    }

    protected void freeMemory() {
    }

    // Start \\

    void internalStart() {

        if (!this.verifyProcess(InternalProcess.START))
            return;

        start();
    }

    protected void start() {
    }

    // Menu Exclusive Update \\

    void internalMenuExclusiveUpdate() {

        if (!this.verifyProcess(InternalProcess.MENU_EXCLUSIVE))
            return;

        menuExclusiveUpdate();
    }

    protected void menuExclusiveUpdate() {
    }

    // Game Exclusive Update \\

    void internalGameExclusiveUpdate() {

        if (!this.verifyProcess(InternalProcess.GAME_EXCLUSIVE))
            return;

        gameExclusiveUpdate();
    }

    protected void gameExclusiveUpdate() {
    }

    // Update \\

    void internalUpdate() {

        if (!this.verifyProcess(InternalProcess.UPDATE))
            return;

        update();
    }

    protected void update() {
    }

    // Fixed Update \\

    void internalFixedUpdate() {

        if (!this.verifyProcess(InternalProcess.FIXED_UPDATE))
            return;

        fixedUpdate();
    }

    protected void fixedUpdate() {
    }

    // Late Update \\

    void internalLateUpdate() {

        if (!this.verifyProcess(InternalProcess.LATE_UPDATE))
            return;

        lateUpdate();
    }

    protected void lateUpdate() {
    }

    // Render \\

    void internalRender() {

        if (!this.verifyProcess(InternalProcess.RENDER))
            return;

        render();
    }

    protected void render() {
    }

    // Dispose \\

    void internalDispose() {

        if (!this.verifyProcess(InternalProcess.DISPOSE))
            return;

        dispose();
    }

    protected void dispose() {
    }

    // Utility \\

    private void verifySystem() {
        if (settings == null ||
                internal == null ||
                localManager == null ||
                internalProcess == null)
            throwException("System was created without proper registration");
    }

    // Accessible \\

    protected final void debugProcess() {
        debugProcess("");
    }

    protected final void debugProcess(Object input) {
        debug("[" + internalProcess.toString() + "] " + String.valueOf(input));
    }

    protected final InstanceFrame create(InstanceFrame instanceFrame) {

        instanceFrame.create(
                internal,
                this);

        return instanceFrame;
    }
}
