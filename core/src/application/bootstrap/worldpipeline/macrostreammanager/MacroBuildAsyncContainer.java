package application.bootstrap.worldpipeline.macrostreammanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;

public class MacroBuildAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for one macro build — the surface height and packed
     * color at every lattice point of the tile, and the biome blend each point
     * is sampled through. Filled by MacroBuildBranch and read by
     * MacroMeshBranch, so a build allocates nothing.
     */

    static final int SAMPLES_PER_SIDE = EngineSetting.MACRO_CELLS_PER_SIDE + 1;
    static final int SAMPLE_COUNT = SAMPLES_PER_SIDE * SAMPLES_PER_SIDE;

    // Lattice
    float[] heightBlocks;
    float[] packedColors;

    // Biome Field
    BiomeBlendStruct blend;

    @Override
    protected void create() {
        this.heightBlocks = new float[SAMPLE_COUNT];
        this.packedColors = new float[SAMPLE_COUNT];
        this.blend = new BiomeBlendStruct();
    }

    @Override
    public void reset() {
        blend.reset();
    }
}
