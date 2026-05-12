package engine.root;

import application.kernel.windowpipeline.window.WindowInstance;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public abstract class ContextPackage extends ManagerPackage {

    // Window
    private WindowInstance window;

    // Local Registry
    private Object2ObjectOpenHashMap<Class<?>, SystemPackage> localRegistry;

    // Lifecycle
    boolean pendingStart;

    protected ContextPackage() {
        super();
        this.localRegistry = new Object2ObjectOpenHashMap<>();
    }

    @Override
    boolean verifyContext(SystemContext targetContext) {
        if (!targetContext.canEnterFrom(this.internalContext.order))
            return false;
        this.internalContext = targetContext;
        return true;
    }

    @Override
    protected <T extends SystemPackage> T registerSystem(T systemPackage) {
        if (this.localRegistry.containsKey(systemPackage.getClass()))
            throwException(
                    "System already registered in this context.\n" +
                            "System: " + systemPackage.getClass().getSimpleName());
        this.localRegistry.put(systemPackage.getClass(), systemPackage);
        this.systemCollection.add(systemPackage);
        systemPackage.context = this;
        return systemPackage;
    }

    @SuppressWarnings("unchecked")
    <T> T getLocal(Class<T> type) {
        return (T) this.localRegistry.get(type);
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