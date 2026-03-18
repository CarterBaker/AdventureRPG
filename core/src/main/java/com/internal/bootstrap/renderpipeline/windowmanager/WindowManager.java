package com.internal.bootstrap.renderpipeline.windowmanager;

import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WindowManager extends ManagerPackage {

    /*
     * Owns all windows — main and detached. Main window is registered during
     * bootstrap by EnginePackage after the bootstrap pipelines are created.
     * Window IDs are assigned sequentially — main is always 0, detached windows
     * increment from 1. The active window is set before each draw pass so
     * RenderSystem and CameraBufferSystem read from the correct window each frame.
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

    // Registration \\

    public void registerMainWindow(WindowInstance window) {
        this.mainWindow = window;
        this.activeWindow = window;
        windows.add(window);
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