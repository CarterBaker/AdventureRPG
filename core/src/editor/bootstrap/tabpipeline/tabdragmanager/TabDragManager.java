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
     * Drop resolution uses two separate coordinate values per frame:
     *
     * dragX/dragY — from getGlobalMouseX/Y(). Used only by
     * updateDraggedWindows() to position the floating chrome
     * and content windows within the source OS window's
     * composite space. These are the same coords as before
     * the fix and must not change.
     *
     * resolveDropTarget(dragX, dragY) — receives the same global coords for
     * OS window hit testing, but then queries the platform
     * directly (getCursorXForWindow / getCursorYForWindow) to
     * get window-local coords before calling findLeafAt. This
     * is the key fix: BSP trees store window-local coordinates
     * (origin at the dock canvas top-left within the window),
     * so comparing them against source-window-relative cursor
     * coords fails for any secondary window not at the same
     * screen position.
     *
     * OS window selection in resolveDropTarget was also broken:
     * isScreenPointInOsWindow previously returned true for every OS window
     * (no compositeRect), so bestWindow was always the first OS window in
     * the list regardless of cursor position. The fix tests the cursor
     * against each OS window's actual screen bounds using getScreenX/Y()
     * + getWidth/Height(), and uses the window's real area (not Long.MAX_VALUE)
     * as the tie-break so the smallest containing window wins.
     *
     * On release:
     * 1. If the drop crosses OS windows, moveTabToOsWindow() re-parents the
     * tab chrome and content to the target OS window before any BSP work.
     * 2a. Drop target exists — addTabToLeaf on the resolved leaf.
     * 2b. No drop target — open a new secondary OS window for the handle.
     * 3. Check whether the source OS window is now empty after the move and
     * close it if so (and it is not the main window). The check runs after
     * the move so the dragged handle is no longer counted against the source.
     * 4. pushRects() propagates all composite rects so the re-inserted tab
     * lands at its final BSP-assigned position.
     * 5. clearState() resets all drag fields.
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
    private InputManager inputManager;
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

        // dragX/dragY are used for updateDraggedWindows — they position the
        // floating chrome within the source OS window's composite space and
        // must remain the same global coords used at latch time.
        float dragX = inputManager.getGlobalMouseX();
        float dragY = inputManager.getGlobalMouseY();

        updateDraggedWindows(dragX, dragY);

        DropTargetStruct target = resolveDropTarget(dragX, dragY);
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

        float globalX = inputManager.getGlobalMouseX();
        float globalY = inputManager.getGlobalMouseY();
        grabOffsetX = globalX - tabWindow.getCompositeX();
        grabOffsetY = globalY - tabWindow.getCompositeY();

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
        updateDraggedWindows(globalX, globalY);
    }

    // Dragged Window Tracking \\

    /*
     * Moves both the chrome window and the content window each frame so the
     * full tab — toolbar, chrome menu, and content — travels with the cursor.
     * Both windows keep their existing composite target (the source OS window);
     * their new composite rects simply describe where within that target they
     * render. Uses global cursor coords, which are in the same space as the
     * source OS window's local coords when that window is at screen (0,0) or
     * when the input has been synced to it.
     */
    private void updateDraggedWindows(float dragX, float dragY) {

        float x = dragX - grabOffsetX;
        float y = dragY - grabOffsetY;

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

    /*
     * Two-phase resolution:
     *
     * Phase 1 — OS window selection.
     * Walk all OS windows (hasNativeHandle) and test the global cursor
     * position against each window's real screen bounds using getScreenX/Y()
     * + getWidth/Height(). Previously this used compositeRect, which OS
     * windows never have, causing isScreenPointInOsWindow to always return
     * true and bestWindow to always be the first OS window (main). The fix
     * uses the real screen area as the tie-break so the smallest containing
     * OS window wins, matching the intent of the original algorithm.
     *
     * Phase 2 — BSP leaf lookup.
     * Once bestWindow is known, obtain the cursor position relative to THAT
     * window by querying the platform directly via getCursorXForWindow/Y.
     * This bypasses hoveredWindowLocked and the syncInputForWindow mechanism,
     * both of which may still reflect the drag-source window. Pass those
     * window-local coords to the scoped findLeafAt(osWindow, localX, localY)
     * overload so the search only covers the correct BSP tree with matching
     * coordinates.
     */
    private DropTargetStruct resolveDropTarget(float globalX, float globalY) {

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

            if (!isGlobalPointInOsWindow(w, globalX, globalY))
                continue;

            // Use the real window area rather than Long.MAX_VALUE so the
            // smallest containing OS window wins when windows overlap.
            long area = (long) w.getWidth() * (long) w.getHeight();

            if (area < bestArea || (area == bestArea && w.getDepth() > bestDepth)) {
                bestArea = area;
                bestDepth = w.getDepth();
                bestWindow = w;
            }
        }

        if (bestWindow == null)
            return null;

        // Get cursor position in bestWindow's local space via a direct platform
        // query — independent of which window syncInputForWindow last targeted.
        float localX = inputManager.getCursorXForWindow(bestWindow);
        float localY = inputManager.getCursorYForWindow(bestWindow);

        DockNodeStruct leaf = tabManager.getDockLayoutSystem()
                .findLeafAt(bestWindow, localX, localY);

        if (leaf == null)
            return null;

        DropZone zone = classifyZone(leaf, localX, localY);

        return new DropTargetStruct(bestWindow, leaf, zone);
    }

    /*
     * Tests whether the global screen cursor position falls within an OS
     * window's real screen bounds. Uses getScreenX/Y() (set by the platform
     * layer on open and on window-moved callbacks) plus getWidth/Height().
     *
     * Previously this method returned true unconditionally for OS windows
     * because compositeRect is never set on them. That caused every OS window
     * to be a candidate, made the area tie-break meaningless (all Long.MAX_VALUE),
     * and forced bestWindow to always be the first OS window in the list.
     */
    private boolean isGlobalPointInOsWindow(WindowInstance w, float globalX, float globalY) {
        return globalX >= w.getScreenX()
                && globalX < w.getScreenX() + w.getWidth()
                && globalY >= w.getScreenY()
                && globalY < w.getScreenY() + w.getHeight();
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

        if (target != null) {

            WindowInstance targetOsWindow = target.getWindow();

            // Re-parent chrome and content to the target OS window before any BSP
            // work. The old TabContext and its logical window are destroyed here —
            // after this point sourceTabWindow is stale and must not be read.
            if (targetOsWindow != sourceOsWindow)
                tabManager.moveTabToOsWindow(draggedHandle, targetOsWindow);

            tabManager.getDockLayoutSystem()
                    .addTabToLeaf(target.getLeaf(), draggedHandle, target.getZone());

        } else {

            // No target — float into a brand-new OS window. openSecondaryWindowForTab
            // handles its own teardown and re-parent internally.
            tabManager.openSecondaryWindowForTab(draggedHandle);
        }

        // Check emptiness after the move so the dragged handle is no longer
        // counted against the source OS window. The old tab chrome window was
        // removed by moveTabToOsWindow or openSecondaryWindowForTab, so
        // isSourceOsWindowEmpty now reflects the post-move state accurately.
        boolean sourceIsMain = sourceOsWindow == windowManager.getMainWindow();
        boolean sourceNowEmpty = isSourceOsWindowEmpty(sourceOsWindow);

        if (!sourceIsMain && sourceNowEmpty)
            tabManager.closeOsWindow(sourceOsWindow);

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