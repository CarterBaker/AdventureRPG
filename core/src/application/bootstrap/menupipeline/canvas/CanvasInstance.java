package application.bootstrap.menupipeline.canvas;

import engine.root.InstancePackage;

public class CanvasInstance extends InstancePackage {

    /*
     * Computed layout-space rect for a menu's canvas_area element, stored in
     * the owning window's top-left coordinate space (Y+ down). Written every
     * frame by MenuRenderSystem after resolving the element's position and size.
     * Null on MenuInstance means this menu has no canvas.
     */

    private int x;
    private int y;
    private int w;
    private int h;

    // Internal \\

    public void set(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    // Accessible \\

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }
}