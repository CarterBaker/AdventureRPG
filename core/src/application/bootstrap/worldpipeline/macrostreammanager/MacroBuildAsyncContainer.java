package application.bootstrap.worldpipeline.macrostreammanager;

import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;

public class MacroBuildAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch for one macro build, sized for the finest lattice
     * a tile can take: the ground height, packed top and side colors and open
     * sea flag at every lattice point, the lattice's lowest ground, and the
     * surface sample each point is read through. Filled by MacroBuildBranch
     * and read by MacroMeshBranch, so a build allocates nothing.
     */

    static final int MAX_SAMPLES_PER_SIDE = EngineSetting.MACRO_CELLS_PER_SIDE_MAX + 1;
    static final int MAX_SAMPLE_COUNT = MAX_SAMPLES_PER_SIDE * MAX_SAMPLES_PER_SIDE;

    // Lattice
    int cellsPerSide;
    float[] heightBlocks;
    float[] topColors;
    float[] sideColors;
    boolean[] openWater;
    float minHeightBlocks;
    int openWaterCount;

    // Surface
    TerrainSurfaceSampleStruct sample;

    @Override
    protected void create() {
        this.heightBlocks = new float[MAX_SAMPLE_COUNT];
        this.topColors = new float[MAX_SAMPLE_COUNT];
        this.sideColors = new float[MAX_SAMPLE_COUNT];
        this.openWater = new boolean[MAX_SAMPLE_COUNT];
        this.sample = new TerrainSurfaceSampleStruct();
    }

    @Override
    public void reset() {
        sample.getBlend().reset();
        this.openWaterCount = 0;
    }

    // Lattice \\

    int getSamplesPerSide() {
        return cellsPerSide + 1;
    }

    int getSampleCount() {
        return (cellsPerSide + 1) * (cellsPerSide + 1);
    }
}
