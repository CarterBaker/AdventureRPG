package application.bootstrap.worldpipeline.grid;

import engine.root.StructPackage;

public class GridRangeStruct extends StructPackage {

    /*
     * How far one grid reaches: the chunk render distance its slots are laid
     * for, the tree detail distance its trees are judged by, and the reach of
     * the macro terrain beyond its rim. A render distance of zero streams no
     * chunks and a macro reach of zero no distant terrain. Grids follow a
     * shared range by reference, so a profile edited in place reaches every
     * grid following it, and keep a copy of the range their layout was last
     * laid for.
     */

    // Range
    private int renderDistance;
    private int treeDetailDistance;
    private float macroReachBlocks;

    // Management \\

    public void set(int renderDistance, int treeDetailDistance, float macroReachBlocks) {
        this.renderDistance = renderDistance;
        this.treeDetailDistance = treeDetailDistance;
        this.macroReachBlocks = macroReachBlocks;
    }

    public void copy(GridRangeStruct range) {
        set(range.renderDistance, range.treeDetailDistance, range.macroReachBlocks);
    }

    // True when a grid laid for this range needs no new layout to stand for the other
    public boolean matchesLayout(GridRangeStruct range) {
        return renderDistance == range.renderDistance && macroReachBlocks == range.macroReachBlocks;
    }

    // Accessible \\

    public int getRenderDistance() {
        return renderDistance;
    }

    public int getTreeDetailDistance() {
        return treeDetailDistance;
    }

    public float getMacroReachBlocks() {
        return macroReachBlocks;
    }
}
