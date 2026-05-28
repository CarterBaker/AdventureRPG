package engine.editor.menueventsmanager.menus;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.shaderpipeline.sprite.SpriteHandle;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabdragmanager.TabDragManager;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.editor.EditorWindowSetting;
import engine.root.BranchPackage;
import engine.root.EngineContext;

public class TabBranch extends BranchPackage {

    /*
     * Menu event handlers for tab chrome menus (TabFrame).
     *
     * Drag concerns are separated by element:
     *
     * window_frame (on_drag → onTabFrameDrag):
     * Handles BSP divider resize only. On the first drag frame it calls
     * findDividerAt — if no divider is found the gesture is ignored entirely,
     * so dragging the tab background never accidentally triggers a tab move.
     * Window-edge resize is handled natively by the OS.
     *
     * tab_toolbar (on_drag → onTabToolbarDrag):
     * Delegates entirely to TabDragManager. The manager latches the handle on
     * the first call and then owns the drag loop via update(); subsequent
     * on_drag callbacks are ignored because draggedHandle is already set.
     *
     * isDividerDrag is set on the first frame a divider is confirmed and held
     * until mouse release. It is the only latch flag remaining — isTabDrag has
     * been removed because TabDragManager owns that state internally.
     *
     * Hover callbacks receive null for MenuInstance from ElementHitSystem so
     * $parent cannot be used. The hovered window is resolved directly via
     * WindowManager instead.
     *
     * getHoverMouseX/Y is used in checkResizeCursor — hover callbacks must not
     * gate on focus so an unfocused tab still shows correct cursor feedback.
     *
     * onTabFrameHoverExit is wired to window_frame only. If the cursor moves
     * from window_frame into tab_toolbar the exit fires and clears the cursor,
     * which is correct because the toolbar area should not show resize feedback.
     */

    // Internal
    private TabManager tabManager;
    private WindowManager windowManager;
    private InputManager inputManager;
    private SpriteManager spriteManager;
    private DockLayoutSystem dockLayoutSystem;
    private TabDragManager tabDragManager;

    // Cursor sprites
    private SpriteHandle cursorResizeH;
    private SpriteHandle cursorResizeV;

    // Divider drag state
    private DockNodeStruct dragNode;
    private boolean isDividerDrag;

    // Internal \\

    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
        this.windowManager = get(WindowManager.class);
        this.inputManager = get(InputManager.class);
        this.spriteManager = get(SpriteManager.class);
        this.dockLayoutSystem = get(DockLayoutSystem.class);
        this.tabDragManager = get(TabDragManager.class);
        this.cursorResizeH = spriteManager.getSpriteHandleFromSpriteName(EditorWindowSetting.CURSOR_RESIZE_H);
        this.cursorResizeV = spriteManager.getSpriteHandleFromSpriteName(EditorWindowSetting.CURSOR_RESIZE_V);
    }

    @Override
    protected void update() {
        if (isDividerDrag && EngineContext.input.isMouseReleased(0)) {
            dragNode = null;
            isDividerDrag = false;
        }
    }

    // Tab Events \\

    public void closeTab(MenuInstance menu) {
        TabHandle handle = tabManager.getTabHandleForWindow(menu.getWindow());
        if (handle == null)
            return;
        tabManager.closeTab(handle);
    }

    public void onTabFrameHover() {

        WindowInstance window = windowManager.getHoveredWindow();

        if (window == null)
            return;

        float mouseX = inputManager.getHoverMouseX(window);
        float mouseY = inputManager.getHoverMouseY(window);
        float screenX = window.getCompositeX() + mouseX;
        float screenY = window.getCompositeY() + mouseY;

        DockNodeStruct divider = dockLayoutSystem.findDividerAt(screenX, screenY);

        if (divider != null) {
            inputManager.setCursorSprite(divider.isSplitHorizontal() ? cursorResizeV : cursorResizeH);
            return;
        }

        checkResizeCursor(window, mouseX, mouseY);
    }

    public void onTabFrameHoverExit() {
        dragNode = null;
        inputManager.clearCursor();
    }

    // window_frame drag — BSP divider resize only.
    // If the cursor is not over a divider the gesture is ignored.
    // Tab dragging is handled by onTabToolbarDrag below.
    public void onTabFrameDrag() {

        boolean released = EngineContext.input.isMouseReleased(0);

        if (!isDividerDrag) {
            float screenX = EngineContext.input.getMouseX();
            float screenY = EngineContext.input.getMouseY();

            dragNode = dockLayoutSystem.findDividerAt(screenX, screenY);

            if (dragNode == null)
                return; // Not a divider — nothing for the background drag to do.

            isDividerDrag = true;
        }

        handleDividerDrag(released);
    }

    // tab_toolbar drag — tab repositioning only.
    // TabDragManager latches the handle on the first call and then owns the
    // drag loop via update(); subsequent callbacks are no-ops.
    public void onTabToolbarDrag() {
        WindowInstance window = windowManager.getHoveredWindow();
        tabDragManager.onTabDragUpdate(window);
    }

    // Divider Drag \\

    private void handleDividerDrag(boolean released) {

        if (released) {
            dragNode = null;
            isDividerDrag = false;
            return;
        }

        if (dragNode == null)
            return;

        float screenX = EngineContext.input.getMouseX();
        float screenY = EngineContext.input.getMouseY();

        float ratio = dragNode.isSplitHorizontal()
                ? (screenY - dragNode.getY()) / dragNode.getH()
                : (screenX - dragNode.getX()) / dragNode.getW();

        dockLayoutSystem.setSplitRatio(dragNode, ratio);
        tabManager.pushRects();
    }

    // Resize Cursor \\

    private void checkResizeCursor(WindowInstance window, float mouseX, float mouseY) {

        float w = window.getWidth();
        float h = window.getHeight();
        float t = EditorWindowSetting.RESIZE_EDGE_TOLERANCE;

        boolean onHEdge = mouseX <= t || mouseX >= w - t;
        boolean onVEdge = mouseY <= t || mouseY >= h - t;

        if (onHEdge)
            inputManager.setCursorSprite(cursorResizeH);
        else if (onVEdge)
            inputManager.setCursorSprite(cursorResizeV);
        else
            inputManager.clearCursor();
    }
}