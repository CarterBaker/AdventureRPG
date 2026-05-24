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
     * work after latch runs in update() so the drag is never interrupted by
     * hover state changes. Once draggedHandle is set, the drag continues until
     * isMouseReleased(0) regardless of which window the cursor is over.
     *
     * On latch: the handle is removed from the BSP immediately so remaining
     * tabs reflow into the vacated space. Tab dimensions are cached from the
     * tab window before pushRects() overwrites them. The actual tabWindow and
     * contentWindow are then driven by updateDraggedWindows() each frame so
     * the full chrome and content travel with the cursor. No separate drag
     * ghost window is created — the real windows are the ghost.
     *
     * Depth during drag: the tab window and content window are elevated to
     * TAB_DRAG_TAB_DEPTH and TAB_DRAG_CONTENT_DEPTH so they render above the
     * zone ghost. On drop they are restored to TAB_DEFAULT_TAB_DEPTH and
     * TAB_DEFAULT_CONTENT_DEPTH. These four constants are the single source of
     * truth — no arithmetic on depth values anywhere in this class.
     *
     * zoneGhost — half-panel drop preview rendered on the target OS window at
     * TAB_DRAG_GHOST_DEPTH. The ghost window and FBO are allocated once per
     * drag gesture when the first drop target is resolved. On subsequent zone
     * changes within the same OS window the ghost is simply repositioned and
     * the FBO resized — no teardown. If the cursor moves to a different OS
     * window the ghost is fully torn down and rebuilt against the new target.
     * Closed immediately before drop — it is a preview only.
     *
     * Drop resolution walks only OS windows (hasNativeHandle) each frame to
     * find which one contains the raw screen cursor, then delegates to
     * DockLayoutSystem.findLeafAt() for the leaf, then classifies the cursor
     * position within that leaf into a DropZone. The center region resolves
     * to the nearest edge zone so every pixel of a leaf has a defined
     * destination.
     *
     * On release:
     * 1. Check whether the source OS window is now empty, excluding the
     * dragged handle itself (which is about to be re-inserted elsewhere).
     * Close it if so and it is not the main window.
     * 2a. Drop target exists — addTabToLeaf on the resolved leaf.
     * 2b. No drop target — open a new secondary OS window for the handle.
     * 3. pushRects() propagates all composite rects so the re-inserted tab
     * lands at its final BSP-assigned position.
     * 4. clearState() resets all drag fields.
     *
     * grabOffsetX/Y is the cursor offset within the dragged tab window at
     * latch time so the content does not snap to the cursor corner on pickup.
     * dragW/H caches the tab dimensions before pushRects() clears them.
     *
     * resolveOsWindow() is used in latchDrag() to pass the correct OS window
     * key to DockLayoutSystem.removeTab() — the tab window is a logical window
     * compositing into an OS window, not an OS window itself.
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
    private float dragW;
    private float dragH;

    // Zone ghost — drop-target preview on the target OS window
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

        updateDraggedWindows(screenX, screenY);

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

        WindowInstance tabWindow = handle.getTabContext().getWindow();

        float screenX = EngineContext.input.getMouseX();
        float screenY = EngineContext.input.getMouseY();
        grabOffsetX = screenX - tabWindow.getCompositeX();
        grabOffsetY = screenY - tabWindow.getCompositeY();

        // Cache tab dimensions before BSP reflow overwrites the composite rect.
        dragW = tabWindow.getCompositeW();
        dragH = tabWindow.getCompositeH();

        draggedHandle = handle;

        // Elevate above the zone ghost for the duration of the drag.
        tabWindow.setDepth(EngineSetting.TAB_DRAG_TAB_DEPTH);
        handle.getContentContext().getWindow().setDepth(EngineSetting.TAB_DRAG_CONTENT_DEPTH);

        // Remove from the correct per-OS-window BSP tree immediately so
        // remaining tabs reflow into the gap.
        WindowInstance osWindow = resolveOsWindow(tabWindow);
        tabManager.getDockLayoutSystem().removeTab(osWindow, handle);
        tabManager.pushRects();

        // Place the actual windows at the cursor on the first frame.
        updateDraggedWindows(screenX, screenY);
    }

    // Dragged Window Tracking \\

    /*
     * Moves both the chrome window and the content window each frame so the
     * full tab — toolbar, chrome menu, and content — travels with the cursor.
     * Both windows keep their existing composite target (the source OS window);
     * their new composite rects simply describe where within that target they
     * render.
     */
    private void updateDraggedWindows(float screenX, float screenY) {

        float x = screenX - grabOffsetX;
        float y = screenY - grabOffsetY;

        WindowInstance tabWindow = draggedHandle.getTabContext().getWindow();
        WindowInstance contentWindow = draggedHandle.getContentContext().getWindow();

        tabWindow.setCompositeRect(x, y, dragW, dragH);
        contentWindow.setCompositeRect(x, y, dragW, dragH);
    }

    // Zone Ghost \\

    /*
     * Three tiers of work per frame:
     * - Zone unchanged → nothing (early return on matches).
     * - Zone changed, same OS → reposition and resize only, FBO reused.
     * - OS window changed → full teardown then rebuild against new target.
     */
    private void updateZoneGhost(DropTargetStruct target) {

        // Zone unchanged — nothing to do.
        if (target != null && target.matches(lastDropTarget))
            return;

        // Same OS window, zone changed — reposition without teardown.
        if (target != null
                && lastDropTarget != null
                && target.getWindow() == lastDropTarget.getWindow()
                && zoneGhostWindow != null) {
            repositionZoneGhost(target);
            return;
        }

        // OS window changed, no target, or ghost not open — full cycle.
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

    private DropTargetStruct resolveDropTarget(float screenX, float screenY) {

        ObjectArrayList<WindowInstance> windows = windowManager.getWindows();
        Object[] elements = windows.elements();
        int size = windows.size();

        WindowInstance bestWindow = null;
        long bestArea = Long.MAX_VALUE;
        int bestDepth = Integer.MIN_VALUE;

        for (int i = 0; i < size; i++) {

            WindowInstance w = (WindowInstance) elements[i];

            // Only consider real OS windows, not logical/ghost windows.
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

        // Window fills its entire monitor — always a candidate.
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

        // Center region — resolve to nearest edge so every pixel has a destination.
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

        // The handle was already removed from the BSP at latch time.
        // Check whether any other tab still composites to the source OS window
        // before deciding to close it.
        boolean sourceIsMain = sourceOsWindow == windowManager.getMainWindow();
        boolean sourceNowEmpty = isSourceOsWindowEmpty(sourceOsWindow);

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

    /*
     * Returns true if no tab other than the currently dragged one has its
     * chrome window compositing to the given OS window. The dragged handle is
     * excluded because it is about to be re-inserted somewhere else and should
     * not prevent an empty source window from being closed.
     */
    private boolean isSourceOsWindowEmpty(WindowInstance osWindow) {

        ObjectArrayList<TabHandle> openTabs = tabManager.getOpenTabs();
        Object[] elements = openTabs.elements();
        int size = openTabs.size();

        for (int i = 0; i < size; i++) {

            TabHandle handle = (TabHandle) elements[i];

            if (handle == draggedHandle)
                continue;

            WindowInstance tabWindow = handle.getTabContext().getWindow();
            WindowInstance composite = tabWindow.getCompositeTarget();

            if (composite == osWindow)
                return false;
        }

        return true;
    }

    // State Cleanup \\

    private void clearState() {

        closeZoneGhost();

        // Restore default render order.
        if (draggedHandle != null) {
            draggedHandle.getTabContext().getWindow().setDepth(EngineSetting.TAB_DEFAULT_TAB_DEPTH);
            draggedHandle.getContentContext().getWindow().setDepth(EngineSetting.TAB_DEFAULT_CONTENT_DEPTH);
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