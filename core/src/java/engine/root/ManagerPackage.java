package engine.root;

import java.io.File;

import engine.settings.Settings;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public abstract class ManagerPackage extends SystemPackage {

    /*
     * Intermediary between the engine and individual systems. Owns child
     * systems, propagates every lifecycle phase down to them, and releases
     * them through its garbage list. Children inherit this manager's context
     * at any depth and register into that context's local registry; managers
     * outside a context register into the engine registry. The last loader
     * created or retrieved is tracked as internalLoader for on-demand loads.
     */

    // System Management
    SystemPackage[] systemArray;
    ObjectArrayList<SystemPackage> systemCollection;
    ObjectArrayList<Class<?>> garbageCollection;

    // Loader
    protected LoaderPackage internalLoader;

    // Internal \\

    protected ManagerPackage(Settings settings) {

        super(settings);

        // System Management
        this.systemArray = new SystemPackage[0];
        this.systemCollection = new ObjectArrayList<>();
        this.garbageCollection = new ObjectArrayList<>();
    }

    protected ManagerPackage() {

        super();

        // System Management
        this.systemArray = new SystemPackage[0];
        this.systemCollection = new ObjectArrayList<>();
        this.garbageCollection = new ObjectArrayList<>();
    }

    // System Registry \\

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends UtilityPackage> T create(Class<T> systemClass) {

        if (systemClass == EnginePackage.class)
            throwException("Only one engine package is allowed at any given time");

        if (SystemPackage.class.isAssignableFrom(systemClass)) {

            T result = (T) this.createSystem((Class<? extends SystemPackage>) systemClass);

            if (result instanceof LoaderPackage loader)
                this.internalLoader = loader;

            return result;
        }

        return super.create(systemClass);
    }

    <T extends SystemPackage> T createSystem(Class<T> systemClass) {

        if (this.getContext() != SystemContext.KERNEL &&
                this.getContext() != SystemContext.CREATE &&
                this.internal.getContext() != SystemContext.BOOTSTRAP &&
                this.internal.getContext() != SystemContext.CREATE)
            throwException(
                    "Subsystem registration rejected.\n" +
                            "Attempted during process: " + this.getContext() + "\n" +
                            "Allowed processes: KERNEL, BOOTSTRAP, CREATE");

        try {
            SystemPackage.setupConstructor(
                    this.settings,
                    this.internal,
                    this);
            var constructor = systemClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            T systemPackage = constructor.newInstance();
            return this.registerSystem(systemPackage);
        } catch (Exception e) {
            throw new InternalException(
                    "Failed to create system: " + systemClass.getSimpleName(),
                    e);
        } finally {
            SystemPackage.SYSTEM_STRUCT.remove();
        }
    }

    protected <T extends SystemPackage> T registerSystem(T systemPackage) {

        if (this.context != null)
            this.context.registerLocal(systemPackage);
        else
            this.internal.internalRegistry.put(systemPackage.getClass(), systemPackage);

        this.systemCollection.add(systemPackage);
        systemPackage.context = this.context;

        return systemPackage;
    }

    SystemPackage lookupRegistered(Class<?> systemClass) {

        if (this.context != null)
            return this.context.lookupRegistered(systemClass);

        return this.internal.internalRegistry.get(systemClass);
    }

    void unregister(Class<?> systemClass) {

        if (this.context != null)
            this.context.unregisterLocal(systemClass);
        else
            this.internal.internalRegistry.remove(systemClass);
    }

    // System Retrieval \\

    @Override
    protected <T> T get(Class<T> type) {

        T result = super.get(type);

        if (result instanceof LoaderPackage loader)
            this.internalLoader = loader;

        return result;
    }

    // On-Demand \\

    protected void requestFromLoader(File file) {

        if (this.internalLoader == null)
            throwException(
                    "On-demand load requested but loader has already been released.\n" +
                            "Requested file: " + file.getAbsolutePath() + "\n" +
                            "Ensure the resource exists in the scan directory or " +
                            "request it before the loader finishes.");

        this.internalLoader.request(file);
    }

    // System Release \\

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

    void internalRelease(Class<?> systemClass) {

        if (this.garbageCollection.contains(systemClass))
            return;

        this.garbageCollection.add(systemClass);
    }

    void clearGarbage() {

        if (this.garbageCollection.isEmpty())
            return;

        for (int i = 0; i < this.garbageCollection.size(); i++) {

            Class<?> systemClass = this.garbageCollection.get(i);
            SystemPackage systemPackage = this.lookupRegistered(systemClass);
            this.unregister(systemClass);
            this.systemCollection.remove(systemPackage);

            if (systemPackage == this.internalLoader)
                this.internalLoader = null;
        }

        this.garbageCollection.clear();
        this.cacheSubSystems();
    }

    // Create \\

    @Override
    void internalCreate() {

        super.internalCreate();
        this.cacheSubSystems();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalCreate();
    }

    // Get \\

    @Override
    void internalGet() {

        super.internalGet();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalGet();
    }

    // Awake \\

    @Override
    void internalAwake() {

        super.internalAwake();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalAwake();
    }

    // Release \\

    @Override
    void internalRelease() {

        super.internalRelease();

        for (int i = 0; i < this.systemArray.length; i++)
            this.systemArray[i].internalRelease();

        this.clearGarbage();
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

    final void cacheSubSystems() {
        this.systemArray = this.systemCollection.toArray(new SystemPackage[0]);
    }
}
