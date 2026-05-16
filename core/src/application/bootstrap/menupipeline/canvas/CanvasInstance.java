package application.bootstrap.menupipeline.canvas;

import engine.root.InstancePackage;

public class CanvasInstance extends InstancePackage {

    /*
     * Computed screen rect for a menu's canvas_area element, stored in
     * OpenGL space (Y+ up, origin bottom-left). Written every frame by
     * MenuRenderSystem. Null on MenuInstance means this menu has no canvas.
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