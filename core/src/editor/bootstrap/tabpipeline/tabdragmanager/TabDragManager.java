package editor.bootstrap.tabpipeline.tabdragmanager;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
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
     * Owns the full tab drag lifecycle. Called on the first drag frame by
     * TabBranch via onTabDragUpdate to latch the source tab. All per-frame
     * work after latch — ghost position, drop target resolution, release
     * detection — runs in update() so the drag is never interrupted by hover
     * state changes. Once draggedHandle is set, the drag continues until
     * isMouseReleased(0) regardless of which window the cursor is over.
     *
     * onTabDragUpdate is kept minimal: it latches the handle on the first
     * call and does nothing else. Subsequent on_drag callbacks are ignored
     * because update() owns the active drag loop.
     *
     * Two ghost menus are managed:
     * dragGhost — mimics the dragged tab, follows the cursor, lives on the
     * main window at TAB_DRAG_GHOST_DEPTH. Always paints above
     * the zone ghost.
     * zoneGhost — half-panel drop preview on the target OS window, one depth
     * below the drag ghost so the cursor ghost always reads on top.
     * Opened and closed as the resolved drop zone changes each frame.
     * Closed immediately on drop — it is a preview only.
     *
     * Ghost menus are plain visuals — TabGhost has no canvas_area, no close
     * button, and no interactive elements. Ghost windows are flagged
     * captureEligible=false and focusIndependent=true so they do not affect
     * focus or cursor capture, but they still participate in hover detection.
     * This is why all active drag logic lives in update() rather than relying
     * on on_drag callbacks from a specific element.
     *
     * Drop resolution walks all OS windows each frame by native bounds to find
     * which one the raw screen cursor is inside, then delegates to
     * DockLayoutSystem.findLeafAt() to get the leaf, then classifies the
     * cursor position within that leaf into a DropZone. The center region
     * (inner 50%) resolves to the nearest edge zone so every pixel of the
     * leaf has a defined destination.
     *
     * On release:
     * 1. Remove handle from source BSP. Auto-close source OS window if empty
     * and not the main window.
     * 2a. If drop target exists — addTabToLeaf on the target leaf.
     * 2b. If no drop target — open a new secondary OS window for the handle.
     * 3. pushRects() propagates all composite rects.
     *
     * grabOffsetX/Y is the cursor position within the dragged tab window at
     * latch time so the ghost does not snap to the cursor corner on pickup.
     */

    // Constants
    private float dropZoneEdgeFraction;
    private String menuTabGhost;

    // Internal
    private WindowManager windowManager;
    private MenuManager menuManager;
    private FboManager fboManager;
    private TabManager tabManager;
    private TabDragLayoutStruct layoutSystem;

    // Drag state
    private TabHandle draggedHandle;
    private float grabOffsetX;
    private float grabOffsetY;

    // Drag ghost — follows cursor on main window
    private MenuInstance dragGhost;
    private WindowInstance dragGhostWindow;

    // Zone ghost — preview on target window, one depth below drag ghost
    private MenuInstance zoneGhost;
    private WindowInstance zoneGhostWindow;

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
    }

    @Override
    protected void update() {

        if (draggedHandle == null)
            return;

        if (EngineContext.input.isMouseReleased(0)) {
            executeDrop();
            return;
        }

        float screenX = EngineContext.input.getMouseX();
        float screenY = EngineContext.input.getMouseY();

        updateDragGhost(screenX, screenY);

        DropTargetStruct target = resolveDropTarget(screenX, screenY);
        updateZoneGhost(target);

        lastDropTarget = target;
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

        draggedHandle = handle;

        float screenX = EngineContext.input.getMouseX();
        float screenY = EngineContext.input.getMouseY();
        grabOffsetX = screenX - sourceWindow.getCompositeX();
        grabOffsetY = screenY - sourceWindow.getCompositeY();

        openDragGhost(sourceWindow);
    }

    // Drag Ghost \\

    private void openDragGhost(WindowInstance sourceWindow) {

        WindowInstance mainWindow = windowManager.getMainWindow();

        dragGhostWindow = windowManager.createLogicalWindow(
                EngineSetting.TAB_DRAG_GHOST_WINDOW_TITLE, mainWindow);
        dragGhostWindow.setDepth(EngineSetting.TAB_DRAG_GHOST_DEPTH);
        dragGhostWindow.setCaptureEligible(false);
        dragGhostWindow.setFocusIndependent(true);

        float w = sourceWindow.getCompositeW();
        float h = sourceWindow.getCompositeH();
        float screenX = EngineContext.input.getMouseX();
        float screenY = EngineContext.input.getMouseY();

        dragGhostWindow.setCompositeRect(
                screenX - grabOffsetX,
                screenY - grabOffsetY,
                w, h);
        dragGhostWindow.resize((int) w, (int) h);

        FboInstance fbo = fboManager.cloneFbo(
                application.runtime.RuntimeSetting.FBO_UI, dragGhostWindow);
        menuManager.setMenuTargetFbo(dragGhostWindow, fbo);

        dragGhost = menuManager.openMenu(menuTabGhost, dragGhostWindow);
    }

    private void updateDragGhost(float screenX, float screenY) {

        if (dragGhostWindow == null)
            return;

        float x = screenX - grabOffsetX;
        float y = screenY - grabOffsetY;
        float w = dragGhostWindow.getCompositeW();
        float h = dragGhostWindow.getCompositeH();

        dragGhostWindow.setCompositeRect(x, y, w, h);
    }

    // Zone Ghost \\

    private void updateZoneGhost(DropTargetStruct target) {

        if (target != null && target.matches(lastDropTarget))
            return;

        closeZoneGhost();

        if (target == null)
            return;

        openZoneGhost(target);
    }

    private void openZoneGhost(DropTargetStruct target) {

        DockNodeStruct leaf = target.getLeaf();
        DropZone zone = target.getZone();
        WindowInstance targetOsWindow = resolveOsWindow(target.getWindow());

        float leafX = leaf.getX();
        float leafY = leaf.getY();
        float leafW = leaf.getW();
        float leafH = leaf.getH();

        float ghostX = layoutSystem.zoneX(leafX, leafW, zone);
        float ghostY = layoutSystem.zoneY(leafY, leafH, zone);
        float ghostW = layoutSystem.zoneW(leafW, zone);
        float ghostH = layoutSystem.zoneH(leafH, zone);

        float osX = targetOsWindow.hasCompositeRect()
                ? targetOsWindow.getCompositeX()
                : 0f;
        float osY = targetOsWindow.hasCompositeRect()
                ? targetOsWindow.getCompositeY()
                : 0f;

        zoneGhostWindow = windowManager.createLogicalWindow(
                EngineSetting.TAB_ZONE_GHOST_WINDOW_TITLE, targetOsWindow);
        zoneGhostWindow.setDepth(EngineSetting.TAB_DRAG_GHOST_DEPTH - 1);
        zoneGhostWindow.setCaptureEligible(false);
        zoneGhostWindow.setFocusIndependent(true);

        zoneGhostWindow.setCompositeRect(ghostX, ghostY, ghostW, ghostH);
        zoneGhostWindow.resize((int) ghostW, (int) ghostH);

        FboInstance fbo = fboManager.cloneFbo(
                application.runtime.RuntimeSetting.FBO_UI, zoneGhostWindow);
        menuManager.setMenuTargetFbo(zoneGhostWindow, fbo);

        zoneGhost = menuManager.openMenu(menuTabGhost, zoneGhostWindow);
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
    }

    // Drop Resolution \\

    private DropTargetStruct resolveDropTarget(float screenX, float screenY) {

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

            if (!isScreenPointInOsWindow(w, screenX, screenY))
                continue;

            long area = w.hasCompositeRect()
                    ? (long) w.getCompositeW() * (long) w.getCompositeH()
                    : Long.MAX_VALUE;

            if (area < bestArea || (area == bestArea && w.getDepth() > bestDepth)) {
                bestArea = area;
                bestDepth = w.getDepth();
                bestWindow = w;
            }
        }

        if (bestWindow == null)
            return null;

        DockNodeStruct leaf = tabManager.getDockLayoutSystem()
                .findLeafAt(screenX, screenY);

        if (leaf == null)
            return null;

        DropZone zone = classifyZone(leaf, screenX, screenY);

        return new DropTargetStruct(bestWindow, leaf, zone);
    }

    private boolean isScreenPointInOsWindow(WindowInstance w, float sx, float sy) {

        if (w.hasCompositeRect())
            return sx >= w.getCompositeX()
                    && sx < w.getCompositeX() + w.getCompositeW()
                    && sy >= w.getCompositeY()
                    && sy < w.getCompositeY() + w.getCompositeH();

        return true;
    }

    private DropZone classifyZone(DockNodeStruct leaf, float screenX, float screenY) {

        float lx = leaf.getX();
        float ly = leaf.getY();
        float lw = leaf.getW();
        float lh = leaf.getH();

        float relX = (screenX - lx) / lw;
        float relY = (screenY - ly) / lh;

        float edge = dropZoneEdgeFraction;

        if (relX < edge)
            return DropZone.LEFT;

        if (relX > 1f - edge)
            return DropZone.RIGHT;

        if (relY < edge)
            return DropZone.TOP;

        if (relY > 1f - edge)
            return DropZone.BOTTOM;

        // Center — resolve to nearest edge
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

        DropTargetStruct target = lastDropTarget;

        WindowInstance sourceTabWindow = draggedHandle.getTabContext().getWindow();
        WindowInstance sourceOsWindow = resolveOsWindow(sourceTabWindow);

        tabManager.getDockLayoutSystem().removeTab(draggedHandle);

        boolean sourceIsMain = sourceOsWindow == windowManager.getMainWindow();
        boolean sourceNowEmpty = tabManager.isOsWindowEmpty(sourceOsWindow);

        if (!sourceIsMain && sourceNowEmpty)
            tabManager.closeOsWindow(sourceOsWindow);

        if (target != null)
            tabManager.getDockLayoutSystem()
                    .addTabToLeaf(target.getLeaf(), draggedHandle, target.getZone());
        else
            tabManager.openSecondaryWindowForTab(draggedHandle);

        tabManager.pushRects();

        clearState();
    }

    // State Cleanup \\

    private void clearState() {

        closeZoneGhost();

        if (dragGhost != null) {
            menuManager.closeMenu(dragGhost);
            dragGhost = null;
        }

        if (dragGhostWindow != null) {
            menuManager.setMenuTargetFbo(dragGhostWindow, null);
            windowManager.removeWindow(dragGhostWindow);
            dragGhostWindow = null;
        }

        draggedHandle = null;
        lastDropTarget = null;
        grabOffsetX = 0f;
        grabOffsetY = 0f;
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