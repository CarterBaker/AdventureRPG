package application.kernel.windowpipeline.windowmanager;

import application.kernel.windowpipeline.window.WindowData;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.ContextPackage;
import engine.root.EngineUtility;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WindowManager extends ManagerPackage {

    /*
     * Owns all engine windows. The main window is platform-hosted and registered
     * at bootstrap. Secondary windows are registered on demand and opened
     * immediately by the platform layer.
     */

    // Windows
    private ObjectArrayList<WindowInstance> windows;
    private WindowInstance mainWindow;
    private WindowInstance activeWindow;
    private WindowInstance renderWindow;
    private WindowInstance contextWindow;

    // Identity
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
        syncActiveWindow();

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

    private void syncActiveWindow() {
        WindowInstance focusedWindow = resolveFocusedWindow();

        if (focusedWindow == null) {
            boolean switchedToMain = activeWindow != mainWindow;
            activeWindow = mainWindow;

            if (switchedToMain && mainWindow != null && mainWindow.hasNativeHandle())
                internal.windowPlatform.makeContextCurrent(mainWindow);

            return;
        }

        if (activeWindow == focusedWindow)
            return;

        activeWindow = focusedWindow;
        internal.windowPlatform.makeContextCurrent(focusedWindow);
    }

    private WindowInstance resolveFocusedWindow() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            WindowInstance window = windows.get(i);
            if (window == mainWindow)
                continue;
            if (!window.hasNativeHandle())
                continue;
            if (!internal.windowPlatform.isWindowFocused(window))
                continue;
            return window;
        }

        if (mainWindow != null
                && mainWindow.hasNativeHandle()
                && internal.windowPlatform.isWindowFocused(mainWindow))
            return mainWindow;

        return null;
    }

    // Accessible \\

    public void registerMainWindow(WindowInstance window) {
        verifyWindowRegistration(window, true);
        this.mainWindow = window;
        this.activeWindow = window;
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

    private void registerWindow(WindowInstance window) {
        windows.add(window);
        internal.windowPlatform.openWindow(window);
    }

    public void removeWindow(WindowInstance window) {
        windows.remove(window);
        if (activeWindow == window)
            activeWindow = mainWindow;
    }

    public int issueWindowID() {
        return nextWindowID++;
    }

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

    public WindowInstance getMainWindow() {
        return mainWindow;
    }

    public WindowInstance getActiveWindow() {
        return activeWindow;
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

    // Validation \\

    private void verifyWindowRegistration(WindowInstance window, boolean isMain) {
        if (window == null)
            throwException("Cannot register null window.");
        int windowID = window.getWindowID();
        if (isMain && windowID != 0)
            throwException("Main window must use window ID 0. Received: " + windowID);
        if (!isMain && windowID == 0)
            throwException("Detached windows must not use window ID 0.");
        if (hasWindowID(windowID))
            throwException("Window ID already registered: " + windowID);
    }

    private boolean hasWindowID(int windowID) {
        for (int i = 0; i < windows.size(); i++) {
            if (windows.get(i).getWindowID() == windowID)
                return true;
        }
        return false;
    }
}