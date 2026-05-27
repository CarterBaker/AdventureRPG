package editor.runtime.editor;

import application.bootstrap.menupipeline.canvas.CanvasInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import engine.root.SystemPackage;

public class SecondaryTabCompositorSystem extends SystemPackage {

    /*
     * Drives dock rect updates for a secondary OS window. Mirrors
     * EditorTabCompositorSystem exactly — the only difference is the base menu
     * source and that context.getWindow() returns the secondary OS window.
     *
     * setDockRect() triggers pushRects(), which calls TabContext.placeAt() on
     * every tab. Chrome and content are positioned together in that single call.
     */

    private TabManager tabManager;
    private SecondaryMenuSystem secondaryMenuSystem;

    private float lastX;
    private float lastY;
    private float lastW;
    private float lastH;

    // Internal \\

    @Override
    protected void get() {
        tabManager = get(TabManager.class);
        secondaryMenuSystem = get(SecondaryMenuSystem.class);
    }

    @Override
    protected void update() {

        MenuInstance baseMenu = secondaryMenuSystem.getBaseMenu();
        CanvasInstance canvas = baseMenu.getCanvas();

        if (canvas == null)
            return;

        float x = canvas.getX();
        float y = canvas.getY();
        float w = canvas.getW();
        float h = canvas.getH();

        if (x != lastX || y != lastY || w != lastW || h != lastH) {
            lastX = x;
            lastY = y;
            lastW = w;
            lastH = h;
            tabManager.setDockRect(context.getWindow(), x, y, w, h);
        }
    }
}