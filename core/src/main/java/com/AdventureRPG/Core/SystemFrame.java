package com.AdventureRPG.Core;

import com.AdventureRPG.Core.Exceptions.CoreException;
import com.AdventureRPG.SettingsSystem.Settings;

public abstract class SystemFrame extends MainFrame {

    // Internal
    private InternalProcess internalProcess = InternalProcess.CREATE;
    private InternalState internalState = InternalState.CONSTRUCTOR;
    protected Settings settings;
    protected RootManager rootManager;
    protected ManagerFrame localManager;

    // Root \\

    protected final void registerLocalManager(ManagerFrame localManager) {
        this.localManager = localManager;
    }

    // Internal Process \\

    // Get
    protected final InternalProcess getInternalProcess() {
        return getInternalRootProcess();
    }

    final InternalProcess getInternalRootProcess() {
        return rootManager.internalProcess();
    }

    final InternalProcess internalProcess() {
        return internalProcess;
    }

    // Set
    final void setInternalRootProcess(InternalProcess internalProcess) {
        rootManager.setInternalProcess(internalProcess);
    }

    final void setInternalProcess(InternalProcess internalProcess) {
        this.internalProcess = internalProcess;
    }

    // Internal State \\

    // Get
    protected final InternalState getInternalState() {
        return rootManager.getInternalRootState();
    }

    final InternalState getInternalRootState() {
        return internalState;
    }

    // Set
    protected final void requestInternalState(InternalState internalState) {

        if (!internalState.accessible)
            throw new CoreException.GameStateException(internalState);

        rootManager.setInternalRootState(internalState);
    }

    final void setInternalRootState(InternalState internalState) {
        rootManager.setInternalState(internalState);
    }

    final void setInternalState(InternalState internalState) {
        this.internalState = internalState;
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

        setInternalRootProcess(InternalProcess.INIT);

        init();
    }

    protected void init() {
    }

    // Awake \\

    void internalAwake() {

        setInternalRootProcess(InternalProcess.AWAKE);

        awake();

        internalState = InternalState.FIRST_FRAME;
    }

    protected void awake() {
    }

    // Start \\

    void internalStart() {

        setInternalRootProcess(InternalProcess.START);

        start();
    }

    protected void start() {
    }

    // Menu Exclusive Update \\

    void internalMenuExclusiveUpdate() {

        setInternalRootProcess(InternalProcess.MENU_EXCLUSIVE);

        menuExclusiveUpdate();
    }

    protected void menuExclusiveUpdate() {
    }

    // Game Exclusive Update \\

    void internalGameExclusiveUpdate() {

        setInternalRootProcess(InternalProcess.GAME_EXCLUSIVE);

        gameExclusiveUpdate();
    }

    protected void gameExclusiveUpdate() {
    }

    // Update \\

    void internalUpdate() {

        setInternalRootProcess(InternalProcess.UPDATE);

        update();
    }

    protected void update() {
    }

    // Fixed Update \\

    void internalFixedUpdate() {

        setInternalRootProcess(InternalProcess.FIXED_UPDATE);

        fixedUpdate();
    }

    protected void fixedUpdate() {
    }

    // Late Update \\

    void internalLateUpdate() {

        setInternalRootProcess(InternalProcess.LATE_UPDATE);

        lateUpdate();
    }

    protected void lateUpdate() {
    }

    // Render \\

    void internalRender() {

        setInternalRootProcess(InternalProcess.RENDER);

        render();
    }

    protected void render() {
    }

    // Dispose \\

    void internalDispose() {

        setInternalRootProcess(InternalProcess.DISPOSE);
        internalState = InternalState.EXIT;

        dispose();
    }

    protected void dispose() {
    }
}
