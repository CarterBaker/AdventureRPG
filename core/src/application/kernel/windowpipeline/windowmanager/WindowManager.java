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
     * Owns all engine windows. Each frame rebuilds hoveredWindows — the ordered
     * list of every window the cursor is currently inside, sorted by depth
     * descending, area ascending on ties. Index 0 is always the highest-priority
     * window; input systems iterate the list in order and take the first hit.
     *
     * OS windows are depth 0. Logical windows (tabs, composited panels) are
     * depth 1+, so they always sort above OS windows.
     *
     * Hover resolution is two-phase, executed per OS window:
     *
     * Phase 1 — all OS windows are queried via glfwGetCursorPos and synced via
     * syncInputForWindow unconditionally every frame, not just the ones the cursor
     * is inside. This eliminates cursor-staleness completely: GLFW cursor callbacks
     * only fire on the OS-focused window, but the direct query bypasses that.
     * No external lock mechanism is needed to keep coordinates current.
     * OS windows where the cursor falls within [0, width) × [0, height) are added
     * to hoveredWindows.
     *
     * Phase 2 — immediately after syncing each hit OS window, EngineContext.input
     * reflects its coordinate space. Logical windows whose composite target is
     * that OS window are tested against the Y-up cursor position. All matching
     * logical windows are added.
     *
     * The list is sorted once after all windows are evaluated. Downstream systems
     * receive it via getHoveredWindows() and iterate from index 0.
     *
     * capturedWindow overrides the scan — while set, hoveredWindows contains only
     * the captured window and its GL window is synced. This ensures
     * EngineContext.input
     * is maintained correctly during capture without any special-casing elsewhere.
     *
     * focusedWindow is the window that currently owns input. It is set on click
     * and is the single authority for cursor capture transitions. Capture follows
     * focus, not hover — only the focused window may acquire or release capture.
     */

    private ObjectArrayList<WindowInstance> windows;
    private final ObjectArrayList<WindowInstance> hoveredWindows = new ObjectArrayList<>();
    private WindowInstance mainWindow;
    private WindowInstance focusedWindow;
    private WindowInstance renderWindow;
    private WindowInstance contextWindow;
    private WindowInstance capturedWindow;

    private int nextWindowID;

    // Scratch — reused per frame to avoid allocation in the OS-window hot path
    private final float[] cursorPosScratch = new float[2];

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

        syncHoveredWindows();
        resolveClickFocus();

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

    private void syncHoveredWindows() {

        hoveredWindows.clear();

        if (capturedWindow != null) {
            hoveredWindows.add(capturedWindow);
            internal.windowPlatform.syncInputForWindow(capturedWindow.getGLWindow());
            return;
        }

        for (int i = 0; i < windows.size(); i++) {

            WindowInstance w = windows.get(i);

            if (!w.hasNativeHandle())
                continue;

            // Query and sync unconditionally — every OS window gets current cursor
            // coords regardless of which window holds OS focus. This is the only
            // place syncInputForWindow is called; no external refresh is needed.
            internal.windowPlatform.getCursorPos(w, cursorPosScratch);
            internal.windowPlatform.syncInputForWindow(w);

            float lx = cursorPosScratch[0];
            float ly = cursorPosScratch[1];

            if (lx < 0 || lx >= w.getWidth() || ly < 0 || ly >= w.getHeight())
                continue;

            // Phase 1 — OS window is hit
            hoveredWindows.add(w);

            // Phase 2 — logical windows targeting this OS window.
            // EngineContext.input now reflects this window's coordinate space,
            // so getMouseX/Y() matches the Y-up local space of composite rects.
            float mx = EngineContext.input.getMouseX();
            float my = EngineContext.input.getMouseY();

            for (int j = 0; j < windows.size(); j++) {
                WindowInstance logical = windows.get(j);
                if (logical.hasNativeHandle())
                    continue;
                if (!logical.hasCompositeRect())
                    continue;
                if (logical.getCompositeTarget() != w)
                    continue;
                if (isMouseOver(logical, mx, my))
                    hoveredWindows.add(logical);
            }
        }

        // Deepest depth first; smallest area wins on equal depth.
        hoveredWindows.sort((a, b) -> {
            int depthCmp = b.getDepth() - a.getDepth();
            if (depthCmp != 0)
                return depthCmp;
            return Long.compare(effectiveArea(a), effectiveArea(b));
        });
    }

    private long effectiveArea(WindowInstance w) {
        return w.hasNativeHandle()
                ? (long) w.getWidth() * w.getHeight()
                : (long) w.getCompositeW() * (long) w.getCompositeH();
    }

    private boolean isMouseOver(WindowInstance w, float mx, float my) {
        return mx >= w.getCompositeX()
                && mx < w.getCompositeX() + w.getCompositeW()
                && my >= w.getCompositeY()
                && my < w.getCompositeY() + w.getCompositeH();
    }

    private void resolveClickFocus() {

        if (hoveredWindows.isEmpty())
            return;

        // Only act on the frame the primary button is first pressed.
        // EngineContext.input is already current — syncHoveredWindows just ran.
        if (!EngineContext.input.isMouseClicked(0))
            return;

        // Walk depth-sorted list. focusIndependent=true means the window is
        // intentionally transparent to focus (toolbar, tab chrome). The first
        // non-transparent window geometrically under the cursor owns the click,
        // regardless of whether any UI element was hit inside it.
        for (int i = 0; i < hoveredWindows.size(); i++) {

            WindowInstance w = hoveredWindows.get(i);
            if (!w.isFocusIndependent()) {
                focusedWindow = w;
                return;
            }
        }
    }

    // Registration \\

    public void registerMainWindow(WindowInstance window) {
        verifyWindowRegistration(window, true);
        this.mainWindow = window;
        this.focusedWindow = window;
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
        hoveredWindows.remove(window);
        if (capturedWindow == window)
            capturedWindow = null;
        if (focusedWindow == window)
            focusedWindow = null;
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

    // Capture \\

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

    public WindowInstance getFocusedWindow() {
        return focusedWindow;
    }

    public WindowInstance getRenderWindow() {
        return renderWindow;
    }

    public WindowInstance getContextWindow() {
        return contextWindow;
    }

    public WindowInstance getCapturedWindow() {
        return capturedWindow;
    }

    public void setFocusedWindow(WindowInstance window) {
        this.focusedWindow = window;
    }

    /** The highest-priority hovered window (index 0 of hoveredWindows), or null. */
    public WindowInstance getHoveredWindow() {
        return hoveredWindows.isEmpty() ? null : hoveredWindows.get(0);
    }

    /**
     * All windows the cursor is currently inside, sorted depth descending, area
     * ascending.
     */
    public ObjectArrayList<WindowInstance> getHoveredWindows() {
        return hoveredWindows;
    }

    public ObjectArrayList<WindowInstance> getWindows() {
        return windows;
    }

    public boolean hasMainWindow() {
        return mainWindow != null;
    }

    public void reparentWindow(WindowInstance window, WindowInstance newParent) {
        window.setCompositeTarget(newParent);
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

    public void destroyOsWindow(WindowInstance window) {
        if (window == null || window == mainWindow || !window.hasNativeHandle())
            return;
        internal.windowPlatform.makeContextCurrent(window);
        window.dispose();
        internal.windowPlatform.destroyWindow(window);
        internal.windowPlatform.restoreMainContext();
    }
}