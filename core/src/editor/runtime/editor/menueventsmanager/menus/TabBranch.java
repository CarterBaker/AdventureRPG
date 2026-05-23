package editor.runtime.editor.menueventsmanager.menus;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.shaderpipeline.sprite.SpriteHandle;
import application.bootstrap.shaderpipeline.spritemanager.SpriteManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.tab.TabHandle;
import editor.bootstrap.tabmanager.TabManager;
import editor.runtime.editor.EditorWindowSetting;
import engine.root.BranchPackage;

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
     * Mouse Y from GLFW is already converted to Y-up in Lwjgl3Input so no flip
     * is needed when comparing against window bounds.
     */

    private TabManager tabManager;
    private WindowManager windowManager;
    private InputManager inputManager;
    private SpriteManager spriteManager;

    private SpriteHandle cursorResizeH;
    private SpriteHandle cursorResizeV;

    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
        this.windowManager = get(WindowManager.class);
        this.inputManager = get(InputManager.class);
        this.spriteManager = get(SpriteManager.class);
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
        checkResizeCursor(window);
    }

    public void onTabFrameHoverExit() {
        inputManager.clearCursor();
    }

    // Resize Cursor — reusable \\

    private void checkResizeCursor(WindowInstance window) {

        float mouseX = inputManager.getHoverMouseX(window);
        float mouseY = inputManager.getHoverMouseY(window);

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