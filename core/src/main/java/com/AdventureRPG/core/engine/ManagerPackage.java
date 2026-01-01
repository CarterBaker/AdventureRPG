package com.AdventureRPG.core.engine;

import java.util.ArrayList;
import java.util.List;

public abstract class ManagerPackage extends SystemPackage {

    /*
     * This is the intermediary class between the engine and individual
     * systems. ManagerPackages can contain and manage multiple child
     * SystemPackages, propagating lifecycle calls down the hierarchy.
     * This allows for modular organization where related systems can
     * be grouped under a single manager.
     *
     * Key responsibilities:
     * - Child system registration and lifecycle management
     * - Garbage collection of released systems
     * - Lifecycle propagation to all registered children
     */

    // System Management
    SystemPackage[] systemArray;
    private List<SystemPackage> systemCollection;
    private List<SystemPackage> garbageCollection;

    // Internal \\

    public ManagerPackage() {

        // System Management
        this.systemArray = new SystemPackage[0];
        this.systemCollection = new ArrayList<>();
        this.garbageCollection = new ArrayList<>();
    }

    // System Registry \\

    protected SystemPackage register(SystemPackage subSystem) {

        if (this.getContext() != InternalContext.CREATE)
            throwException(
                    "Subsystem registration rejected.\n" +
                            "Attempted during process: " + this.getContext() + "\n" +
                            "Allowed process: CREATE");

        return internalRegister(subSystem);
    }

    SystemPackage internalRegister(SystemPackage subSystem) {

        if (subSystem instanceof EnginePackage)
            throwException("Only one engine package is allowed at any given time");

        if (this.systemCollection.contains(subSystem))
            throwException("Subsystem: " + subSystem.getClass().getSimpleName()
                    + ", Already exists within the internal engine. Only one instance of any given system can exist at a time");

        this.systemCollection.add(subSystem);

        subSystem.register(
                settings,
                internal,
                this);

        return internal.internalRegister(subSystem);
    }

    // System Release \\

    protected final SystemPackage release(SystemPackage subSystem) {
        this.internalRelease(subSystem);
        return null;
    }

    void internalRelease(SystemPackage subSystem) {

        if (this.getContext() != InternalContext.FREE_MEMORY)
            throwException(
                    "Subsystem release rejected.\n" +
                            "Attempted during process: " + this.getContext() + "\n" +
                            "Allowed process: FREE_MEMORY");

        if (subSystem == internal)
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

        this.clearGarbage();
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

    final void cacheSubSystems() {
        this.systemArray = this.systemCollection.toArray(new SystemPackage[0]);
    }

    void clearGarbage() {

        if (this.garbageCollection.isEmpty())
            return;

        for (SystemPackage target : garbageCollection)
            this.systemCollection.remove(target);

        this.garbageCollection.clear();

        this.cacheSubSystems();
    }
}
