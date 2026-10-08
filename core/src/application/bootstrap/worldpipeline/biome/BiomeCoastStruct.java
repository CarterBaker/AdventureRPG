package application.bootstrap.worldpipeline.biome;

import engine.root.StructPackage;

public class BiomeCoastStruct extends StructPackage {

    /*
     * How a land biome meets the sea: the height of the cliffs it raises at
     * the shore, the share of its coastline a noise mask lets them stand on,
     * how many blocks the waves undercut them at the waterline, and how
     * densely sea caves are cut into them. NONE leaves a natural shore.
     */

    public static final BiomeCoastStruct NONE = new BiomeCoastStruct(0f, 0f, 0f, 0f);

    private final float cliffHeightBlocks;
    private final float coverage;
    private final float overhangBlocks;
    private final float seaCaves;

    public BiomeCoastStruct(float cliffHeightBlocks, float coverage, float overhangBlocks, float seaCaves) {
        this.cliffHeightBlocks = cliffHeightBlocks;
        this.coverage = coverage;
        this.overhangBlocks = overhangBlocks;
        this.seaCaves = seaCaves;
    }

    // Accessible \\

    public float getCliffHeightBlocks() {
        return cliffHeightBlocks;
    }

    public float getCoverage() {
        return coverage;
    }

    public float getOverhangBlocks() {
        return overhangBlocks;
    }

    public float getSeaCaves() {
        return seaCaves;
    }
}
