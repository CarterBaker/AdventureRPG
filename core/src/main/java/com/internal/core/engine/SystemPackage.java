package com.internal.core.engine;

import java.util.concurrent.Future;

import com.internal.core.engine.settings.Settings;
import com.internal.core.kernel.InternalThreadManager.InternalThreadManager.AsyncStructConsumer;
import com.internal.core.kernel.InternalThreadManager.InternalThreadManager.AsyncStructConsumerMulti;

public abstract class SystemPackage extends EngineUtility {

    /*
     * This is the base class for all systems within the engine.
     * SystemPackages follow a strict lifecycle with automatic phase
     * verification to ensure proper execution order. Each phase can
     * be overridden by child classes to implement custom behavior.
     *
     * Lifecycle order:
     * CREATE → GET → AWAKE → RELEASE → START →
     * UPDATE → FIXED_UPDATE → LATE_UPDATE → RENDER → DISPOSE
     *
     * Systems must be created through a ManagerPackage or
     * EnginePackage to function properly.
     */

    // Core
    static final ThreadLocal<SystemStruct> SYSTEM_STRUCT = new ThreadLocal<>();

    // Main
    public final Settings settings;
    public final EnginePackage internal;
    public final ManagerPackage local;

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
         * A container used to ensure proper system creation during the creation
         * phase of the internal engines lifecycle. Mainly serves as a temporary data
         * transfer mechanism.
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

        if (this.internal.internalContext != targetContext
                || !targetContext.canEnterFrom(this.internalContext.order))
            return false;

        this.setContext(targetContext);
        return true;
    }

    // System Registry \\

    @SuppressWarnings("unchecked")
    protected <T extends EngineUtility> T create(Class<T> targetClass) {

        if (InstancePackage.class.isAssignableFrom(targetClass))
            return (T) this.createInstance((Class<? extends InstancePackage>) targetClass);

        return throwException(
                "SystemPackage can only create InstancePackage types.");
    }

    final <T extends InstancePackage> T createInstance(Class<T> instanceClass) {

        try {

            InstancePackage.setupConstructor(this.internal, this);

            T instance = instanceClass.getDeclaredConstructor().newInstance();

            instance.internalCreate();
            instance.internalGet();
            instance.internalAwake();

            return instance;
        }

        catch (Exception e) {

            throwException("Failed to create instance: " + e.getMessage());

            return null;
        }

        finally {
            InstancePackage.CREATION_STRUCT.remove();
        }
    }

    // System Retrieval \\

    @SuppressWarnings("unchecked")
    protected final <T> T get(Class<T> type) {
        return this.internal.get(false, type);
    }

    // Thread Management \\

    protected ThreadHandle getThreadHandleFromThreadName(String threadName) {
        return internal.getThreadHandleFromThreadName(threadName);
    }

    protected Future<?> executeAsync(ThreadHandle handle, Runnable task) {
        return internal.executeAsync(handle, task);
    }

    protected <T extends AsyncContainerPackage> Future<?> executeAsync(
            ThreadHandle handle,
            AsyncContainerPackage asyncStruct,
            AsyncStructConsumer<T> consumer) {

        return internal.executeAsync(handle, asyncStruct, consumer);
    }

    protected Future<?> executeAsync(
            ThreadHandle handle,
            AsyncStructConsumerMulti consumer,
            AsyncContainerPackage... asyncStructs) {

        return internal.executeAsync(handle, consumer, asyncStructs);
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

    // FreeMemory \\

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
