package program.core.kernel.windowmanager;

import program.core.engine.ManagerPackage;
import program.core.kernel.window.WindowInstance;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WindowManager extends ManagerPackage {
    /*
     * Owns all engine windows. The main window is platform-hosted and registered
     * at bootstrap. Secondary windows are registered on demand and opened
     * immediately
     * by the platform layer.
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
    protected void update() {
        syncActiveWindow();

        // Close Detection
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
            if (window == mainWindow)
                continue;
            internal.windowPlatform.makeContextCurrent(window);
            window.dispose();
            internal.windowPlatform.destroyWindow(window);
        }
    }

    private void syncActiveWindow() {
        for (int i = 0; i < windows.size(); i++) {
            WindowInstance window = windows.get(i);

            if (!window.hasNativeHandle())
                continue;

            if (!internal.windowPlatform.isWindowFocused(window))
                continue;

            activeWindow = window;
            return;
        }

        activeWindow = mainWindow;
    }

    // Accessible \\
    public void registerMainWindow(WindowInstance window) {
        verifyWindowRegistration(window, true);
        this.mainWindow = window;
        this.activeWindow = window;
        windows.add(window);
        internal.windowPlatform.openWindow(window);
    }

    public void registerDetachedWindow(WindowInstance window) {
        verifyWindowRegistration(window, false);
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