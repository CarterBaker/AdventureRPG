package application.bootstrap.worldpipeline.cavebiome;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class CaveSpeleothemStruct extends StructPackage {

    /*
     * The stalactites and stalagmites a cave biome grows: the block they are
     * made of, the share of open ceiling and floor cells a small one hangs
     * from or rises on, the longest a small one grows, the share of tall
     * chambers a giant cone or column stands in and the widest a giant
     * spreads at its base. NONE grows nothing.
     */

    public static final CaveSpeleothemStruct NONE = new CaveSpeleothemStruct(
            null,
            EngineSetting.DEFAULT_SPELEOTHEM_STALACTITES,
            EngineSetting.DEFAULT_SPELEOTHEM_STALAGMITES,
            EngineSetting.DEFAULT_SPELEOTHEM_MAX_LENGTH_BLOCKS,
            EngineSetting.DEFAULT_SPELEOTHEM_GIANTS,
            EngineSetting.DEFAULT_SPELEOTHEM_MAX_GIANT_RADIUS_BLOCKS);

    private final String blockName;
    private final float stalactites;
    private final float stalagmites;
    private final int maxLengthBlocks;
    private final float giants;
    private final int maxGiantRadiusBlocks;

    public CaveSpeleothemStruct(
            String blockName,
            float stalactites,
            float stalagmites,
            int maxLengthBlocks,
            float giants,
            int maxGiantRadiusBlocks) {

        this.blockName = blockName;
        this.stalactites = stalactites;
        this.stalagmites = stalagmites;
        this.maxLengthBlocks = maxLengthBlocks;
        this.giants = giants;
        this.maxGiantRadiusBlocks = maxGiantRadiusBlocks;
    }

    // Accessible \\

    public String getBlockName() {
        return blockName;
    }

    public boolean hasSpeleothems() {
        return blockName != null;
    }

    public float getStalactites() {
        return stalactites;
    }

    public float getStalagmites() {
        return stalagmites;
    }

    public int getMaxLengthBlocks() {
        return maxLengthBlocks;
    }

    public float getGiants() {
        return giants;
    }

    public int getMaxGiantRadiusBlocks() {
        return maxGiantRadiusBlocks;
    }
}
