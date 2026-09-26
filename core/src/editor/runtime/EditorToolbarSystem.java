package editor.runtime;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.ElementHitSystem;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.SystemPackage;

public class EditorToolbarSystem extends SystemPackage {

    /*
     * Owns the editor toolbar, which only the main window carries. The toolbar
     * lives in its own logical window so its dropdowns composite above tabs.
     * Each frame it is kept sized to the OS window, re-raised whenever anything
     * has been brought up over it, and given an input rect: the strip above
     * the dock canvas while idle, so it never covers the tabs hover and focus
     * resolve against, and the whole window while one of its dropdowns is
     * hovered open. Only input changes — the toolbar is never resized for it,
     * so its FBO and layout stay stable.
     */

    // Internal
    private WindowManager windowManager;
    private MenuManager menuManager;
    private ElementHitSystem elementHitSystem;
    private EditorDockSystem editorDockSystem;

    // Menus
    private MenuInstance toolbarMenu;

    // Base \\

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
        this.menuManager = get(MenuManager.class);
        this.elementHitSystem = get(ElementHitSystem.class);
        this.editorDockSystem = get(EditorDockSystem.class);
    }

    @Override
    protected void awake() {
        this.toolbarMenu = menuManager.openMenuWindow(EditorSetting.MENU_EDITOR_TOOLBAR, context.getWindow());
    }

    @Override
    protected void dispose() {
        menuManager.closeMenuWindow(toolbarMenu);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance osWindow = context.getWindow();
        WindowInstance toolbarWindow = toolbarMenu.getWindow();
        int w = osWindow.getWidth();
        int h = osWindow.getHeight();

        if (w > 0 && h > 0 && (w != toolbarWindow.getWidth() || h != toolbarWindow.getHeight()))
            toolbarWindow.place(0, 0, w, h);

        float inputTop = elementHitSystem.getHoveredMenu() == toolbarMenu ? 0f : editorDockSystem.getDockTop();
        toolbarWindow.setInputRect(0, inputTop, w, h - inputTop);

        if (!windowManager.isFrontmost(toolbarWindow))
            windowManager.bringToFront(toolbarWindow);
    }
}
