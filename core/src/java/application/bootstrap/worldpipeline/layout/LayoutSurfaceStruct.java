package application.bootstrap.worldpipeline.layout;

import engine.root.StructPackage;

public class LayoutSurfaceStruct extends StructPackage {

    /*
     * Output container for what layouts show at one point seen from afar:
     * whether a road recolors the land or something rises above it, the
     * height its top stands at in blocks, and the packed colors its top and
     * sides are drawn in. Written in place — never allocated per sample.
     */

    // Internal
    private LayoutSurfaceKind kind;
    private float heightBlocks;
    private int topColor;
    private int sideColor;

    // Constructor \\

    public LayoutSurfaceStruct() {
        clear();
    }

    // Management \\

    public void clear() {
        this.kind = LayoutSurfaceKind.NONE;
        this.heightBlocks = -Float.MAX_VALUE;
        this.topColor = 0;
        this.sideColor = 0;
    }

    // Keeps what stands highest, anything raised standing over a road on the ground
    public void offer(LayoutSurfaceKind kind, float heightBlocks, int topColor, int sideColor) {

        boolean better = kind.ordinal() > this.kind.ordinal()
                || kind == this.kind && heightBlocks > this.heightBlocks;

        if (!better)
            return;

        this.kind = kind;
        this.heightBlocks = heightBlocks;
        this.topColor = topColor;
        this.sideColor = sideColor;
    }

    // Accessible \\

    public LayoutSurfaceKind getKind() {
        return kind;
    }

    public boolean isFound() {
        return kind != LayoutSurfaceKind.NONE;
    }

    public boolean isRaised() {
        return kind == LayoutSurfaceKind.RAISED;
    }

    public float getHeightBlocks() {
        return heightBlocks;
    }

    public int getTopColor() {
        return topColor;
    }

    public int getSideColor() {
        return sideColor;
    }
}
