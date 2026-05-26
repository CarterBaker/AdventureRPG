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
     * Hover resolution is two-phase:
     *
     * Phase 1 — OS window selection.
     * Each OS window is queried via the platform for its local cursor position
     * using glfwGetCursorPos, which is a direct OS call that returns correct
     * coordinates regardless of which window has OS focus or which GLFW cursor
     * callbacks have fired. A bounds check in raw Y-down space is sufficient to
     * determine containment — the valid range [0, height) is identical in both
     * Y-down and Y-up. Smallest area wins when OS windows overlap.
     * syncInputForWindow is called on the resolved OS window immediately so
     * EngineContext.input reflects that window's coordinate space for phase 2
     * and all downstream systems this frame.
     *
     * Phase 2 — logical window selection.
     * EngineContext.input.getMouseX/Y() is now correct for the resolved OS
     * window — the cursor callbacks on each Lwjgl3Input fire on cursor movement
     * regardless of OS focus, so those values are always current and in the
     * correct Y-up window-local space. Composite rects are expressed in that
     * same space. The search is scoped to logical windows whose composite target
     * is the resolved OS window. Tightest rect wins; depth breaks ties. Falls
     * back to the OS window itself when no logical child is hit.
     *
     * The original single-pass approach failed for secondary windows because
     * EngineContext.input always reflected the main window before
     * syncInputForWindow was called — secondary logical windows have composite
     * rects in their own OS window's local space so the hit test always failed
     * for them. Phase 1 resolves the correct OS window first so the sync
     * precedes the test.
     *
     * hoveredWindowLocked prevents syncHoveredWindow from reassigning
     * hoveredWindow while an element hover is active. Lock is set by
     * ElementHitSystem on hover entry and released on hover exit. While locked,
     * syncInputForWindow is still called on the current hoveredWindow so cursor
     * coordinates stay current for hover-exit testing — GLFW cursor callbacks
     * only fire on the OS-focused window, so without this refresh the coords
     * stale out when a secondary window has OS focus and hover-exit never fires.
     *
     * focusedWindow is the window that currently owns input. It is set by
     * clicking on a window and is the single authority for input routing.
     * Cursor capture follows focus — the kernel InputSystem drives capture
     * transitions whenever focus changes or menu lock state changes on the
     * focused window.
     *
     * capturedWindow pins hoveredWindow when cursor capture is active. While
     * set, phase 1 and 2 are skipped — the captured window is always considered
     * hovered. syncInputForWindow is still called on the captured window's GL
     * window so EngineContext.input is actively maintained every frame rather
     * than relying on whatever the last resolved OS window happened to be.
     * Released when capture ends, restoring normal hover resolution.
     */

    private ObjectArrayList<WindowInstance> windows;
    private WindowInstance mainWindow;
    private WindowInstance hoveredWindow;
    private WindowInstance focusedWindow;
    private WindowInstance renderWindow;
    private WindowInstance contextWindow;
    private WindowInstance capturedWindow;

    private boolean hoveredWindowLocked;

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

        if (hoveredWindowLocked) {
            // Cursor coordinates must stay current even while locked so
            // hover-exit testing in ElementHitSystem can fire correctly.
            // GLFW cursor callbacks only deliver to the OS-focused window, so
            // without this refresh the hovered window's Lwjgl3Input stales out
            // whenever a secondary window holds OS focus, and hover exit never
            // fires. getGLWindow() is required because hoveredWindow may be a
            // logical window with no native handle — syncInputForWindow would
            // be a no-op on it and the cursor would never refresh.
            if (hoveredWindow != null)
                internal.windowPlatform.syncInputForWindow(hoveredWindow.getGLWindow());
            return;
        }

        if (capturedWindow != null) {
            hoveredWindow = capturedWindow;
            // EngineContext.input must be actively maintained even while captured
            // so it is always correct when InputManager reads it. Without this,
            // input context reflects whatever the last resolved OS window was
            // rather than the window that actually owns capture.
            internal.windowPlatform.syncInputForWindow(capturedWindow.getGLWindow());
            return;
        }

        // Phase 1 — find the OS window the cursor is physically inside via a
        // direct glfwGetCursorPos query per window. EngineContext.input is not
        // used here — it always reflects the most recently synced window (the
        // main window before this method runs) and cannot detect secondary OS
        // windows. syncInputForWindow is called on the resolved OS window before
        // phase 2 so EngineContext.input is correct for the hit test below.
        WindowInstance osWindow = resolveHoveredOsWindow();

        if (osWindow == null) {
            hoveredWindow = null;
            return;
        }

        internal.windowPlatform.syncInputForWindow(osWindow);

        // Phase 2 — find the tightest logical window compositing to the resolved
        // OS window. EngineContext.input now reflects that window's coordinate
        // space so getMouseX/Y() matches the Y-up local space of composite rects.
        float mx = EngineContext.input.getMouseX();
        float my = EngineContext.input.getMouseY();

        WindowInstance topHit = null;
        long topArea = Long.MAX_VALUE;
        int topDepth = Integer.MIN_VALUE;

        for (int i = 0; i < windows.size(); i++) {

            WindowInstance w = windows.get(i);

            if (!w.hasCompositeRect())
                continue;

            if (w.getCompositeTarget() != osWindow)
                continue;

            if (!isMouseOver(w, mx, my))
                continue;

            long area = (long) w.getCompositeW() * (long) w.getCompositeH();

            if (area < topArea || (area == topArea && w.getDepth() > topDepth)) {
                topArea = area;
                topDepth = w.getDepth();
                topHit = w;
            }
        }

        // Fall back to the OS window itself when no logical child is hit.
        hoveredWindow = topHit != null ? topHit : osWindow;
    }

    /*
     * Finds the OS window whose local cursor position falls within its client
     * area using a direct glfwGetCursorPos query per window. The bounds check
     * uses raw Y-down coords from the platform — the valid range [0, height)
     * is the same in both Y-down and Y-up so no conversion is needed here.
     * Smallest area wins when OS windows overlap. getCursorPos is used instead
     * of separate getCursorX/getCursorY calls to avoid a redundant platform
     * round-trip per window per frame.
     */
    private WindowInstance resolveHoveredOsWindow() {

        WindowInstance best = null;
        long bestArea = Long.MAX_VALUE;

        for (int i = 0; i < windows.size(); i++) {

            WindowInstance w = windows.get(i);

            if (!w.hasNativeHandle())
                continue;

            internal.windowPlatform.getCursorPos(w, cursorPosScratch);
            float lx = cursorPosScratch[0];
            float ly = cursorPosScratch[1];

            if (lx < 0 || lx >= w.getWidth() || ly < 0 || ly >= w.getHeight())
                continue;

            long area = (long) w.getWidth() * (long) w.getHeight();

            if (area < bestArea) {
                bestArea = area;
                best = w;
            }
        }

        return best;
    }

    private boolean isMouseOver(WindowInstance w, float mx, float my) {
        return mx >= w.getCompositeX()
                && mx < w.getCompositeX() + w.getCompositeW()
                && my >= w.getCompositeY()
                && my < w.getCompositeY() + w.getCompositeH();
    }

    // Registration \\

    public void registerMainWindow(WindowInstance window) {
        verifyWindowRegistration(window, true);
        this.mainWindow = window;
        this.hoveredWindow = window;
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
        if (capturedWindow == window)
            capturedWindow = null;
        if (hoveredWindow == window)
            hoveredWindow = null;
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

    public WindowInstance getFocusedWindow() {
        return focusedWindow;
    }

    public void setFocusedWindow(WindowInstance window) {
        this.focusedWindow = window;
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
}