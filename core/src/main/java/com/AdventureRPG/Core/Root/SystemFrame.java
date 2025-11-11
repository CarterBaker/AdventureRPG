package com.AdventureRPG.Core.Root;

import com.AdventureRPG.Core.Exceptions.CoreException;
import com.AdventureRPG.SettingsSystem.Settings;

public abstract class SystemFrame extends MainFrame {

    // Internal
    InternalProcess internalProcess = InternalProcess.CREATE;
    InternalState internalState = InternalState.CONSTRUCTOR;
    protected Settings settings;
    protected RootManager rootManager;
    protected ManagerFrame localManager;

    // Root \\

    protected final void registerLocalManager(ManagerFrame localManager) {
        this.localManager = localManager;
    }

    // Internal Process \\

    protected final InternalProcess getInternalProcess() {
        return rootManager.getInternalRootProcess();
    }

    final void setInternalProcess(InternalProcess internalProcess) {
        rootManager.setInternalRootProcess(internalProcess);
    }

    // Internal State \\

    protected final InternalState getInternalState() {
        return rootManager.getInternalRootState();
    }

    protected final void requestInternalState(InternalState internalState) {

        if (!internalState.accessible)
            throw new CoreException.GameStateException(internalState);

        rootManager.setInternalRootState(internalState);
    }

    // Create \\

    void internalCreate(Settings settings, RootManager rootManager) {

        // Root
        this.settings = settings;
        this.rootManager = rootManager;

        create();
    }

    protected void create() {
    }

    // Init \\

    void internalInit() {

        setInternalProcess(InternalProcess.INIT);

        init();
    }

    protected void init() {
    }

    // Awake \\

    void internalAwake() {

        setInternalProcess(InternalProcess.AWAKE);

        awake();

        internalState = InternalState.FIRST_FRAME;
    }

    protected void awake() {
    }

    // Start \\

    void internalStart() {

        setInternalProcess(InternalProcess.START);

        start();
    }

    protected void start() {
    }

    // Menu Exclusive Update \\

    void internalMenuExclusiveUpdate() {

        setInternalProcess(InternalProcess.MENU_EXCLUSIVE);

        menuExclusiveUpdate();
    }

    protected void menuExclusiveUpdate() {
    }

    // Game Exclusive Update \\

    void internalGameExclusiveUpdate() {

        setInternalProcess(InternalProcess.GAME_EXCLUSIVE);

        gameExclusiveUpdate();
    }

    protected void gameExclusiveUpdate() {
    }

    // Update \\

    void internalUpdate() {

        setInternalProcess(InternalProcess.UPDATE);

        update();
    }

    protected void update() {
    }

    // Fixed Update \\

    void internalFixedUpdate() {

        setInternalProcess(InternalProcess.FIXED_UPDATE);

        fixedUpdate();
    }

    protected void fixedUpdate() {
    }

    // Late Update \\

    void internalLateUpdate() {

        setInternalProcess(InternalProcess.LATE_UPDATE);

        lateUpdate();
    }

    protected void lateUpdate() {
    }

    // Render \\

    void internalRender() {

        setInternalProcess(InternalProcess.RENDER);

        render();
    }

    protected void render() {
    }

    // Dispose \\

    void internalDispose() {

        setInternalProcess(InternalProcess.DISPOSE);
        internalState = InternalState.EXIT;

        dispose();
    }

    protected void dispose() {
    }
}
