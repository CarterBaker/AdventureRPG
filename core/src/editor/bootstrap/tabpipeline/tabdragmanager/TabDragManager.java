package editor.bootstrap.tabpipeline.tabdragmanager;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.bootstrap.tabpipeline.util.DropTargetStruct;
import editor.bootstrap.tabpipeline.util.DropZone;
import editor.bootstrap.tabpipeline.util.TabDragLayoutStruct;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TabDragManager extends ManagerPackage {

    /*
     * Owns the full tab drag lifecycle. Never touches composite rects directly —
     * all positioning goes through TabContext.placeAt() so chrome and content
     * always move together.
     *
     * On latch: the handle is removed from the BSP so remaining tabs reflow.
     * Tab dimensions are cached before pushRects() overwrites them. Depth is
     * elevated on both windows so the dragged tab floats above everything.
     *
     * Each frame: updateDraggedTab() calls placeAt() with the cursor-tracked rect.
     * Chrome and content follow together in that single call.
     *
     * zoneGhost — half-panel drop preview on the target OS window. Repositioned
     * on zone change, rebuilt on OS window change, closed before drop.
     *
     * On drop:
     * Cross-window — tabManager.moveTabToOsWindow() reparents both windows.
     * Into leaf — addTabToLeaf on the resolved BSP leaf.
     * Void — tabManager.openSecondaryWindowForTab() into a new window.
     * pushRects() settles all positions. clearState() resets drag fields.
     */

    // Constants
    private float dropZoneEdgeFraction;
    private String menuTabGhost;

    // Internal
    private WindowManager windowManager;
    private MenuManager menuManager;
    private FboManager fboManager;
    private TabManager tabManager;
    private InputManager inputManager;
    private TabDragLayoutStruct layoutSystem;

    // Drag state
    private TabHandle draggedHandle;
    private float grabOffsetX;
    private float grabOffsetY;
    private float dragW;
    private float dragH;

    // Zone ghost
    private MenuInstance zoneGhost;
    private WindowInstance zoneGhostWindow;
    private FboInstance zoneGhostFbo;

    // Drop resolution
    private DropTargetStruct lastDropTarget;

    // Internal \\

    @Override
    protected void create() {
        this.layoutSystem = new TabDragLayoutStruct();
        this.dropZoneEdgeFraction = EngineSetting.TAB_DRAG_EDGE_FRACTION;
        this.menuTabGhost = EngineSetting.MENU_TAB_GHOST;
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FboManager.class);
        this.tabManager = get(TabManager.class);
        this.inputManager = get(InputManager.class);
    }

    @Override
    protected void update() {

        if (draggedHandle == null)
            return;

        if (EngineContext.input.isMouseReleased(0)) {
            executeDrop();
            return;
        }

        WindowInstance sourceOsWindow = resolveOsWindow(draggedHandle.getTabContext().getWindow());
        float globalX = inputManager.getGlobalMouseX();
        float globalY = inputManager.getGlobalMouseY();
        float screenX = globalX + sourceOsWindow.getScreenX();
        float screenY = globalY + sourceOsWindow.getScreenY();

        updateDraggedTab(globalX, globalY);

        DropTargetStruct target = resolveDropTarget(screenX, screenY);
        updateZoneGhost(target);

        if (target != null)
            lastDropTarget = target;
        else if (!isCursorOverAnyOsWindow(screenX, screenY))
            lastDropTarget = null;
    }

    // Entry Point \\

    public void onTabDragUpdate(WindowInstance sourceWindow) {

        if (draggedHandle != null)
            return;

        if (EngineContext.input.isMouseReleased(0))
            return;

        latchDrag(sourceWindow);
    }

    // Drag Start \\

    private void latchDrag(WindowInstance sourceWindow) {

        TabHandle handle = tabManager.getTabHandleForWindow(sourceWindow);

        if (handle == null)
            return;

        WindowInstance tabWindow = handle.getTabContext().getWindow();

        float globalX = inputManager.getGlobalMouseX();
        float globalY = inputManager.getGlobalMouseY();
        grabOffsetX = 0f;
        grabOffsetY = 0f;

        dragW = EngineSetting.TAB_DRAG_PREVIEW_W;
        dragH = EngineSetting.TAB_DRAG_PREVIEW_H;

        draggedHandle = handle;

        // Elevate above zone ghost for the duration of drag
        tabWindow.setDepth(EngineSetting.TAB_DRAG_TAB_DEPTH);
        handle.getTabContext().getContentContext().getWindow()
                .setDepth(EngineSetting.TAB_DRAG_CONTENT_DEPTH);

        WindowInstance osWindow = resolveOsWindow(tabWindow);
        tabManager.getDockLayoutSystem().removeTab(osWindow, handle);
        tabManager.pushRects();

        updateDraggedTab(globalX, globalY);
    }

    // Drag Tracking \\

    /*
     * Positions the dragged tab each frame via placeAt() so chrome and content
     * always travel together. This is the only place drag-frame positioning
     * happens.
     */
    private void updateDraggedTab(float globalX, float globalY) {

        float x = globalX - grabOffsetX;
        float y = globalY - grabOffsetY;

        draggedHandle.getTabContext().placeAt(x, y, dragW, dragH);
    }

    // Zone Ghost \\

    private void updateZoneGhost(DropTargetStruct target) {

        if (target != null && target.matches(lastDropTarget))
            return;

        if (target != null
                && lastDropTarget != null
                && target.getWindow() == lastDropTarget.getWindow()
                && zoneGhostWindow != null) {
            repositionZoneGhost(target);
            return;
        }

        closeZoneGhost();

        if (target == null)
            return;

        openZoneGhost(target);
    }

    private void openZoneGhost(DropTargetStruct target) {

        DockNodeStruct leaf = target.getLeaf();
        DropZone zone = target.getZone();
        WindowInstance targetOsWindow = resolveOsWindow(target.getWindow());

        float ghostX = layoutSystem.zoneX(leaf.getX(), leaf.getW(), zone);
        float ghostY = layoutSystem.zoneY(leaf.getY(), leaf.getH(), zone);
        float ghostW = layoutSystem.zoneW(leaf.getW(), zone);
        float ghostH = layoutSystem.zoneH(leaf.getH(), zone);

        zoneGhostWindow = windowManager.createLogicalWindow(
                EngineSetting.TAB_ZONE_GHOST_WINDOW_TITLE, targetOsWindow);
        zoneGhostWindow.setDepth(EngineSetting.TAB_DRAG_GHOST_DEPTH);
        zoneGhostWindow.setCaptureEligible(false);
        zoneGhostWindow.setFocusIndependent(true);

        zoneGhostWindow.setCompositeRect(ghostX, ghostY, ghostW, ghostH);
        zoneGhostWindow.resize((int) ghostW, (int) ghostH);

        zoneGhostFbo = fboManager.cloneFbo(
                application.runtime.RuntimeSetting.FBO_UI, zoneGhostWindow);
        menuManager.setMenuTargetFbo(zoneGhostWindow, zoneGhostFbo);

        zoneGhost = menuManager.openMenu(menuTabGhost, zoneGhostWindow);
    }

    private void repositionZoneGhost(DropTargetStruct target) {

        DockNodeStruct leaf = target.getLeaf();
        DropZone zone = target.getZone();

        float ghostX = layoutSystem.zoneX(leaf.getX(), leaf.getW(), zone);
        float ghostY = layoutSystem.zoneY(leaf.getY(), leaf.getH(), zone);
        float ghostW = layoutSystem.zoneW(leaf.getW(), zone);
        float ghostH = layoutSystem.zoneH(leaf.getH(), zone);

        zoneGhostWindow.setCompositeRect(ghostX, ghostY, ghostW, ghostH);
        zoneGhostWindow.resize((int) ghostW, (int) ghostH);
        zoneGhostFbo.resize((int) ghostW, (int) ghostH);
    }

    private void closeZoneGhost() {

        if (zoneGhost != null) {
            menuManager.closeMenu(zoneGhost);
            zoneGhost = null;
        }

        if (zoneGhostWindow != null) {
            menuManager.setMenuTargetFbo(zoneGhostWindow, null);
            windowManager.removeWindow(zoneGhostWindow);
            zoneGhostWindow = null;
        }

        zoneGhostFbo = null;
    }

    // Drop Resolution \\

    private DropTargetStruct resolveDropTarget(float globalX, float globalY) {

        ObjectArrayList<WindowInstance> windows = windowManager.getWindows();
        Object[] elements = windows.elements();
        int size = windows.size();

        WindowInstance bestWindow = null;
        long bestArea = Long.MAX_VALUE;
        int bestDepth = Integer.MIN_VALUE;

        for (int i = 0; i < size; i++) {

            WindowInstance w = (WindowInstance) elements[i];

            if (!w.hasNativeHandle())
                continue;

            if (!isGlobalPointInOsWindow(w, globalX, globalY))
                continue;

            long area = (long) w.getWidth() * (long) w.getHeight();

            if (area < bestArea || (area == bestArea && w.getDepth() > bestDepth)) {
                bestArea = area;
                bestDepth = w.getDepth();
                bestWindow = w;
            }
        }

        if (bestWindow == null)
            return null;

        float localX = inputManager.getCursorXForWindow(bestWindow);
        float localY = inputManager.getCursorYForWindow(bestWindow);

        DockNodeStruct leaf = tabManager.getDockLayoutSystem()
                .findLeafAt(bestWindow, localX, localY);

        if (leaf == null)
            return null;

        DropZone zone = classifyZone(leaf, localX, localY);

        return new DropTargetStruct(bestWindow, leaf, zone);
    }

    private boolean isGlobalPointInOsWindow(WindowInstance w, float globalX, float globalY) {
        return globalX >= w.getScreenX()
                && globalX < w.getScreenX() + w.getWidth()
                && globalY >= w.getScreenY()
                && globalY < w.getScreenY() + w.getHeight();
    }

    private boolean isCursorOverAnyOsWindow(float globalX, float globalY) {

        ObjectArrayList<WindowInstance> windows = windowManager.getWindows();
        Object[] elements = windows.elements();
        int size = windows.size();

        for (int i = 0; i < size; i++) {
            WindowInstance w = (WindowInstance) elements[i];
            if (!w.hasNativeHandle())
                continue;
            if (isGlobalPointInOsWindow(w, globalX, globalY))
                return true;
        }

        return false;
    }

    private DropZone classifyZone(DockNodeStruct leaf, float localX, float localY) {

        float lx = leaf.getX();
        float ly = leaf.getY();
        float lw = leaf.getW();
        float lh = leaf.getH();

        float relX = (localX - lx) / lw;
        float relY = (localY - ly) / lh;

        float edge = dropZoneEdgeFraction;

        if (relX < edge)
            return DropZone.LEFT;
        if (relX > 1f - edge)
            return DropZone.RIGHT;
        if (relY < edge)
            return DropZone.TOP;
        if (relY > 1f - edge)
            return DropZone.BOTTOM;

        float distLeft = relX;
        float distRight = 1f - relX;
        float distTop = relY;
        float distBottom = 1f - relY;

        float minH = Math.min(distLeft, distRight);
        float minV = Math.min(distTop, distBottom);

        if (minH <= minV)
            return distLeft < distRight ? DropZone.LEFT : DropZone.RIGHT;

        return distTop < distBottom ? DropZone.TOP : DropZone.BOTTOM;
    }

    // Drop Execution \\

    private void executeDrop() {

        if (draggedHandle == null) {
            clearState();
            return;
        }

        // Reset depths before any rect propagation so the compositor never
        // sees a drop frame with drag-elevated windows.
        draggedHandle.getTabContext().getWindow()
                .setDepth(EngineSetting.TAB_DEFAULT_TAB_DEPTH);
        draggedHandle.getTabContext().getContentContext().getWindow()
                .setDepth(EngineSetting.TAB_DEFAULT_CONTENT_DEPTH);

        DropTargetStruct target = lastDropTarget;
        WindowInstance sourceOsWindow = resolveOsWindow(
                draggedHandle.getTabContext().getWindow());

        if (target != null) {

            WindowInstance targetOsWindow = target.getWindow();

            if (targetOsWindow != sourceOsWindow)
                tabManager.moveTabToOsWindow(draggedHandle, targetOsWindow);

            tabManager.getDockLayoutSystem().addTabToLeaf(
                    target.getLeaf(),
                    draggedHandle, target.getZone());
        }

        else
            tabManager.openSecondaryWindowForTab(draggedHandle);

        if (sourceOsWindow != windowManager.getMainWindow()
                && isSourceOsWindowEmpty(sourceOsWindow))
            tabManager.closeOsWindow(sourceOsWindow);

        tabManager.pushRects();
        clearState();
    }

    private boolean isSourceOsWindowEmpty(WindowInstance osWindow) {

        ObjectArrayList<TabHandle> openTabs = tabManager.getOpenTabs();
        Object[] elements = openTabs.elements();
        int size = openTabs.size();

        for (int i = 0; i < size; i++) {

            TabHandle handle = (TabHandle) elements[i];

            if (handle == draggedHandle)
                continue;

            if (handle.getTabContext().getWindow().getCompositeTarget() == osWindow)
                return false;
        }

        return true;
    }

    // State Cleanup \\

    private void clearState() {

        closeZoneGhost();
        windowManager.unlockHoveredWindow();

        if (draggedHandle != null) {
            draggedHandle.getTabContext().getWindow()
                    .setDepth(EngineSetting.TAB_DEFAULT_TAB_DEPTH);
            draggedHandle.getTabContext().getContentContext().getWindow()
                    .setDepth(EngineSetting.TAB_DEFAULT_CONTENT_DEPTH);
        }

        draggedHandle = null;
        lastDropTarget = null;
        grabOffsetX = 0f;
        grabOffsetY = 0f;
        dragW = 0f;
        dragH = 0f;
    }

    // Utility \\

    private WindowInstance resolveOsWindow(WindowInstance window) {

        if (window.hasNativeHandle())
            return window;

        WindowInstance composite = window.getCompositeTarget();

        if (composite != null && composite.hasNativeHandle())
            return composite;

        return windowManager.getMainWindow();
    }

    // Accessible \\

    public boolean isDragging() {
        return draggedHandle != null;
    }
}