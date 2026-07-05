package engine.editor.menueventsmanager.menus;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.shaderpipeline.sprite.SpriteHandle;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.tabpipeline.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.tabpipeline.docknode.DockNodeStruct;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabdragmanager.TabDragManager;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.editor.EditorSetting;
import engine.root.BranchPackage;

public class TabBranch extends BranchPackage {

    /*
     * Menu event handlers for tab chrome menus (TabFrame).
     *
     * Every gesture here receives the WindowInstance it happened on
     * directly, as a parameter supplied by ElementHitSystem — the same
     * window ElementHitSystem itself determined the hit occurred on. None
     * of these methods re-derive "which window is this" via a separate
     * lookup, so there is no way for the two to disagree.
     *
     * dragWindow caches the chrome window a divider drag started on, so
     * every frame of the same gesture keeps polling that same window even
     * if hover moves elsewhere mid-drag.
     *
     * window_frame (on_drag → onTabFrameDrag(WindowInstance)):
     * Handles BSP divider resize only. On the first drag frame it calls
     * findDividerAt — if no divider is found the gesture is ignored entirely,
     * so dragging the tab background never accidentally triggers a tab move.
     * Window-edge resize is handled natively by the OS.
     *
     * tab_toolbar (on_drag → onTabToolbarDrag(WindowInstance)):
     * Delegates entirely to TabDragManager, passing the exact window the
     * gesture latched on — fixed once at the moment the drag started, not
     * re-queried live every frame, so a fast gesture that briefly crosses
     * another window mid-drag can't be attributed to the wrong source.
     * The manager latches the handle on the first call and then owns the
     * drag loop via update(); subsequent on_drag callbacks are ignored
     * because draggedHandle is already set.
     *
     * isDividerDrag is set on the first frame a divider is confirmed and held
     * until mouse release.
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
    private InputManager inputManager;
    private SpriteManager spriteManager;
    private DockLayoutSystem dockLayoutSystem;
    private TabDragManager tabDragManager;

    // Cursor sprites
    private SpriteHandle cursorResizeH;
    private SpriteHandle cursorResizeV;

    // Divider drag state
    private DockNodeStruct dragNode;
    private WindowInstance dragWindow;
    private boolean isDividerDrag;

    // Internal \\

    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
        this.inputManager = get(InputManager.class);
        this.spriteManager = get(SpriteManager.class);
        this.dockLayoutSystem = get(DockLayoutSystem.class);
        this.tabDragManager = get(TabDragManager.class);
        this.cursorResizeH = spriteManager.getSpriteHandleFromSpriteName(EditorSetting.CURSOR_RESIZE_H);
        this.cursorResizeV = spriteManager.getSpriteHandleFromSpriteName(EditorSetting.CURSOR_RESIZE_V);
    }

    @Override
    protected void update() {
        if (isDividerDrag && inputManager.getRawInput(dragWindow.getGLWindow()).isMouseReleased(0))
            endDividerDrag();
    }

    // Tab Events \\

    public void closeTab(MenuInstance menu) {
        TabHandle handle = tabManager.getTabHandleForWindow(menu.getWindow());
        if (handle == null)
            return;
        tabManager.closeTab(handle);
    }

    public void onTabFrameHover(WindowInstance window) {

        float mouseX = inputManager.getHoverMouseX(window);
        float mouseY = inputManager.getHoverMouseY(window);
        float localX = window.getCompositeX() + mouseX;
        float localY = window.getCompositeY() + mouseY;

        DockNodeStruct divider = dockLayoutSystem.findDividerAt(window.getGLWindow(), localX, localY);

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
    public void onTabFrameDrag(WindowInstance window) {

        if (!isDividerDrag) {

            float localX = window.getCompositeX() + inputManager.getHoverMouseX(window);
            float localY = window.getCompositeY() + inputManager.getHoverMouseY(window);

            dragNode = dockLayoutSystem.findDividerAt(window.getGLWindow(), localX, localY);

            if (dragNode == null)
                return; // Not a divider — nothing for the background drag to do.

            dragWindow = window;
            isDividerDrag = true;
        }

        boolean released = inputManager.getRawInput(dragWindow.getGLWindow()).isMouseReleased(0);
        handleDividerDrag(released);
    }

    // tab_toolbar drag — tab repositioning only.
    // TabDragManager latches the handle on the first call and then owns the
    // drag loop via update(); subsequent callbacks are no-ops.
    public void onTabToolbarDrag(WindowInstance window) {
        tabDragManager.onTabDragUpdate(window);
    }

    // Divider Drag \\

    private void handleDividerDrag(boolean released) {

        if (released) {
            endDividerDrag();
            return;
        }

        if (dragNode == null)
            return;

        float localX = dragWindow.getCompositeX() + inputManager.getHoverMouseX(dragWindow);
        float localY = dragWindow.getCompositeY() + inputManager.getHoverMouseY(dragWindow);

        float ratio = dragNode.isSplitHorizontal()
                ? (localY - dragNode.getY()) / dragNode.getH()
                : (localX - dragNode.getX()) / dragNode.getW();

        dockLayoutSystem.setSplitRatio(dragNode, ratio);
        tabManager.pushRects();
    }

    private void endDividerDrag() {
        dragNode = null;
        dragWindow = null;
        isDividerDrag = false;
    }

    // Resize Cursor \\

    private void checkResizeCursor(WindowInstance window, float mouseX, float mouseY) {

        float w = window.getWidth();
        float h = window.getHeight();
        float t = EditorSetting.RESIZE_EDGE_TOLERANCE;

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