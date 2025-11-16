package com.AdventureRPG.Core.Bootstrap;

import com.AdventureRPG.Core.Util.Exceptions.CoreException;
import com.AdventureRPG.SettingsSystem.Settings;

public abstract class SystemFrame extends MainFrame {

    // Internal
    InternalProcess internalProcess = InternalProcess.CREATE;
    InternalState internalState = InternalState.CONSTRUCTOR;
    protected Settings settings;
    protected EngineFrame engineManager;
    protected ManagerFrame localManager;

    // Root \\

    protected final InstanceFrame create(InstanceFrame instanceFrame) {

        instanceFrame.create(
                engineManager,
                this);

        return instanceFrame;
    }

    protected final void registerLocalManager(ManagerFrame localManager) {
        this.localManager = localManager;
    }

    // Internal Process \\

    protected final InternalProcess getInternalProcess() {

        return engineManager.getInternalRootProcess();
    }

    final void setInternalProcess(InternalProcess internalProcess) {
        this.internalProcess = internalProcess;
        engineManager.setInternalRootProcess(internalProcess);
    }

    final boolean verifyProcess(InternalProcess target) {

        InternalProcess rootProcess = getInternalProcess();

        if (target.isUpdateProcess()) {

            setInternalProcess(target);
            return true;
        }

        if (target.order < rootProcess.order || target.order < internalProcess.order)
            return false;

        setInternalProcess(target);
        return true;
    }

    // Internal State \\

    protected final InternalState getInternalState() {
        return engineManager.getInternalRootState();
    }

    protected final void requestInternalState(InternalState internalState) {

        if (!internalState.accessible)
            throw new CoreException.GameStateException(internalState);

        engineManager.setInternalRootState(internalState);
    }

    // Create \\

    void internalCreate(Settings settings, EngineFrame engineManager) {

        // Root
        this.settings = settings;
        this.engineManager = engineManager;

        if (!verifyProcess(InternalProcess.CREATE))
            return;

        create();
    }

    protected void create() {
    }

    // Init \\

    void internalInit() {

        if (!verifyProcess(InternalProcess.INIT))
            return;

        init();
    }

    protected void init() {
    }

    // Awake \\

    void internalAwake() {

        if (!verifyProcess(InternalProcess.AWAKE))
            return;

        awake();

        internalState = InternalState.FIRST_FRAME;
    }

    protected void awake() {
    }

    // Start \\

    void internalStart() {

        if (!verifyProcess(InternalProcess.START))
            return;

        start();
    }

    protected void start() {
    }

    // Menu Exclusive Update \\

    void internalMenuExclusiveUpdate() {

        if (!verifyProcess(InternalProcess.MENU_EXCLUSIVE))
            return;

        menuExclusiveUpdate();
    }

    protected void menuExclusiveUpdate() {
    }

    // Game Exclusive Update \\

    void internalGameExclusiveUpdate() {

        if (!verifyProcess(InternalProcess.GAME_EXCLUSIVE))
            return;

        gameExclusiveUpdate();
    }

    protected void gameExclusiveUpdate() {
    }

    // Update \\

    void internalUpdate() {

        if (!verifyProcess(InternalProcess.UPDATE))
            return;

        update();
    }

    protected void update() {
    }

    // Fixed Update \\

    void internalFixedUpdate() {

        if (!verifyProcess(InternalProcess.FIXED_UPDATE))
            return;

        fixedUpdate();
    }

    protected void fixedUpdate() {
    }

    // Late Update \\

    void internalLateUpdate() {

        if (!verifyProcess(InternalProcess.LATE_UPDATE))
            return;

        lateUpdate();
    }

    protected void lateUpdate() {
    }

    // Render \\

    void internalRender() {

        if (!verifyProcess(InternalProcess.RENDER))
            return;

        render();
    }

    protected void render() {
    }

    // Dispose \\

    void internalDispose() {

        if (!verifyProcess(InternalProcess.DISPOSE))
            return;

        internalState = InternalState.EXIT;

        dispose();
    }

    protected void dispose() {
    }

    // Debug \\

    protected final void debugProcess() {
        debugProcess("");
    }

    protected final void debugProcess(String input) {
        debug("[" + internalProcess.toString() + "] " + input);
    }
}
