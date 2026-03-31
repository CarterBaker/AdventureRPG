package com.internal.bootstrap.renderpipeline.windowmanager;

import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WindowManager extends ManagerPackage {

    /*
     * Owns all windows. Main window is LibGDX-hosted.
     * Detached windows are opened by platform layer and drawn by engine.
     */

    // Windows
    private ObjectArrayList<WindowInstance> windows;
    private ObjectArrayList<WindowInstance> pendingWindowOpen;
    private WindowInstance mainWindow;
    private WindowInstance activeWindow;

    // Identity
    private int nextWindowID;

    // Internal \\

    @Override
    protected void create() {
        this.windows = new ObjectArrayList<>();
        this.pendingWindowOpen = new ObjectArrayList<>();
        this.nextWindowID = 1;
    }

    // Registration \\

    public void registerMainWindow(WindowInstance window) {
        this.mainWindow = window;
        this.activeWindow = window;
        windows.add(window);
    }

    public void registerDetachedWindow(WindowInstance window) {
        windows.add(window);
        pendingWindowOpen.add(window);
    }

    @Override
    protected void update() {

        // Open newly registered detached windows.
        if (!pendingWindowOpen.isEmpty()) {
            for (int i = 0; i < pendingWindowOpen.size(); i++)
                internal.windowPlatform.openWindow(pendingWindowOpen.get(i));
            pendingWindowOpen.clear();
        }

        // Poll close requests for detached windows.
        // Iterate backwards to allow removals.
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

    public void removeWindow(WindowInstance window) {
        windows.remove(window);
        if (activeWindow == window)
            activeWindow = mainWindow;
    }

    // Accessible \\

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

    public int issueWindowID() {
        return nextWindowID++;
    }
}