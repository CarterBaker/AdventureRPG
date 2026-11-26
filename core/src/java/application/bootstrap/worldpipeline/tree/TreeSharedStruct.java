package application.bootstrap.worldpipeline.tree;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class TreeSharedStruct extends StructPackage {

    /*
     * The one far model a tree species shares between all its trees at every
     * coarse LOD level, as a forest tool bakes each species' far levels once
     * and stands every tree on them: the tree its LOD seed grows at full age,
     * drawn at each level in the bark and leaf vertex layouts, in blocks from
     * the centre of its root, and the height it stands to, which every tree
     * drawing it scales it by. Level 0 is never shared and holds nothing.
     * Immutable once built, so any thread reads it without a lock.
     */

    // Seed
    private final long seed;

    // Levels
    private final float[][] bark;
    private final float[][] leaves;

    // Height — blocks
    private final float height;

    // Constructor \\

    public TreeSharedStruct(long seed, float[][] bark, float[][] leaves, float height) {

        // Seed
        this.seed = seed;

        // Levels
        this.bark = bark;
        this.leaves = leaves;

        // Height
        this.height = Math.max(height, EngineSetting.TREE_MIN_SEGMENT_BLOCKS);
    }

    // Accessible \\

    public long getSeed() {
        return seed;
    }

    public float[] getBark(int level) {
        return bark[level];
    }

    public float[] getLeaves(int level) {
        return leaves[level];
    }

    public float getHeight() {
        return height;
    }
}
