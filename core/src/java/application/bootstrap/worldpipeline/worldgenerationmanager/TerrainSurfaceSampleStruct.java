package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.util.TerrainFeatureStruct;
import engine.root.StructPackage;

public class TerrainSurfaceSampleStruct extends StructPackage {

    /*
     * One point of terrain as distant views draw it, filled in place by
     * WorldGenerationManager.sampleSurface(): the ground height with its
     * detail, whether the sea or still water stands over it and the height of
     * that water's surface, and the colors of its top and of its slopes,
     * blended across every biome reaching it from the average albedo of the
     * blocks a chunk would dress it with. The biome blend and terrain features
     * it was sampled through stay readable for anything that names the biome.
     */

    // Biome Field
    private final BiomeBlendStruct blend = new BiomeBlendStruct();
    private final TerrainFeatureStruct features = new TerrainFeatureStruct();

    // Ground
    float groundHeightBlocks;
    boolean openWater;
    boolean lakeWater;
    float waterSurfaceBlocks;

    // Color
    int topColor;
    int sideColor;

    // Accessible \\

    public BiomeBlendStruct getBlend() {
        return blend;
    }

    public TerrainFeatureStruct getFeatures() {
        return features;
    }

    public float getGroundHeightBlocks() {
        return groundHeightBlocks;
    }

    public boolean isOpenWater() {
        return openWater;
    }

    public boolean isLakeWater() {
        return lakeWater;
    }

    // Measured like the ground: the highest water block standing over it, or the ground itself where none does
    public float getWaterSurfaceBlocks() {
        return waterSurfaceBlocks;
    }

    public int getTopColor() {
        return topColor;
    }

    public int getSideColor() {
        return sideColor;
    }
}
