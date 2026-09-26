package editor.bootstrap.tabpipeline.tabdragmanager;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.bootstrap.tabpipeline.util.DropTargetStruct;
import editor.bootstrap.tabpipeline.util.DropZone;
import editor.bootstrap.tabpipeline.util.TabDragLayoutStruct;
import editor.runtime.EditorSetting;
import engine.input.Buttons;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TabDragManager extends ManagerPackage {

    /*
     * Owns the tab drag lifecycle. On latch the tab leaves the BSP and floats
     * to the front; each frame it follows the cursor while the drop target and
     * zone resolve from the hovered OS window, previewed by a zone ghost. Every
     * drop routes through TabManager, docking into a leaf or opening a new
     * window.
     */
    // Internal
    private WindowManager windowManager;
    private MenuManager menuManager;
    private TabManager tabManager;
    private InputManager inputManager;
    private DockLayoutSystem dockLayoutSystem;
    private TabDragLayoutStruct tabDragLayoutStruct;
    // Drag
    private TabHandle draggedHandle;
    private DropTargetStruct lastDropTarget;
    // Zone Ghost
    private MenuInstance zoneGhost;

    // Internal \\
    @Override
    protected void create() {
        this.tabDragLayoutStruct = new TabDragLayoutStruct();
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
        this.menuManager = get(MenuManager.class);
        this.tabManager = get(TabManager.class);
        this.inputManager = get(InputManager.class);
        this.dockLayoutSystem = get(DockLayoutSystem.class);
    }

    @Override
    protected void update() {

        if (draggedHandle == null)
            return;

        if (!draggedHandle.isOpen()) {
            clearState();
            return;
        }

        WindowInstance sourceOsWindow = draggedHandle.getTabContext().getWindow().getGLWindow();

        if (inputManager.getRawInput(sourceOsWindow).isMouseReleased(Buttons.LEFT)) {
            executeDrop();
            return;
        }

        updateDraggedTab(sourceOsWindow);

        DropTargetStruct target = resolveDropTarget();
        updateZoneGhost(target);
        lastDropTarget = target;
    }

    // Entry Point \\
    public void onTabDragUpdate(WindowInstance chromeWindow) {

        if (draggedHandle != null)
            return;

        if (inputManager.getRawInput(chromeWindow).isMouseReleased(Buttons.LEFT))
            return;

        latchDrag(chromeWindow);
    }

    // Drag Start \\
    private void latchDrag(WindowInstance chromeWindow) {

        TabHandle handle = tabManager.getTabHandleForWindow(chromeWindow);

        if (handle == null)
            return;

        WindowInstance osWindow = chromeWindow.getGLWindow();

        draggedHandle = handle;
        handle.getTabContext().bringToFront();
        dockLayoutSystem.removeTab(osWindow, handle);
        tabManager.pushRects();
        updateDraggedTab(osWindow);
    }

    // Drag Tracking \\
    private void updateDraggedTab(WindowInstance osWindow) {
        draggedHandle.getTabContext().placeAt(
                inputManager.getGlobalMouseX(osWindow),
                inputManager.getGlobalMouseY(osWindow),
                EditorSetting.TAB_DRAG_PREVIEW_W,
                EditorSetting.TAB_DRAG_PREVIEW_H);
    }

    // Zone Ghost \\
    private void updateZoneGhost(DropTargetStruct target) {

        if (target != null && target.matches(lastDropTarget))
            return;

        DockNodeStruct leaf = target != null ? target.getLeaf() : null;

        if (zoneGhost != null && (leaf == null || zoneGhost.getWindow().getGLWindow() != target.getWindow()))
            closeZoneGhost();

        if (leaf == null)
            return;

        if (zoneGhost == null)
            zoneGhost = menuManager.openMenuWindow(EditorSetting.MENU_TAB_GHOST, target.getWindow());

        DropZone zone = target.getZone();
        zoneGhost.getWindow().place(
                tabDragLayoutStruct.zoneX(leaf.getX(), leaf.getW(), zone),
                tabDragLayoutStruct.zoneY(leaf.getY(), leaf.getH(), zone),
                tabDragLayoutStruct.zoneW(leaf.getW(), zone),
                tabDragLayoutStruct.zoneH(leaf.getH(), zone));
    }

    private void closeZoneGhost() {
        menuManager.closeMenuWindow(zoneGhost);
        zoneGhost = null;
    }

    // Drop Resolution \\
    private DropTargetStruct resolveDropTarget() {

        WindowInstance osWindow = resolveHoveredOsWindow();

        if (osWindow == null)
            return null;

        float localX = inputManager.getGlobalMouseX(osWindow);
        float localY = inputManager.getGlobalMouseY(osWindow);
        DockNodeStruct leaf = dockLayoutSystem.findLeafAt(osWindow, localX, localY);
        DropZone zone = leaf != null ? classifyZone(leaf, localX, localY) : null;

        return new DropTargetStruct(osWindow, leaf, zone);
    }

    private WindowInstance resolveHoveredOsWindow() {

        ObjectArrayList<WindowInstance> hoveredWindows = windowManager.getHoveredWindows();

        for (int i = 0; i < hoveredWindows.size(); i++) {
            WindowInstance window = hoveredWindows.get(i);
            if (window.hasNativeHandle())
                return window;
        }

        return null;
    }

    private DropZone classifyZone(DockNodeStruct leaf, float localX, float localY) {
        float relX = (localX - leaf.getX()) / leaf.getW();
        float relY = (localY - leaf.getY()) / leaf.getH();
        float edge = EditorSetting.TAB_DRAG_EDGE_FRACTION;
        if (relX < edge)
            return DropZone.LEFT;
        if (relX > 1f - edge)
            return DropZone.RIGHT;
        if (relY < edge)
            return DropZone.TOP;
        if (relY > 1f - edge)
            return DropZone.BOTTOM;
        float minH = Math.min(relX, 1f - relX);
        float minV = Math.min(relY, 1f - relY);
        if (minH <= minV)
            return relX < 1f - relX ? DropZone.LEFT : DropZone.RIGHT;
        return relY < 1f - relY ? DropZone.TOP : DropZone.BOTTOM;
    }

    // Drop Execution \\
    private void executeDrop() {

        TabHandle handle = draggedHandle;
        DropTargetStruct target = lastDropTarget;
        clearState();

        if (target != null)
            tabManager.dockTab(handle, target.getWindow(), target.getLeaf(), target.getZone());
        else
            tabManager.openSecondaryWindowForTab(handle);

        windowManager.setFocusedWindow(handle.getWindow());
    }

    // State Cleanup \\

    private void clearState() {
        closeZoneGhost();
        draggedHandle = null;
        lastDropTarget = null;
    }

    // Accessible \\
    public boolean isDragging() {
        return draggedHandle != null;
    }
}
