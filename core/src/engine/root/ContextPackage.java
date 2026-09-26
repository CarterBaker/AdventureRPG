package engine.root;

import application.kernel.windowpipeline.window.WindowInstance;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public abstract class ContextPackage extends ManagerPackage {

    /*
     * Attaches a set of systems to a window and runs them through the engine
     * lifecycle. Every system created beneath a context, at any depth, lives
     * in its local registry, so two contexts never share or overwrite each
     * other's systems. A context given a crash listener is isolated: a failure
     * is recorded here instead of ending the process, and the listener closes it.
     */

    // Window
    private WindowInstance window;

    // Local Registry
    private Object2ObjectOpenHashMap<Class<?>, SystemPackage> localRegistry;

    // Lifecycle
    boolean pendingStart;

    // Isolation
    private Runnable crashListener;
    private volatile Throwable crashCause;

    // Internal \\

    protected ContextPackage() {

        super();

        // Local Registry
        this.localRegistry = new Object2ObjectOpenHashMap<>();
    }

    @Override
    boolean verifyContext(SystemContext targetContext) {

        if (!targetContext.canEnterFrom(this.internalContext.order))
            return false;

        this.internalContext = targetContext;
        return true;
    }

    // Local Registry \\

    @Override
    protected <T extends SystemPackage> T registerSystem(T systemPackage) {

        this.registerLocal(systemPackage);
        this.systemCollection.add(systemPackage);
        systemPackage.context = this;

        return systemPackage;
    }

    @Override
    SystemPackage lookupRegistered(Class<?> systemClass) {
        return this.localRegistry.get(systemClass);
    }

    @Override
    void unregister(Class<?> systemClass) {
        this.unregisterLocal(systemClass);
    }

    void registerLocal(SystemPackage systemPackage) {

        if (this.localRegistry.containsKey(systemPackage.getClass()))
            throwException(
                    "System already registered in this context.\n" +
                            "System: " + systemPackage.getClass().getSimpleName());

        this.localRegistry.put(systemPackage.getClass(), systemPackage);
    }

    void unregisterLocal(Class<?> systemClass) {
        this.localRegistry.remove(systemClass);
    }

    @SuppressWarnings("unchecked")
    <T> T getLocal(Class<T> type) {
        return (T) this.localRegistry.get(type);
    }

    // Isolation \\

    void setCrashListener(Runnable crashListener) {
        this.crashListener = crashListener;
    }

    synchronized void crash(Throwable failure) {

        if (crashCause != null)
            return;

        this.crashCause = failure;
        errorLog(getClass().getSimpleName() + " crashed and was stopped.", failure);
    }

    void notifyCrashListener() {
        if (crashListener != null)
            crashListener.run();
    }

    boolean isIsolated() {
        return crashListener != null;
    }

    public boolean isCrashed() {
        return crashCause != null;
    }

    // Resize \\

    public void onResize(int width, int height) {
    }

    // Accessible \\

    public WindowInstance getWindow() {
        return window;
    }

    void setWindow(WindowInstance window) {
        this.window = window;
    }

    public boolean hasWindow() {
        return window != null;
    }
}