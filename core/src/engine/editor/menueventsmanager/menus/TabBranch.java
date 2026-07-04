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
import engine.editor.EditorSetting;
import engine.root.BranchPackage;

public class TabBranch extends BranchPackage {

    /*
     * Menu event handlers for tab chrome menus (TabFrame).
     *
     * Every gesture here resolves its own acting window explicitly and polls
     * that window's own raw input via InputManager.getRawInput()/
     * getHoverMouseX/Y(). Nothing reads engine.root.EngineContext.input
     * directly — that global reflects whichever window owns engine-wide
     * focus, which is very often NOT this window: tab chrome is deliberately
     * focus-independent, so it's almost never the focused window.
     *
     * dragWindow caches the chrome window a divider drag started on, so
     * every frame of the same gesture keeps polling that same window even
     * if hover or focus moves elsewhere mid-drag.
     *
     * window_frame (on_drag → onTabFrameDrag): BSP divider resize only.
     * tab_toolbar (on_drag → onTabToolbarDrag): delegates to TabDragManager.
     */

    private TabManager tabManager;
    private WindowManager windowManager;
    private InputManager inputManager;
    private SpriteManager spriteManager;
    private DockLayoutSystem dockLayoutSystem;
    private TabDragManager tabDragManager;

    private SpriteHandle cursorResizeH;
    private SpriteHandle cursorResizeV;

    private DockNodeStruct dragNode;
    private WindowInstance dragWindow;
    private boolean isDividerDrag;

    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
        this.windowManager = get(WindowManager.class);
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

    public void onTabFrameDrag() {

        if (!isDividerDrag) {

            WindowInstance window = windowManager.getHoveredWindow();

            if (window == null)
                return;

            float localX = window.getCompositeX() + inputManager.getHoverMouseX(window);
            float localY = window.getCompositeY() + inputManager.getHoverMouseY(window);

            dragNode = dockLayoutSystem.findDividerAt(window.getGLWindow(), localX, localY);

            if (dragNode == null)
                return;

            dragWindow = window;
            isDividerDrag = true;
        }

        boolean released = inputManager.getRawInput(dragWindow.getGLWindow()).isMouseReleased(0);
        handleDividerDrag(released);
    }

    public void onTabToolbarDrag() {
        WindowInstance window = windowManager.getHoveredWindow();
        tabDragManager.onTabDragUpdate(window);
    }

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