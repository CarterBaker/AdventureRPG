package com.internal.core.engine;

import java.util.ArrayList;
import java.util.List;

import com.internal.core.engine.settings.Settings;

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
     * - Lifecycle propagation to all created children
     */

    // System Management
    SystemPackage[] systemArray;
    List<SystemPackage> systemCollection;
    List<Class<?>> garbageCollection;

    // Internal \\

    protected ManagerPackage(Settings settings) {

        // Internal
        super(settings);

        this.systemArray = new SystemPackage[0];
        this.systemCollection = new ArrayList<>();
        this.garbageCollection = new ArrayList<>();
    }

    protected ManagerPackage() {

        // Internal System Package
        super();

        // Internal
        this.systemArray = new SystemPackage[0];
        this.systemCollection = new ArrayList<>();
        this.garbageCollection = new ArrayList<>();
    }

    // System Registry \\

    @Override // From `SystemPackage`
    @SuppressWarnings("unchecked")
    protected <T extends EngineUtility> T create(Class<T> systemClass) {

        if (systemClass == EnginePackage.class)
            throwException("Only one engine package is allowed at any given time");

        if (SystemPackage.class.isAssignableFrom(systemClass))
            return (T) this.createSystem((Class<? extends SystemPackage>) systemClass);

        return super.create(systemClass);
    }

    @SuppressWarnings("unchecked")
    <T extends SystemPackage> T createSystem(Class<T> systemClass) {

        if (this.getContext() != SystemContext.KERNEL &&
                this.internal.getContext() != SystemContext.BOOTSTRAP &&
                this.internal.getContext() != SystemContext.CREATE)
            throwException(
                    "Subsystem registration rejected.\n" +
                            "Attempted during process: " + this.getContext() + "\n" +
                            "Allowed processes: KERNEL, BOOTSTRAP, CREATE");

        try {

            // Prepare system creation context
            SystemPackage.setupConstructor(
                    this.settings,
                    this.internal,
                    this);

            var constructor = systemClass.getDeclaredConstructor();
            constructor.setAccessible(true);

            T systemPackage = constructor.newInstance();

            if (this.internal.internalRegistry.containsKey(systemPackage.getClass()))
                throwException(
                        "Subsystem: " + systemPackage.getClass().getSimpleName() +
                                " already exists within the internal engine");

            return this.registerSystem(systemPackage);
        }

        catch (Exception e) {
            throw new InternalException(
                    "Failed to create system: " + systemClass.getSimpleName(),
                    e);
        }

        finally {
            SystemPackage.SYSTEM_STRUCT.remove();
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends SystemPackage> T registerSystem(T systemPackage) {

        this.internal.internalRegistry.put(systemPackage.getClass(), systemPackage);
        this.systemCollection.add(systemPackage);

        return systemPackage;
    }

    // System Release \\

    @SuppressWarnings("unchecked")
    public <T extends SystemPackage> T release(Class<T> systemClass) {

        if (this.getContext() != SystemContext.RELEASE)
            throwException(
                    "Subsystem release rejected.\n" +
                            "Attempted during process: " + this.getContext() + "\n" +
                            "Allowed process: RELEASE");

        if (systemClass == EnginePackage.class)
            throwException(
                    "Release call was attempted on the game engine itself. This is not allowed under any circumstance");

        this.internalRelease(systemClass);

        return null;
    }

    @SuppressWarnings("unchecked")
    void internalRelease(Class<?> systemClass) {

        if (this.garbageCollection.contains(systemClass))
            return;

        this.garbageCollection.add(systemClass);
    }

    void clearGarbage() {

        if (this.garbageCollection.isEmpty())
            return;

        for (Class<?> systemClass : garbageCollection) {

            SystemPackage systemPackage = this.internal.internalRegistry.get(systemClass);

            this.internal.internalRegistry.remove(systemClass);
            this.systemCollection.remove(systemPackage);
        }

        this.garbageCollection.clear();

        this.cacheSubSystems();
    }

    // Create \\

    @Override // From `SystemPackage`
    void internalCreate() {

        super.internalCreate();

        this.cacheSubSystems();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalCreate();
    }

    // Get \\

    @Override // From `SystemPackage`
    void internalGet() {

        super.internalGet();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalGet();
    }

    // Awake \\

    @Override // From `SystemPackage`
    void internalAwake() {

        super.internalAwake();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalAwake();
    }

    // Release \\

    @Override // From `SystemPackage`
    void internalRelease() {

        super.internalRelease();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalRelease();

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
}
