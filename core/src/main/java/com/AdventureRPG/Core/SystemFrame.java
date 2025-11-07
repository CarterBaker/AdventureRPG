package com.AdventureRPG.Core;

import com.AdventureRPG.Core.Exceptions.GeneralException;
import com.AdventureRPG.SettingsSystem.Settings;

public abstract class SystemFrame extends MainFrame {

    // Internal
    private InternalProcess internalProcess;
    private InternalState internalState;
    protected Settings settings;
    protected RootManager rootManager;
    protected ManagerFrame localManager;

    protected final void registerLocalManager(ManagerFrame localManager) {
        this.localManager = localManager;
    }

    protected final InternalProcess getInternalProcess() {
        return internalProcess;
    }

    protected final InternalState getInternalState() {
        return internalState;
    }

    protected final void setInternalState(InternalState internalState) {

        if (internalState != InternalState.MENU_EXCLUSIVE || internalState != InternalState.GAME_EXCLIVE)
            throw new GeneralException.GameStateException(internalState);

        this.internalState = internalState;
    }

    // Create \\

    void internalCreate(Settings settings, RootManager rootManager) {

        internalProcess = InternalProcess.CREATE;
        internalState = InternalState.CONSTRUCTOR;

        // Root
        this.settings = settings;
        this.rootManager = rootManager;

        create();
    }

    protected void create() {
    }

    // Init \\

    void internalInit() {

        internalProcess = InternalProcess.INIT;
        init();
    }

    // Init \\

    protected void init() {
    }

    // Awake

    void internalAwake() {

        internalProcess = InternalProcess.AWAKE;
        awake();
    }

    protected void awake() {
    }

    // Start \\

    void internalStart() {

        internalProcess = InternalProcess.START;
        start();
    }

    protected void start() {
    }

    // Menu Exclusive Update \\

    void internalMenuExclusiveUpdate() {

        internalProcess = InternalProcess.MENU_EXCLUSIVE;
        internalState = InternalState.FIRST_FRAME;

        menuExclusiveUpdate();
    }

    protected void menuExclusiveUpdate() {
    }

    // Game Exclusive Update \\

    void internalGameExclusiveUpdate() {

        internalProcess = InternalProcess.GAME_EXCLUSIVE;
        gameExclusiveUpdate();
    }

    protected void gameExclusiveUpdate() {
    }

    // Update \\

    void internalUpdate() {

        internalProcess = InternalProcess.UPDATE;
        update();
    }

    protected void update() {
    }

    // Fixed Update \\

    void internalFixedUpdate() {

        internalProcess = InternalProcess.FIXED_UPDATE;
        fixedUpdate();
    }

    protected void fixedUpdate() {
    }

    // Late Update \\

    void internalLateUpdate() {

        internalProcess = InternalProcess.LATE_UPDATE;
        lateUpdate();
    }

    protected void lateUpdate() {
    }

    // Render \\

    void internalRender() {

        internalProcess = InternalProcess.RENDER;
        render();
    }

    protected void render() {
    }

    // Dispose \\

    void internalDispose() {

        internalProcess = InternalProcess.DISPOSE;
        internalState = InternalState.EXIT;

        dispose();
    }

    protected void dispose() {
    }
}
