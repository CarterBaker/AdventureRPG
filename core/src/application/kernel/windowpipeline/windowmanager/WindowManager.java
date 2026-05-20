package application.kernel.windowpipeline.windowmanager;

import application.kernel.windowpipeline.window.WindowData;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ContextPackage;
import engine.root.EngineContext;
import engine.root.EngineUtility;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WindowManager extends ManagerPackage {

    /*
     * Owns all engine windows. Each frame resolves which window the cursor is
     * geometrically inside and stores it as hoveredWindow. No concept of active,
     * focused, or foreground window — all windows are equal participants. Input
     * routing is entirely hover-driven.
     *
     * OS windows are depth 0. Logical windows (tabs) are depth 1+.
     *
     * Hover resolution uses tightest-rect-wins: among all windows whose bounds
     * contain the cursor, the window with the smallest compositeRect area wins.
     * Depth breaks ties only when two windows have the same area. This prevents
     * full-screen overlay windows (e.g. toolbar at depth 3 with a full-screen
     * compositeRect) from stealing hover from smaller precisely-positioned windows
     * (e.g. tab windows at depth 1 with a dock-sized compositeRect).
     *
     * Windows without a compositeRect but with a native handle are treated as
     * infinite area and only win when no compositeRect window contains the cursor.
     *
     * hoveredWindowLocked prevents syncHoveredWindow from reassigning hoveredWindow
     * while an element hover is active. Lock is set by ElementHitSystem on hover
     * entry and released on hover exit.
     *
     * capturedWindow pins hoveredWindow when cursor capture is active. While set,
     * syncHoveredWindow is bypassed entirely — the captured window is always
     * considered hovered. Released when capture ends, restoring normal hover
     * resolution. This prevents cursor-locked gameplay from losing window focus
     * when multiple tabs are open.
     */

    private ObjectArrayList<WindowInstance> windows;
    private WindowInstance mainWindow;
    private WindowInstance hoveredWindow;
    private WindowInstance renderWindow;
    private WindowInstance contextWindow;
    private WindowInstance capturedWindow;

    private boolean hoveredWindowLocked;

    private int nextWindowID;

    // Internal \\

    @Override
    protected void create() {
        this.windows = new ObjectArrayList<>();
        this.nextWindowID = 1;
    }

    @Override
    protected void awake() {
        EngineUtility.assignWindowManager(this);
    }

    @Override
    protected void update() {
        syncHoveredWindow();

        for (int i = windows.size() - 1; i >= 0; i--) {
            WindowInstance window = windows.get(i);
            if (window == mainWindow)
                continue;
            if (!window.hasNativeHandle())
                continue;
            if (!internal.windowPlatform.shouldClose(window))
                continue;
            internal.windowPlatform.makeContextCurrent(window);
            window.dispose();
            internal.windowPlatform.destroyWindow(window);
        }
    }

    @Override
    protected void dispose() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            WindowInstance window = windows.get(i);
            if (window == mainWindow || !window.hasNativeHandle())
                continue;
            internal.windowPlatform.makeContextCurrent(window);
            window.dispose();
            internal.windowPlatform.destroyWindow(window);
        }
    }

    private void syncHoveredWindow() {

        if (hoveredWindowLocked)
            return;

        if (capturedWindow != null) {
            hoveredWindow = capturedWindow;
            return;
        }

        float mx = EngineContext.input.getMouseX();
        float my = EngineContext.input.getMouseY();

        WindowInstance topHit = null;
        long topArea = Long.MAX_VALUE;
        int topDepth = Integer.MIN_VALUE;

        for (int i = 0; i < windows.size(); i++) {
            WindowInstance w = windows.get(i);

            if (!isMouseOver(w, mx, my))
                continue;

            long area = w.hasCompositeRect()
                    ? (long) w.getCompositeW() * (long) w.getCompositeH()
                    : Long.MAX_VALUE;

            if (area < topArea || (area == topArea && w.getDepth() > topDepth)) {
                topArea = area;
                topDepth = w.getDepth();
                topHit = w;
            }
        }

        hoveredWindow = topHit;

        if (hoveredWindow != null)
            internal.windowPlatform.syncInputForWindow(hoveredWindow);
    }

    private boolean isMouseOver(WindowInstance w, float mx, float my) {
        if (w.hasCompositeRect())
            return mx >= w.getCompositeX()
                    && mx < w.getCompositeX() + w.getCompositeW()
                    && my >= w.getCompositeY()
                    && my < w.getCompositeY() + w.getCompositeH();
        if (w.hasNativeHandle())
            return true;
        return false;
    }

    // Registration \\

    public void registerMainWindow(WindowInstance window) {
        verifyWindowRegistration(window, true);
        this.mainWindow = window;
        this.hoveredWindow = window;
        registerWindow(window);
    }

    public void registerDetachedWindow(WindowInstance window) {
        verifyWindowRegistration(window, false);
        registerWindow(window);
    }

    public <T extends ContextPackage> WindowInstance openWindow(String title, Class<T> contextClass) {
        WindowData data = new WindowData(issueWindowID(), mainWindow.getWidth(), mainWindow.getHeight(), title);
        WindowInstance window = create(WindowInstance.class);
        window.constructor(data);
        registerDetachedWindow(window);
        internal.createContext(contextClass, window);
        return window;
    }

    public WindowInstance createLogicalWindow(String title, WindowInstance compositeTarget) {
        if (compositeTarget == null)
            throwException("Cannot create logical window without a composite target.");
        WindowData data = new WindowData(
                issueWindowID(),
                compositeTarget.getWidth(),
                compositeTarget.getHeight(),
                title,
                false);
        WindowInstance window = create(WindowInstance.class);
        window.constructor(data);
        window.setCompositeTarget(compositeTarget);
        registerDetachedWindow(window);
        return window;
    }

    private void registerWindow(WindowInstance window) {
        windows.add(window);
        if (window.getWindowData().shouldCreateOSWindow())
            internal.windowPlatform.openWindow(window);
    }

    public void removeWindow(WindowInstance window) {
        windows.remove(window);
        if (capturedWindow == window)
            capturedWindow = null;
        if (hoveredWindow == window)
            hoveredWindow = null;
    }

    // Identity \\

    public int issueWindowID() {
        return nextWindowID++;
    }

    // Render / context frame tracking \\

    public void beginRenderWindow(WindowInstance window) {
        this.renderWindow = window;
    }

    public void endRenderWindow() {
        this.renderWindow = null;
    }

    public void beginContextWindow(WindowInstance window) {
        this.contextWindow = window;
    }

    public void endContextWindow() {
        this.contextWindow = null;
    }

    // Hover lock \\

    public void lockHoveredWindow() {
        hoveredWindowLocked = true;
    }

    public void unlockHoveredWindow() {
        hoveredWindowLocked = false;
    }

    // Capture lock \\

    public void captureLockWindow(WindowInstance window) {
        this.capturedWindow = window;
    }

    public void releaseCaptureLock() {
        this.capturedWindow = null;
    }

    // Accessors \\

    public WindowInstance getMainWindow() {
        return mainWindow;
    }

    public WindowInstance getHoveredWindow() {
        return hoveredWindow;
    }

    public WindowInstance getRenderWindow() {
        return renderWindow;
    }

    public WindowInstance getContextWindow() {
        return contextWindow;
    }

    public ObjectArrayList<WindowInstance> getWindows() {
        return windows;
    }

    public boolean hasMainWindow() {
        return mainWindow != null;
    }

    public WindowInstance getCapturedWindow() {
        return capturedWindow;
    }

    // Validation \\

    private void verifyWindowRegistration(WindowInstance window, boolean isMain) {
        if (window == null)
            throwException("Cannot register null window.");
        int id = window.getWindowID();
        if (isMain && id != 0)
            throwException("Main window must use window ID 0. Received: " + id);
        if (!isMain && id == 0)
            throwException("Detached windows must not use window ID 0.");
        if (hasWindowID(id))
            throwException("Window ID already registered: " + id);
    }

    private boolean hasWindowID(int id) {
        for (int i = 0; i < windows.size(); i++)
            if (windows.get(i).getWindowID() == id)
                return true;
        return false;
    }
}