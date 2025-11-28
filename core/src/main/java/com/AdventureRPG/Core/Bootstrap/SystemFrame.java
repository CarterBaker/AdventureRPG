package com.AdventureRPG.Core.Bootstrap;

import com.AdventureRPG.Core.Settings.Settings;

public abstract class SystemFrame extends MainFrame {

    // Internal
    InternalProcess internalProcess = InternalProcess.CREATE;
    protected Settings settings;
    protected EngineFrame gameEngine;
    protected ManagerFrame localManager;

    // Root \\

    final void register(
            Settings settings,
            EngineFrame gameEngine,
            ManagerFrame localManager) {

        this.settings = settings;
        this.gameEngine = gameEngine;
        this.localManager = localManager;
    }

    // Internal Process \\

    final InternalProcess getInternalProcess() {
        return internalProcess;
    }

    final void setInternalProcess(InternalProcess target) {
        this.internalProcess = target;
    }

    final boolean verifyProcess(InternalProcess target) {

        InternalProcess rootProcess = gameEngine.getInternalProcess();

        if (!target.isUpdateProcess() &&
                (target.getOrder() < rootProcess.getOrder() || target.getOrder() < this.internalProcess.getOrder()))
            return false;

        this.setInternalProcess(target);
        return true;
    }

    // Create \\

    void internalCreate() {

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

    // Accessible \\

    protected final InstanceFrame create(InstanceFrame instanceFrame) {

        instanceFrame.create(
                gameEngine,
                this);

        return instanceFrame;
    }

    // Debug \\

    protected final void debugProcess() {
        debugProcess("");
    }

    protected final void debugProcess(Object input) {
        debug("[" + internalProcess.toString() + "] " + String.valueOf(input));
    }
}
