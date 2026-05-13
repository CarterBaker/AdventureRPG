package application.bootstrap.menupipeline.canvassystem;

import application.bootstrap.menupipeline.menu.MenuInstance;
import engine.root.SystemPackage;

public class CanvasAreaSystem extends SystemPackage {

    /*
     * Writes the computed screen rect for the single canvas_area element each
     * menu is permitted to define directly onto the MenuInstance. Updated every
     * frame by MenuRenderSystem.
     *
     * The internal map is gone — canvas bounds belong on the menu, not in a
     * parallel structure. Any caller that holds a MenuInstance reference reads
     * the typed accessors directly. The manager no longer needs getCanvas().
     */

    // Management \\

    public void register(MenuInstance menu, int x, int y, int w, int h) {
        menu.setCanvas(x, y, w, h);
    }

    public void unregister(MenuInstance menu) {
        menu.clearCanvas();
    }
}