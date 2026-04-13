package engine.root;

import application.kernel.util.window.WindowInstance;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public abstract class ContextPackage extends ManagerPackage {

    /*
     * Base class for all render contexts. Permanently paired with a window at
     * creation time — both sides hold a reference to each other. Pairing is
     * enforced by EnginePackage.createContext() and cannot change after creation.
     * Systems inside a context call context.getWindow() to reach their render
     * target without knowing which window they are targeting.
     *
     * Contexts are self-sovereign: their lifecycle phase advances independently
     * of the engine's global phase. CREATE, GET, AWAKE, and RELEASE fire
     * immediately on createContext(). START fires on the next frame before the
     * first update cycle. Context-owned systems are stored in a local registry
     * and are not globally visible — cross-context system access is not supported.
     *
     * The render queue lives on the WindowInstance, not the context. The context
     * drives what runs — the window owns where it draws. Systems set the active
     * window before pushing render calls so calls land in the correct queue.
     */

    // Window
    private WindowInstance window;

    // Local Registry
    private Object2ObjectOpenHashMap<Class<?>, SystemPackage> localRegistry;

    // Lifecycle
    boolean pendingStart;

    // Internal \\

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