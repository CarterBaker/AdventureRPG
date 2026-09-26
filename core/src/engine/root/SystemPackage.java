package engine.root;

import java.util.concurrent.Future;

import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.settings.Settings;

public abstract class SystemPackage extends UtilityPackage {

    /*
     * Base class for every system in the engine. Runs the strict lifecycle
     * CREATE, GET, AWAKE, RELEASE, START, then UPDATE, FIXED_UPDATE,
     * LATE_UPDATE and RENDER each frame, and DISPOSE at shutdown, verifying
     * each phase transition. Systems are only ever created through a manager.
     */

    // Core
    static final ThreadLocal<SystemStruct> SYSTEM_STRUCT = new ThreadLocal<>();

    // Main
    public final Settings settings;
    public final EnginePackage internal;
    public final ManagerPackage local;

    // Context
    protected ContextPackage context;

    // Internal
    SystemContext internalContext;

    // Internal \\

    protected SystemPackage(Settings settings) {

        // Main
        this.settings = settings;
        this.internal = (EnginePackage) this;
        this.local = (ManagerPackage) this;

        // Internal
        this.internalContext = SystemContext.NULL;
    }

    protected SystemPackage() {

        // Core
        SystemStruct data = SYSTEM_STRUCT.get();

        if (data == null)
            throwException("Systems must be created via internal engine `create` method");

        if (data.settings == null || data.internal == null || data.local == null)
            throwException("SystemData was incomplete during system creation");

        // Main
        this.settings = data.settings;
        this.internal = data.internal;
        this.local = data.local;

        // Internal
        this.internalContext = SystemContext.NULL;
    }

    static final class SystemStruct extends StructPackage {

        /*
         * Carries the settings, engine and owning manager through reflective
         * construction so every system is wired before its constructor returns.
         */

        // Internal
        final Settings settings;
        final EnginePackage internal;
        final ManagerPackage local;

        // Internal \\

        SystemStruct(
                Settings settings,
                EnginePackage internal,
                ManagerPackage local) {

            // Internal
            this.settings = settings;
            this.internal = internal;
            this.local = local;
        }
    }

    static void setupConstructor(
            Settings settings,
            EnginePackage internal,
            ManagerPackage local) {
        SYSTEM_STRUCT.set(new SystemStruct(settings, internal, local));
    }

    // System Context \\

    final SystemContext getContext() {
        return this.internalContext;
    }

    void setContext(SystemContext targetContext) {

        if (!targetContext.canEnterFrom(this.internalContext.order))
            throwException("Firing order issue. Internal engine attempted to perform an illegal context set.");
        this.internalContext = targetContext;
    }

    boolean verifyContext(SystemContext targetContext) {

        SystemPackage authority = (this.context != null)
                ? (SystemPackage) this.context
                : this.internal;

        if (authority.internalContext != targetContext
                || !targetContext.canEnterFrom(this.internalContext.order))
            return false;

        this.setContext(targetContext);
        return true;
    }

    // System Registry \\

    @SuppressWarnings("unchecked")
    protected <T extends UtilityPackage> T create(Class<T> targetClass) {

        if (InstancePackage.class.isAssignableFrom(targetClass))
            return (T) this.createInstance((Class<? extends InstancePackage>) targetClass);
        return throwException(
                "SystemPackage can only create InstancePackage types.");
    }

    final <T extends InstancePackage> T createInstance(Class<T> instanceClass) {

        try {
            InstancePackage.setupConstructor(this.internal, this);
            var constructor = instanceClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();
            instance.internalCreate();
            instance.internalGet();
            instance.internalAwake();
            return instance;
        } catch (Exception e) {
            return throwException("Failed to create instance: " + instanceClass.getSimpleName(), e);
        } finally {
            InstancePackage.CREATION_STRUCT.remove();
        }
    }

    // System Retrieval \\

    protected <T> T get(Class<T> type) {
        return this.internal.getUnchecked(this.context, type);
    }

    // Thread Management \\

    protected ThreadHandle getThreadHandleFromThreadName(String threadName) {
        return internal.getThreadHandleFromThreadName(threadName);
    }

    protected Future<?> executeAsync(ThreadHandle handle, Runnable task) {
        return internal.executeAsync(handle, task);
    }

    // Create \\

    void internalCreate() {
        if (!this.verifyContext(SystemContext.CREATE))
            return;
        this.create();
    }

    protected void create() {
    }

    // Get \\

    void internalGet() {
        if (!this.verifyContext(SystemContext.GET))
            return;
        this.get();
    }

    protected void get() {
    }

    // Awake \\

    void internalAwake() {
        if (!this.verifyContext(SystemContext.AWAKE))
            return;
        this.awake();
    }

    protected void awake() {
    }

    // Release \\

    void internalRelease() {
        if (!this.verifyContext(SystemContext.RELEASE))
            return;
        this.release();
    }

    protected void release() {
    }

    // Start \\

    void internalStart() {
        if (!this.verifyContext(SystemContext.START))
            return;
        this.start();
    }

    protected void start() {
    }

    // Update \\

    void internalUpdate() {
        if (!this.verifyContext(SystemContext.UPDATE))
            return;
        this.update();
    }

    protected void update() {
    }

    // Fixed Update \\

    void internalFixedUpdate() {
        if (!this.verifyContext(SystemContext.FIXED_UPDATE))
            return;
        this.fixedUpdate();
    }

    protected void fixedUpdate() {
    }

    // Late Update \\

    void internalLateUpdate() {
        if (!this.verifyContext(SystemContext.LATE_UPDATE))
            return;
        this.lateUpdate();
    }

    protected void lateUpdate() {
    }

    // Render \\

    void internalRender() {
        if (!this.verifyContext(SystemContext.RENDER))
            return;
        this.render();
    }

    protected void render() {
    }

    // Dispose \\

    void internalDispose() {
        if (!this.verifyContext(SystemContext.DISPOSE))
            return;
        this.dispose();
    }

    protected void dispose() {
    }

    // Accessible \\

    protected final void debugContext() {
        this.debugContext("");
    }

    protected final void debugContext(Object input) {
        this.debug("[" + this.internalContext.toString() + "] " + String.valueOf(input));
    }
}