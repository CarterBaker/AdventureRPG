package engine.editor;

import application.bootstrap.menupipeline.canvas.CanvasInstance;
import application.bootstrap.menupipeline.elementhitsystem.ElementHitSystem;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FboManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.SystemPackage;

public class EditorMenuSystem extends SystemPackage {

    /*
     * Owns the editor chrome of the OS window its context is paired with. The
     * base menu (background and dock canvas) renders on the OS window itself;
     * the toolbar lives in its own logical window so its dropdowns composite
     * above tabs. Each frame the dock canvas is published to TabManager and
     * the toolbar window is kept sized to the OS window, re-raised whenever
     * anything has been brought up over it, and given an input rect: the strip
     * above the dock canvas while idle, so it never covers the tabs hover and
     * focus resolve against, and the whole window while one of its dropdowns
     * is hovered open. Only input changes — the toolbar is never resized for
     * it, so its FBO and layout stay stable.
     */

    // Internal
    private WindowManager windowManager;
    private MenuManager menuManager;
    private ElementHitSystem elementHitSystem;
    private FboManager fboManager;
    private TabManager tabManager;

    // Menus
    private MenuInstance baseMenu;
    private MenuInstance toolbarMenu;

    // Base \\

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
        this.menuManager = get(MenuManager.class);
        this.elementHitSystem = get(ElementHitSystem.class);
        this.fboManager = get(FboManager.class);
        this.tabManager = get(TabManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance osWindow = context.getWindow();

        menuManager.setMenuTargetFbo(osWindow, fboManager.cloneFbo(RuntimeSetting.FBO_UI, osWindow));
        this.baseMenu = menuManager.openMenu(EditorSetting.MENU_EDITOR_BASE, osWindow);
        this.toolbarMenu = menuManager.openMenuWindow(EditorSetting.MENU_EDITOR_TOOLBAR, osWindow);
    }

    @Override
    protected void dispose() {
        menuManager.closeMenuWindow(toolbarMenu);
        menuManager.closeMenu(baseMenu);
        menuManager.setMenuTargetFbo(context.getWindow(), null);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance osWindow = context.getWindow();

        syncDockRect(osWindow);
        syncToolbar(osWindow);
    }

    private void syncDockRect(WindowInstance osWindow) {

        CanvasInstance canvas = baseMenu.getCanvas();

        if (canvas == null)
            return;

        tabManager.setDockRect(osWindow, canvas.getX(), canvas.getY(), canvas.getW(), canvas.getH());
    }

    private void syncToolbar(WindowInstance osWindow) {

        WindowInstance toolbarWindow = toolbarMenu.getWindow();
        int w = osWindow.getWidth();
        int h = osWindow.getHeight();

        if (w > 0 && h > 0 && (w != toolbarWindow.getWidth() || h != toolbarWindow.getHeight()))
            toolbarWindow.place(0, 0, w, h);

        float inputTop = elementHitSystem.getHoveredMenu() == toolbarMenu ? 0f : getDockTop();
        toolbarWindow.setInputRect(0, inputTop, w, h - inputTop);

        if (!windowManager.isFrontmost(toolbarWindow))
            windowManager.bringToFront(toolbarWindow);
    }

    private float getDockTop() {
        CanvasInstance canvas = baseMenu.getCanvas();
        return canvas != null ? canvas.getY() + canvas.getH() : 0f;
    }
}
