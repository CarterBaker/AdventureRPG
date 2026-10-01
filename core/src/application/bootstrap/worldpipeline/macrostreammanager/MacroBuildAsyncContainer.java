package application.bootstrap.worldpipeline.macrostreammanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;

public class MacroBuildAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for one macro build, sized for the finest lattice
     * a tile can take: the surface height and packed color at every lattice
     * point, the lattice's lowest ground, and the biome blend each point is
     * sampled through. Filled by MacroBuildBranch and read by MacroMeshBranch,
     * so a build allocates nothing.
     */

    static final int MAX_SAMPLES_PER_SIDE = EngineSetting.MACRO_CELLS_PER_SIDE_MAX + 1;
    static final int MAX_SAMPLE_COUNT = MAX_SAMPLES_PER_SIDE * MAX_SAMPLES_PER_SIDE;

    // Lattice
    int cellsPerSide;
    float[] heightBlocks;
    float[] packedColors;
    float minHeightBlocks;

    // Biome Field
    BiomeBlendStruct blend;

    @Override
    protected void create() {
        this.heightBlocks = new float[MAX_SAMPLE_COUNT];
        this.packedColors = new float[MAX_SAMPLE_COUNT];
        this.blend = new BiomeBlendStruct();
    }

    @Override
    public void reset() {
        blend.reset();
    }

    // Lattice \\

    int getSamplesPerSide() {
        return cellsPerSide + 1;
    }

    int getSampleCount() {
        return (cellsPerSide + 1) * (cellsPerSide + 1);
    }
}
