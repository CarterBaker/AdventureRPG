package com.AdventureRPG.core.kernel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.AdventureRPG.core.settings.Settings;
import com.AdventureRPG.core.util.Exceptions.CoreException;
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
    private SystemFrame[] kernelArray = new SystemFrame[0];

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
            throw new CoreException.GameStateException(
                    "Cannot switch to the target state: " + target.toString() + ", From the requested location");

        this.setInternalState(target);
    }

    // Kernel Registry \\

    @Override
    SystemFrame internalRegister(SystemFrame subSystem) {

        if (this.getInternalProcess() != InternalProcess.BOOT_KERNEL &&
                this.getInternalProcess() != InternalProcess.CREATE)
            throw new CoreException.OutOfOrderException(
                    "Game engine error, a process was attempted to be called out of order");

        if (this.getInternalProcess() == InternalProcess.CREATE)
            return super.internalRegister(subSystem);

        if (this.kernelTree.contains(subSystem))
            throw new CoreException.DuplicateSystemFrameDetected("Subsystem: " + subSystem.getClass().getSimpleName()
                    + ", Already exists within the engine frame. Only one instance of any given system can exist at a time");

        this.kernelTree.add(subSystem);

        registerToInternalManager(subSystem);

        return subSystem;
    }

    private final void registerToInternalManager(SystemFrame subSystem) {

        this.setInternalProcess(InternalProcess.CREATE);

        super.internalRegister(subSystem);

        this.setInternalProcess(InternalProcess.BOOT_KERNEL);
    }

    // Kernel \\

    void internalBootKernel() {

        this.beginBoot();

        this.kernelCreate();
        this.kernelInit();
        this.kernelAwake();
        this.kernelFreeMemory();

        this.finalizeBoot();
    }

    // Begin
    private final void beginBoot() {

        this.setInternalProcess(InternalProcess.BOOT_KERNEL);

        this.bootKernel();
        this.cacheSubSystems();
    }

    protected void bootKernel() {
    }

    private final void cacheSubSystems() {

        this.kernelArray = this.kernelTree.toArray(new SystemFrame[0]);
        this.kernelTree.clear();
    }

    // Create
    private final void kernelCreate() {

        this.setInternalProcess(InternalProcess.CREATE);

        for (int i = 0; i < this.kernelArray.length; i++)
            this.kernelArray[i].internalCreate();
    }

    // Init
    private final void kernelInit() {

        this.setInternalProcess(InternalProcess.INIT);

        for (int i = 0; i < this.kernelArray.length; i++)
            this.kernelArray[i].internalInit();
    }

    // Awake
    private final void kernelAwake() {

        this.setInternalProcess(InternalProcess.AWAKE);

        for (int i = 0; i < this.kernelArray.length; i++)
            this.kernelArray[i].internalAwake();
    }

    // Awake
    private final void kernelFreeMemory() {

        this.setInternalProcess(InternalProcess.FREE_MEMORY);

        for (int i = 0; i < this.kernelArray.length; i++)
            this.kernelArray[i].internalFreeMemory();
    }

    // Finalize
    private final void finalizeBoot() {

        this.kernelTree.clear();
        this.kernelArray = new SystemFrame[0];

        this.setInternalProcess(InternalProcess.CREATE);
    }

    // Create \\

    @Override
    void internalCreate() {

        this.setInternalProcess(InternalProcess.CREATE);

        super.internalCreate();
    }

    // Init \\

    @Override
    void internalInit() {

        this.setInternalProcess(InternalProcess.INIT);

        super.internalInit();
    }

    // Awake \\

    @Override
    void internalAwake() {

        this.setInternalProcess(InternalProcess.AWAKE);

        super.internalAwake();
    }

    // Free Memory \\

    @Override
    void internalFreeMemory() {

        this.setInternalProcess(InternalProcess.FREE_MEMORY);

        super.internalFreeMemory();
    }

    // Start \\

    @Override
    void internalStart() {

        this.setInternalProcess(InternalProcess.START);

        super.internalStart();
    }

    // Menu Exclusive Update \\

    @Override
    void internalMenuExclusiveUpdate() {

        this.setInternalProcess(InternalProcess.MENU_EXCLUSIVE);

        super.internalMenuExclusiveUpdate();
    }

    // Game Exclusive Update \\

    @Override
    void internalGameExclusiveUpdate() {

        this.setInternalProcess(InternalProcess.GAME_EXCLUSIVE);

        super.internalGameExclusiveUpdate();
    }

    // Update \\

    @Override
    void internalUpdate() {

        this.setInternalProcess(InternalProcess.UPDATE);

        super.internalUpdate();
    }

    // Fixed Update \\

    @Override
    void internalFixedUpdate() {

        this.setInternalProcess(InternalProcess.FIXED_UPDATE);

        super.internalFixedUpdate();
    }

    // Late Update \\

    @Override
    void internalLateUpdate() {

        this.setInternalProcess(InternalProcess.LATE_UPDATE);

        super.internalLateUpdate();
    }

    // Render \\

    @Override
    void internalRender() {

        this.setInternalProcess(InternalProcess.RENDER);

        super.internalRender();
    }

    // Draw \\

    void internalDraw() {

        this.setInternalProcess(InternalProcess.DRAW);

        draw();
    }

    protected void draw() {
    }

    // Dispose \\

    @Override
    void internalDispose() {

        this.setInternalProcess(InternalProcess.DISPOSE);

        super.internalDispose();
    }

    // Accessible \\

    public float getDeltaTime() {
        return Gdx.graphics.getDeltaTime();
    }
}
