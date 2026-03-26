package com.internal.bootstrap.renderpipeline.windowmanager;

import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WindowManager extends ManagerPackage {

    /*
     * Owns all windows — main and detached. Main window is registered during
     * bootstrap by EnginePackage after the bootstrap pipelines are created.
     * Window IDs are assigned sequentially — main is always 0, detached windows
     * increment from 1. Detached windows are opened as real OS windows via
     * internal.windowPlatform on registration.
     *
     * activeWindow tracks the last focused or clicked OS window. This is
     * exclusively for input and raycast systems — it has no relation to
     * rendering. Render calls are routed to windows explicitly.
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
        System.out.println(
                "[EditorDiag][WindowManager.registerDetachedWindow] frame=" + internal.getFrameCount()
                        + " thread=" + Thread.currentThread().getName()
                        + " windowID=" + window.getWindowID()
                        + " pendingCount=" + pendingWindowOpen.size());
    }

    @Override
    protected void update() {

        if (pendingWindowOpen.isEmpty())
            return;

        for (int i = 0; i < pendingWindowOpen.size(); i++) {
            WindowInstance pendingWindow = pendingWindowOpen.get(i);
            System.out.println(
                    "[EditorDiag][WindowManager.update.openWindow] frame=" + internal.getFrameCount()
                            + " thread=" + Thread.currentThread().getName()
                            + " windowID=" + pendingWindow.getWindowID()
                            + " size=" + pendingWindow.getWidth() + "x" + pendingWindow.getHeight());
            internal.windowPlatform.openWindow(pendingWindow);
        }

        pendingWindowOpen.clear();
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
