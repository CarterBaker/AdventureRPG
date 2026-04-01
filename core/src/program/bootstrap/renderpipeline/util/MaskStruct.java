package program.bootstrap.renderpipeline.util;

import program.core.engine.StructPackage;

public class MaskStruct extends StructPackage {

    /*
     * Scissor rectangle used by the menu mask stack. Pooled in MenuManager —
     * mutated in place via set() rather than allocated per frame.
     */

    // Internal
    private int x;
    private int y;
    private int w;
    private int h;

    // Constructor \\

    public MaskStruct() {
    }

    // Mutation \\

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