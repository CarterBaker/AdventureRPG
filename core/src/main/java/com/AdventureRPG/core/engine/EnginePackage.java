package com.AdventureRPG.core.engine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.AdventureRPG.core.engine.settings.Settings;
import com.google.gson.Gson;

public class EnginePackage extends ManagerPackage {

    // Root
    public Main main;
    public File path;
    public Gson gson;

    // Internal
    InternalState internalState = InternalState.CONSTRUCTOR;

    private List<SystemPackage> kernelTree = new ArrayList<>();
    private SystemPackage[] kernelArray = new SystemPackage[0];

    private long frameCount;
    private float delta;

    // Base \\

    public EnginePackage(
            Settings setting,
            Main main,
            File path,
            Gson gson) {

        // Root
        this.settings = setting;
        this.internal = this;
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
            throwException(
                    "Cannot switch to the target state: " + target.toString() + ", From the requested location");

        this.setInternalState(target);
    }

    // Kernel Registry \\

    @Override
    SystemPackage internalRegister(SystemPackage subSystem) {

        if (this.getInternalProcess() != InternalProcess.BOOT_KERNEL &&
                this.getInternalProcess() != InternalProcess.CREATE)
            throwException(
                    "Game engine error, a process was attempted to be called out of order");

        if (this.getInternalProcess() == InternalProcess.CREATE)
            return super.internalRegister(subSystem);

        if (this.kernelTree.contains(subSystem))
            throwException("Subsystem: " + subSystem.getClass().getSimpleName()
                    + ", Already exists within the engine frame. Only one instance of any given system can exist at a time");

        this.kernelTree.add(subSystem);

        this.registerToInternalManager(subSystem);

        return subSystem;
    }

    private final void registerToInternalManager(SystemPackage subSystem) {

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

        this.kernelArray = this.kernelTree.toArray(new SystemPackage[0]);
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

    // Free Memory
    private final void kernelFreeMemory() {

        this.setInternalProcess(InternalProcess.FREE_MEMORY);

        for (int i = 0; i < this.kernelArray.length; i++)
            this.kernelArray[i].internalFreeMemory();
    }

    // Finalize
    private final void finalizeBoot() {

        this.kernelTree.clear();
        this.kernelArray = new SystemPackage[0];

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

        this.setFrameCount();

        super.internalRender();
    }

    // Draw \\

    void internalDraw() {

        this.setInternalProcess(InternalProcess.DRAW);

        draw();
    }

    protected void draw() {
        // Engine specific logic
    }

    // Dispose \\

    @Override
    void internalDispose() {

        this.setInternalProcess(InternalProcess.DISPOSE);

        super.internalDispose();
    }

    // Utility \\

    final void setFrameCount() {
        frameCount++;
    }

    final void setDeltaTime(float delta) {
        this.delta = delta;
    }

    // Accessible \\

    public long getFrameCount() {
        return frameCount;
    }

    public float getDeltaTime() {
        return delta;
    }
}
