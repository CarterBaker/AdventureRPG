package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import engine.root.StructPackage;

public class TerrainSurfaceSampleStruct extends StructPackage {

    /*
     * One point of terrain as distant views draw it, filled in place by
     * WorldGenerationManager.sampleSurface(): the ground height with its
     * detail, whether the sea stands over it, and the colors of its top and of
     * its slopes, blended across every biome reaching it from the average
     * albedo of the blocks a chunk would dress it with. The biome blend it was
     * sampled through stays readable for anything that names the biome.
     */

    // Biome Field
    private final BiomeBlendStruct blend = new BiomeBlendStruct();

    // Ground
    float groundHeightBlocks;
    boolean openWater;

    // Color
    int topColor;
    int sideColor;

    // Accessible \\

    public BiomeBlendStruct getBlend() {
        return blend;
    }

    public float getGroundHeightBlocks() {
        return groundHeightBlocks;
    }

    public boolean isOpenWater() {
        return openWater;
    }

    public int getTopColor() {
        return topColor;
    }

    public int getSideColor() {
        return sideColor;
    }
}
