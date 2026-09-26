package application.kernel.windowpipeline.windowmanager;

import application.kernel.windowpipeline.window.WindowData;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.input.Input;
import engine.root.ContextPackage;
import engine.root.EngineUtility;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WindowManager extends ManagerPackage {

    /*
     * Owns every engine window and rebuilds the hovered window list each frame,
     * sorted by zOrder then area, locating logical windows through their OS
     * window's own input. Focus changes only through setFocusedWindow() from
     * InputManager.
     */

    private ObjectArrayList<WindowInstance> windows;
    private final ObjectArrayList<WindowInstance> hoveredWindows = new ObjectArrayList<>();
    private WindowInstance mainWindow;
    private WindowInstance focusedWindow;
    private WindowInstance renderWindow;
    private WindowInstance contextWindow;
    private WindowInstance capturedWindow;

    private int nextWindowID;
    private int nextZOrder = 1;

    private final float[] cursorPosScratch = new float[2];

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

        // A destroyed OS window takes every window composited onto it along,
        // so the index is clamped back into range after each teardown.
        for (int i = windows.size() - 1; i >= 0; i--) {

            WindowInstance window = windows.get(i);

            if (!internal.windowPlatform.shouldClose(window))
                continue;

            destroyOsWindow(window);
            i = Math.min(i, windows.size());
        }
    }

    @Override
    protected void dispose() {

        ObjectArrayList<WindowInstance> snapshot = new ObjectArrayList<>(windows);

        for (int i = snapshot.size() - 1; i >= 0; i--)
            destroyOsWindow(snapshot.get(i));
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

            internal.windowPlatform.getCursorPos(w, cursorPosScratch);
            internal.windowPlatform.syncInputForWindow(w);

            float lx = cursorPosScratch[0];
            float ly = cursorPosScratch[1];

            if (lx < 0 || lx >= w.getWidth() || ly < 0 || ly >= w.getHeight())
                continue;

            hoveredWindows.add(w);

            Input windowInput = internal.windowPlatform.getInputForWindow(w);
            float mx = windowInput.getMouseX();
            float my = windowInput.getMouseY();

            for (int j = 0; j < windows.size(); j++) {
                WindowInstance logical = windows.get(j);
                if (logical.hasNativeHandle())
                    continue;
                if (!logical.hasCompositeRect())
                    continue;
                if (logical.getCompositeTarget() != w)
                    continue;
                if (logical.acceptsInputAt(mx, my))
                    hoveredWindows.add(logical);
            }
        }

        hoveredWindows.sort((a, b) -> {
            int zCmp = b.getZOrder() - a.getZOrder();
            if (zCmp != 0)
                return zCmp;
            return Long.compare(effectiveArea(a), effectiveArea(b));
        });
    }

    private long effectiveArea(WindowInstance w) {
        return w.hasNativeHandle()
                ? (long) w.getWidth() * w.getHeight()
                : (long) w.getCompositeW() * (long) w.getCompositeH();
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

    // Z-Order \\

    public void bringToFront(WindowInstance window) {
        window.setZOrder(nextZOrder++);
    }

    public boolean isFrontmost(WindowInstance window) {

        WindowInstance glWindow = window.getGLWindow();

        for (int i = 0; i < windows.size(); i++) {
            WindowInstance other = windows.get(i);
            if (other.getGLWindow() == glWindow && other.getZOrder() > window.getZOrder())
                return false;
        }

        return true;
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

    public WindowInstance getHoveredWindow() {
        return hoveredWindows.isEmpty() ? null : hoveredWindows.get(0);
    }

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
        WindowInstance previousGLWindow = window.getGLWindow();
        window.setCompositeTarget(newParent);
        window.migrateRenderResources(previousGLWindow);
    }

    public void placeOsWindow(WindowInstance window, int screenX, int screenY, int width, int height) {
        if (window == null || !window.hasNativeHandle())
            throwException("Cannot place a window that has no OS window.");
        internal.windowPlatform.placeWindow(window, screenX, screenY, width, height);
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