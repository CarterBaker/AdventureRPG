package editor.runtime;

import application.bootstrap.menupipeline.canvas.CanvasInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.SystemPackage;

public class EditorDockSystem extends SystemPackage {

    /*
     * Owns the base menu of the editor OS window its context is paired with —
     * background and dock canvas — and publishes the measured dock canvas to
     * TabManager every frame. Every editor window runs this same system; the
     * context picks the base menu through openBaseMenu(), so the main window's
     * dock leaves room for its toolbar and a secondary window's dock fills the
     * whole window.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private TabManager tabManager;

    // Menus
    private MenuInstance baseMenu;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.tabManager = get(TabManager.class);
    }

    @Override
    protected void dispose() {
        menuManager.closeMenu(baseMenu);
        menuManager.setMenuTargetFbo(context.getWindow(), null);
    }

    // Management \\

    public void openBaseMenu(String menuName) {

        WindowInstance osWindow = context.getWindow();

        menuManager.setMenuTargetFbo(osWindow, fboManager.cloneFbo(RuntimeSetting.FBO_UI, osWindow));
        this.baseMenu = menuManager.openMenu(menuName, osWindow);
    }

    // Update \\

    @Override
    protected void update() {

        CanvasInstance canvas = baseMenu.getCanvas();

        if (canvas == null)
            return;

        tabManager.setDockRect(context.getWindow(), canvas.getX(), canvas.getY(), canvas.getW(), canvas.getH());
    }

    // Accessible \\

    public float getDockTop() {
        CanvasInstance canvas = baseMenu.getCanvas();
        return canvas != null ? canvas.getY() + canvas.getH() : 0f;
    }
}
