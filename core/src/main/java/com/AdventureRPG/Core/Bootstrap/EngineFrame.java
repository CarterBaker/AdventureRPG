package com.AdventureRPG.Core.Bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.AdventureRPG.Core.Util.Exceptions.CoreException;
import com.AdventureRPG.Core.Util.Exceptions.CoreException.DuplicateSystemFrameDetected;
import com.AdventureRPG.SettingsSystem.Settings;
import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;

public class EngineFrame extends ManagerFrame {

    // Root
    public Main main;
    public File path;
    public Gson gson;

    // Internal
    InternalState internalState = InternalState.CONSTRUCTOR;

    private List<SystemFrame> kernelTree = new ArrayList<>();
    private List<SystemFrame> kernelSystems = new ArrayList<>();
    private SystemFrame[] engineSystems = new SystemFrame[0];

    // Base \\

    public EngineFrame(
            Settings setting,
            Main main,
            File path,
            Gson gson) {

        // Root
        this.settings = setting;
        this.gameEngine = this;
        this.localManager = this;
        this.main = main;
        this.path = path;
        this.gson = gson;
    }

    // Internal State \\

    final InternalState getInternalState() {
        return internalState;
    }

    final void setInternalState(InternalState target) {
        this.internalState = target;
    }

    public final void requestInternalState(InternalState target) {

        if (!target.accessible)
            throw new CoreException.GameStateException(target);

        this.setInternalState(target);
    }

    // Boot Kernel \\

    @Override
    SystemFrame internalRegister(SystemFrame subSystem) {

        if (this.getInternalProcess() != InternalProcess.BOOT_KERNEL &&
                this.getInternalProcess() != InternalProcess.CREATE)
            throw new CoreException.OutOfOrderException(this.getInternalProcess());

        if (this.getInternalProcess() == InternalProcess.CREATE)
            return super.internalRegister(subSystem);

        if (this.kernelTree.contains(subSystem))
            throw new DuplicateSystemFrameDetected(subSystem);

        this.kernelTree.add(subSystem);
        this.kernelSystems.add(subSystem);

        subSystem.registerCoreSystems(
                settings,
                gameEngine,
                this);

        return subSystem;
    }

    void internalBootKernel() {

        internalProcess = InternalProcess.BOOT_KERNEL;

        this.bootKernel();
        this.cacheSubSystems();

        this.kernelCreate();
        this.kernelInit();
        this.kernelAwake();

        internalProcess = InternalProcess.CREATE;
    }

    protected void bootKernel() {
    }

    private final void cacheSubSystems() {

        this.engineSystems = this.kernelSystems.toArray(new SystemFrame[0]);
        this.kernelSystems.clear();
    }

    private final void kernelCreate() {

        internalProcess = InternalProcess.CREATE;

        for (int i = 0; i < this.engineSystems.length; i++) {

            this.register(engineSystems[i]);
            this.engineSystems[i].internalCreate();
        }
    }

    private final void kernelInit() {

        internalProcess = InternalProcess.INIT;

        for (int i = 0; i < this.engineSystems.length; i++)
            this.engineSystems[i].internalInit();
    }

    private final void kernelAwake() {

        internalProcess = InternalProcess.AWAKE;

        for (int i = 0; i < this.engineSystems.length; i++)
            this.engineSystems[i].internalAwake();
    }

    // Create \\

    @Override
    void internalCreate() {

        internalProcess = InternalProcess.CREATE;

        super.internalCreate();
    }

    // Init \\

    @Override
    void internalInit() {

        internalProcess = InternalProcess.INIT;

        super.internalInit();
    }

    // Awake \\

    @Override
    void internalAwake() {

        internalProcess = InternalProcess.AWAKE;

        super.internalAwake();
    }

    // Start \\

    @Override
    void internalStart() {

        internalProcess = InternalProcess.START;

        super.internalStart();
    }

    // Menu Exclusive Update \\

    @Override
    void internalMenuExclusiveUpdate() {

        internalProcess = InternalProcess.MENU_EXCLUSIVE;

        super.internalMenuExclusiveUpdate();
    }

    // Game Exclusive Update \\

    @Override
    void internalGameExclusiveUpdate() {

        internalProcess = InternalProcess.GAME_EXCLUSIVE;

        super.internalGameExclusiveUpdate();
    }

    // Update \\

    @Override
    void internalUpdate() {

        internalProcess = InternalProcess.UPDATE;

        super.internalUpdate();
    }

    // Fixed Update \\

    @Override
    void internalFixedUpdate() {

        internalProcess = InternalProcess.FIXED_UPDATE;

        super.internalFixedUpdate();
    }

    // Late Update \\

    @Override
    void internalLateUpdate() {

        internalProcess = InternalProcess.LATE_UPDATE;

        super.internalLateUpdate();
    }

    // Dispose \\

    @Override
    void internalDispose() {

        internalProcess = InternalProcess.DISPOSE;

        super.internalDispose();
    }

    // Render \\

    @Override
    void internalRender() {

        internalProcess = InternalProcess.RENDER;

        super.internalRender();
    }

    // Accessible \\

    public float getDeltaTime() {
        return Gdx.graphics.getDeltaTime();
    }
}
