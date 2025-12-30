package com.AdventureRPG.core.engine;

import java.util.ArrayList;
import java.util.List;

public abstract class ManagerPackage extends SystemPackage {

    // Internal
    private List<SystemPackage> systemTree = new ArrayList<>();
    private SystemPackage[] systemArray = new SystemPackage[0];
    private List<SystemPackage> garbageCollection = new ArrayList<>();

    // System Registry \\

    protected final SystemPackage register(SystemPackage subSystem) {
        return internalRegister(subSystem);
    }

    SystemPackage internalRegister(SystemPackage subSystem) {

        if (this.getInternalProcess() != InternalProcess.CREATE)
            throwException(
                    "Register method was called from a process other than create. Current process: "
                            + this.getInternalProcess());

        if (subSystem instanceof EnginePackage)
            throwException("Only one engine package is allowed at any given time");

        if (this.systemTree.contains(subSystem))
            throwException("Subsystem: " + subSystem.getClass().getSimpleName()
                    + ", Already exists within the engine frame. Only one instance of any given system can exist at a time");

        this.systemTree.add(subSystem);

        setupNewSubSystem(subSystem);

        return subSystem;
    }

    void setupNewSubSystem(SystemPackage subSystem) {
        subSystem.register(
                settings,
                internal,
                this);
    }

    protected final SystemPackage release(SystemPackage subSystem) {
        internalRelease(subSystem);
        return null;
    }

    void internalRelease(SystemPackage subSystem) {

        if (this.getInternalProcess() != InternalProcess.FREE_MEMORY)
            throwException(
                    "Release method was called from a process other than free memory. Current process: "
                            + this.getInternalProcess());

        if (subSystem instanceof EnginePackage)
            throwException(
                    "Release call was attempted on the game engine itself. This is not allowed under any circumstance");

        if (this.garbageCollection.contains(subSystem))
            return;

        this.garbageCollection.add(subSystem);

        return;
    }

    // System Retrieval \\

    @SuppressWarnings("unchecked")
    public final <T> T get(Class<T> type) {

        if (this.internal.getInternalProcess() != InternalProcess.INIT)
            throwException(
                    "Get method was called from a process other than initialization. Current process: "
                            + this.getInternalProcess());

        for (SystemPackage frame : this.systemTree) {

            if (type.isAssignableFrom(frame.getClass()))
                return (T) frame;

            if (frame instanceof ManagerPackage manager) {

                T nested = manager.get(type);

                if (nested != null)
                    return nested;
            }
        }

        return null;
    }

    private final void cacheSubSystems() {
        this.systemArray = this.systemTree.toArray(new SystemPackage[0]);
    }

    // Create \\

    @Override
    void internalCreate() {

        super.internalCreate();

        this.cacheSubSystems();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalCreate();
    }

    // Init \\

    @Override
    void internalInit() {

        super.internalInit();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalInit();
    }

    // Awake \\

    @Override
    void internalAwake() {

        super.internalAwake();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalAwake();
    }

    // Free Memory \\

    @Override
    void internalFreeMemory() {

        super.internalFreeMemory();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalFreeMemory();

        clearGarbage();
    }

    // Start \\

    @Override
    void internalStart() {

        super.internalStart();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalStart();
    }

    // Update \\

    @Override
    void internalUpdate() {

        super.internalUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalUpdate();
    }

    // Menu Exclusive Update \\

    @Override
    void internalMenuExclusiveUpdate() {

        super.internalMenuExclusiveUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalMenuExclusiveUpdate();
    }

    // Game Exclusive Update \\

    @Override
    void internalGameExclusiveUpdate() {

        super.internalGameExclusiveUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalGameExclusiveUpdate();
    }

    // Fixed Update \\

    @Override
    void internalFixedUpdate() {

        super.internalFixedUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalFixedUpdate();
    }

    // Late Update \\

    @Override
    void internalLateUpdate() {

        super.internalLateUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalLateUpdate();
    }

    // Render \\

    @Override
    void internalRender() {

        super.internalRender();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalRender();
    }

    // Dispose \\

    @Override
    void internalDispose() {

        super.internalDispose();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalDispose();
    }

    // Utility \\

    private void clearGarbage() {

        if (garbageCollection.isEmpty())
            return;

        for (SystemPackage target : garbageCollection)
            systemTree.remove(target);

        garbageCollection.clear();

        cacheSubSystems();
    }
}
