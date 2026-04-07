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
        this.mainWindow = window;
        this.activeWindow = window;
        windows.add(window);
        internal.windowPlatform.openWindow(window);
    }

    public void registerDetachedWindow(WindowInstance window) {
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

    public WindowInstance getMainWindow() {
        return mainWindow;
    }

    public WindowInstance getActiveWindow() {
        return activeWindow;
    }

    public ObjectArrayList<WindowInstance> getWindows() {
        return windows;
    }

    public boolean hasMainWindow() {
        return mainWindow != null;
    }
}