package editor.runtime.editor.menueventsmanager.menus;

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
import editor.runtime.editor.EditorWindowSetting;
import engine.root.BranchPackage;
import engine.root.EngineContext;

public class TabBranch extends BranchPackage {

    /*
     * Menu event handlers for tab chrome menus (TabFrame).
     *
     * onTabFrameDrag distinguishes two gesture types on the first drag frame
     * by checking findDividerAt before latching anything. If a divider is under
     * the cursor the gesture is a resize and is handled identically to before.
     * If no divider is found the gesture is a tab drag and is delegated to
     * TabDragSystem for every subsequent frame. The divider latch and tab drag
     * flag are mutually exclusive — whichever is set on frame one is held for
     * the life of the gesture.
     *
     * isDividerDrag is set to true on the first frame a divider is found and
     * held until mouse release, matching the dragNode pattern. isTabDrag mirrors
     * this for the tab drag path. Both are cleared on release.
     *
     * Hover callbacks receive null for MenuInstance from ElementHitSystem so
     * $parent cannot be used. The hovered window is resolved directly via
     * WindowManager instead.
     *
     * getHoverMouseX/Y is used in checkResizeCursor — hover callbacks must not
     * gate on focus so an unfocused tab still shows correct cursor feedback.
     *
     * onTabFrameDragEnd is called on the release frame inside onTabFrameDrag
     * before clearing local state, so TabDragSystem receives the release signal
     * before the branch resets.
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

    // Tab drag state
    private boolean isTabDrag;

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

        boolean released = EngineContext.input.isMouseReleased(0);

        if (!isDividerDrag && !isTabDrag) {

            float screenX = EngineContext.input.getMouseX();
            float screenY = EngineContext.input.getMouseY();

            dragNode = dockLayoutSystem.findDividerAt(screenX, screenY);

            if (dragNode != null)
                isDividerDrag = true;
            else
                isTabDrag = true;
        }

        if (isDividerDrag) {
            handleDividerDrag(released);
            return;
        }

        handleTabDrag(released);
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

    // Tab Drag \\

    private void handleTabDrag(boolean released) {

        WindowInstance window = windowManager.getHoveredWindow();

        tabDragManager.onTabDragUpdate(window);

        if (released)
            isTabDrag = false;
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