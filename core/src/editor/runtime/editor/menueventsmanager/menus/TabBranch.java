package editor.runtime.editor.menueventsmanager.menus;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.shaderpipeline.sprite.SpriteHandle;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.docklayoutsystem.DockLayoutSystem;
import editor.bootstrap.docknode.DockNodeStruct;
import editor.bootstrap.tab.TabHandle;
import editor.bootstrap.tabmanager.TabManager;
import editor.runtime.editor.EditorWindowSetting;
import engine.root.BranchPackage;
import engine.root.EngineContext;

public class TabBranch extends BranchPackage {

    /*
     * Menu event handlers for tab chrome menus (TabFrame).
     *
     * Each TabFrame menu instance is owned by a TabContext whose window is
     * registered in TabManager. Resolving the MenuInstance back to a TabHandle
     * goes through getTabHandleForWindow() — the same resolver that InputSystem
     * uses for authority mapping — so there is no separate lookup table to
     * maintain here.
     *
     * closeTab() is intentionally a no-op when the handle is not found rather
     * than throwing, because a rapid double-click could fire the handler a
     * second time after the tab is already gone.
     *
     * Hover callbacks receive null for MenuInstance from ElementHitSystem so
     * $parent cannot be used. The hovered window is resolved directly via
     * WindowManager instead. window_frame covers the full tab so window bounds
     * are used for edge detection — no element lookup needed.
     *
     * getHoverMouseX/Y is used in checkResizeCursor rather than getMouseX/Y.
     * Hover callbacks fire based on spatial position and must not gate on focus
     * — an unfocused tab still needs correct cursor feedback when the mouse is
     * over its edge.
     *
     * Mouse Y from GLFW is already converted to Y-up in Lwjgl3Input so no flip
     * is needed when comparing against window bounds.
     *
     * onTabFrameDrag reads raw screen-space coords directly from
     * EngineContext.input
     * so window boundaries, hover state, and focus cannot interrupt the gesture.
     * BSP node bounds are in screen space so no conversion is needed.
     *
     * dragNode is latched on the first drag frame a divider is found and held
     * for the duration of the gesture. Re-testing findDividerAt every frame
     * would drop the drag the moment the cursor moves outside the hit tolerance
     * band. Since on_drag only fires while the button is held, the latch is
     * implicitly released when the gesture ends and onTabFrameDrag stops firing.
     */

    private TabManager tabManager;
    private WindowManager windowManager;
    private InputManager inputManager;
    private SpriteManager spriteManager;
    private DockLayoutSystem dockLayoutSystem;

    private SpriteHandle cursorResizeH;
    private SpriteHandle cursorResizeV;

    private DockNodeStruct dragNode;

    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
        this.windowManager = get(WindowManager.class);
        this.inputManager = get(InputManager.class);
        this.spriteManager = get(SpriteManager.class);
        this.dockLayoutSystem = get(DockLayoutSystem.class);
        this.cursorResizeH = spriteManager.getSpriteHandleFromSpriteName(EditorWindowSetting.CURSOR_RESIZE_H);
        this.cursorResizeV = spriteManager.getSpriteHandleFromSpriteName(EditorWindowSetting.CURSOR_RESIZE_V);
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

    public void onTabFrameDrag() {

        float screenX = EngineContext.input.getMouseX();
        float screenY = EngineContext.input.getMouseY();

        if (dragNode == null)
            dragNode = dockLayoutSystem.findDividerAt(screenX, screenY);

        if (dragNode == null)
            return;

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