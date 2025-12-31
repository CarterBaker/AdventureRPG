package com.AdventureRPG.core.engine;

import java.util.ArrayList;
import java.util.List;

public abstract class ManagerPackage extends SystemPackage {

    // Internal
    private List<SystemPackage> systemCollection = new ArrayList<>();
    private SystemPackage[] systemArray = new SystemPackage[0];
    private List<SystemPackage> garbageCollection = new ArrayList<>();

    // System Registry \\

    protected final SystemPackage register(SystemPackage subSystem) {
        return internalRegister(subSystem);
    }

    SystemPackage internalRegister(SystemPackage subSystem) {

        if (this.getContext() != InternalContext.CREATE)
            throwException(
                    "Register method was called from a process other than create. Current process: "
                            + this.getContext());

        if (subSystem instanceof EnginePackage)
            throwException("Only one engine package is allowed at any given time");

        if (this.systemCollection.contains(subSystem))
            throwException("Subsystem: " + subSystem.getClass().getSimpleName()
                    + ", Already exists within the internal engine. Only one instance of any given system can exist at a time");

        this.systemCollection.add(subSystem);

        setupNewSubSystem(subSystem);

        return subSystem;
    }

    private void setupNewSubSystem(SystemPackage subSystem) {

        subSystem.register(
                settings,
                internal,
                this);

        internal.registerToSystemRegistry(subSystem);
    }

    // System Release \\

    protected final SystemPackage release(SystemPackage subSystem) {
        internalRelease(subSystem);
        return null;
    }

    void internalRelease(SystemPackage subSystem) {

        if (this.getContext() != InternalContext.FREE_MEMORY)
            throwException(
                    "Release method was called from a process other than free memory. Current process: "
                            + this.getContext());

        if (subSystem instanceof EnginePackage)
            throwException(
                    "Release call was attempted on the game engine itself. This is not allowed under any circumstance");

        if (this.garbageCollection.contains(subSystem))
            return;

        this.garbageCollection.add(subSystem);
    }

    // Create \\

    @Override // From `SystemPackage`
    void internalCreate() {

        super.internalCreate();

        this.cacheSubSystems();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalCreate();
    }

    // Init \\

    @Override // From `SystemPackage`
    void internalInit() {

        super.internalInit();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalInit();
    }

    // Awake \\

    @Override // From `SystemPackage`
    void internalAwake() {

        super.internalAwake();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalAwake();
    }

    // Free Memory \\

    @Override // From `SystemPackage`
    void internalFreeMemory() {

        super.internalFreeMemory();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalFreeMemory();

        clearGarbage();
    }

    // Start \\

    @Override // From `SystemPackage`
    void internalStart() {

        super.internalStart();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalStart();
    }

    // Update \\

    @Override // From `SystemPackage`
    void internalUpdate() {

        super.internalUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalUpdate();
    }

    // Menu Exclusive Update \\

    @Override // From `SystemPackage`
    void internalMenuExclusiveUpdate() {

        super.internalMenuExclusiveUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalMenuExclusiveUpdate();
    }

    // Game Exclusive Update \\

    @Override // From `SystemPackage`
    void internalGameExclusiveUpdate() {

        super.internalGameExclusiveUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalGameExclusiveUpdate();
    }

    // Fixed Update \\

    @Override // From `SystemPackage`
    void internalFixedUpdate() {

        super.internalFixedUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalFixedUpdate();
    }

    // Late Update \\

    @Override // From `SystemPackage`
    void internalLateUpdate() {

        super.internalLateUpdate();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalLateUpdate();
    }

    // Render \\

    @Override // From `SystemPackage`
    void internalRender() {

        super.internalRender();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalRender();
    }

    // Dispose \\

    @Override // From `SystemPackage`
    void internalDispose() {

        super.internalDispose();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalDispose();
    }

    // Utility \\

    private final void cacheSubSystems() {
        this.systemArray = this.systemCollection.toArray(new SystemPackage[0]);
    }

    void clearGarbage() {

        if (garbageCollection.isEmpty())
            return;

        for (SystemPackage target : garbageCollection)
            systemCollection.remove(target);

        garbageCollection.clear();

        cacheSubSystems();
    }
}
