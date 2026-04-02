package program.bootstrap.renderpipeline.windowmanager;

import program.bootstrap.renderpipeline.window.WindowInstance;
import program.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WindowManager extends ManagerPackage {

    /*
     * Owns all engine windows. The main window is platform-hosted and registered
     * at bootstrap. Secondary windows are registered on demand and opened by the
     * platform layer on the next update.
     */

    // Windows
    private ObjectArrayList<WindowInstance> windows;
    private ObjectArrayList<WindowInstance> pendingOpen;
    private WindowInstance mainWindow;
    private WindowInstance activeWindow;

    // Identity
    private int nextWindowID;

    // Internal \\

    @Override
    protected void create() {
        this.windows = new ObjectArrayList<>();
        this.pendingOpen = new ObjectArrayList<>();
        this.nextWindowID = 1;
    }

    @Override
    protected void update() {

        // Pending Open
        if (!pendingOpen.isEmpty()) {
            for (int i = 0; i < pendingOpen.size(); i++)
                internal.windowPlatform.openWindow(pendingOpen.get(i));
            pendingOpen.clear();
        }

        // Close Detection
        for (int i = windows.size() - 1; i >= 0; i--) {
            WindowInstance window = windows.get(i);

            if (window == mainWindow)
                continue;
            if (!window.hasNativeHandle())
                continue;
            if (!internal.windowPlatform.shouldClose(window))
                continue;

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

            window.dispose();
            internal.windowPlatform.destroyWindow(window);
        }
    }

    // Accessible \\

    public void registerMainWindow(WindowInstance window) {
        this.mainWindow = window;
        this.activeWindow = window;
        windows.add(window);
        pendingOpen.add(window);
    }

    public void registerDetachedWindow(WindowInstance window) {
        windows.add(window);
        pendingOpen.add(window);
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

    public void setActiveWindow(WindowInstance window) {
        this.activeWindow = window;
    }

    public ObjectArrayList<WindowInstance> getWindows() {
        return windows;
    }

    public boolean hasMainWindow() {
        return mainWindow != null;
    }
}