package engine.root;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import engine.settings.Settings;

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
     * - Automatic LoaderPackage reference assignment via internalLoader,
     * both on create() and on get() so managers that retrieve a loader
     * from a parent also have it wired correctly.
     * - Context propagation — systems created under a ContextPackage
     * receive a reference to it automatically. Systems nested under
     * non-context managers inherit their manager's context transitively,
     * so any depth of nesting is handled without extra wiring.
     */

    // System Management
    SystemPackage[] systemArray;
    List<SystemPackage> systemCollection;
    List<Class<?>> garbageCollection;

    // Loader
    //
    // Automatically assigned when any LoaderPackage subclass is created
    // via create() or retrieved via get(). Access via `this.internalLoader`
    // in any ManagerPackage subclass without any additional setup.
    protected LoaderPackage internalLoader;

    // Internal \\

    protected ManagerPackage(Settings settings) {
        super(settings);
        this.systemArray = new SystemPackage[0];
        this.systemCollection = new ArrayList<>();
        this.garbageCollection = new ArrayList<>();
    }

    protected ManagerPackage() {
        super();
        this.systemArray = new SystemPackage[0];
        this.systemCollection = new ArrayList<>();
        this.garbageCollection = new ArrayList<>();
    }

    // System Registry \\

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends UtilityPackage> T create(Class<T> systemClass) {
        if (systemClass == EnginePackage.class)
            throwException("Only one engine package is allowed at any given time");

        if (SystemPackage.class.isAssignableFrom(systemClass)) {
            T result = (T) this.createSystem((Class<? extends SystemPackage>) systemClass);

            // Auto-assign the first LoaderPackage created on this manager.
            // Subsequent create() calls overwrite internalLoader, allowing
            // a manager to swap loaders across different lifecycle phases.
            if (result instanceof LoaderPackage)
                this.internalLoader = (LoaderPackage) result;

            return result;
        }

        return super.create(systemClass);
    }

    // System Retrieval \\

    /*
     * Overrides SystemPackage.get() to intercept LoaderPackage retrievals.
     * When a manager fetches a loader via get(), internalLoader is assigned
     * automatically — same guarantee as create(). Context-local systems are
     * checked first when this manager belongs to a context.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected <T> T get(Class<T> type) {

        if (this.context != null) {
            T local = this.context.getLocal(type);
            if (local != null) {
                if (local instanceof LoaderPackage)
                    this.internalLoader = (LoaderPackage) local;
                return local;
            }
        }

        T result = this.internal.get(true, type);
        if (result instanceof LoaderPackage)
            this.internalLoader = (LoaderPackage) result;
        return result;
    }

    // On-Demand Loading \\

    /*
     * Routes an immediate load request through the active loader.
     * The concrete loader's request(File) pulls the file from the pending
     * queue if still present and calls load() right now, in addition to
     * whatever batch work happens this frame.
     *
     * Crashes clearly if the loader has already self-released — a missing
     * resource requested after the loader is gone is always a logic error.
     */
    protected void requestFromLoader(File file) {
        if (this.internalLoader == null)
            throwException(
                    "On-demand load requested but loader has already been released.\n" +
                            "Requested file: " + file.getAbsolutePath() + "\n" +
                            "Ensure the resource exists in the scan directory or " +
                            "request it before the loader finishes.");
        this.internalLoader.request(file);
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    protected <T extends SystemPackage> T registerSystem(T systemPackage) {
        this.internal.internalRegistry.put(systemPackage.getClass(), systemPackage);
        this.systemCollection.add(systemPackage);

        // If this manager IS a context, own the child directly.
        // Otherwise, pass down whatever context this manager belongs to —
        // so systems nested arbitrarily deep all receive the same context
        // without any manual wiring.
        if (this instanceof ContextPackage)
            systemPackage.context = (ContextPackage) this;
        else if (this.context != null)
            systemPackage.context = this.context;

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

            // Clear internalLoader reference if this loader is being released
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
