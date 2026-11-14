package application.bootstrap.oceanpipeline.exposuremanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class ExposureProbeBranch extends BranchPackage {

    /*
     * Probes one exposure cell: an even lattice of points across the cell,
     * each asked whether the sea covers it through the same coarse surface
     * query distant macro terrain is built from. The count of open-water
     * points is the cell's share of open water. A pure function of the world
     * seed and the cell, so a cell is probed once and cached.
     */

    // Internal
    private WorldGenerationManager worldGenerationManager;

    // Scratch
    private final BiomeBlendStruct blendScratch = new BiomeBlendStruct();

    // Internal \\

    @Override
    protected void get() {
        this.worldGenerationManager = get(WorldGenerationManager.class);
    }

    // Probe \\

    byte probeOpenWater(WorldHandle worldHandle, int wrappedCellX, int wrappedCellZ) {

        int probesPerAxis = EngineSetting.OCEAN_EXPOSURE_PROBES_PER_AXIS;
        double cellBlocks = EngineSetting.OCEAN_EXPOSURE_CELL_BLOCKS;
        double stepBlocks = cellBlocks / probesPerAxis;
        double originX = wrappedCellX * cellBlocks;
        double originZ = wrappedCellZ * cellBlocks;
        int openCount = 0;

        for (int z = 0; z < probesPerAxis; z++) {
            for (int x = 0; x < probesPerAxis; x++) {

                boolean open = worldGenerationManager.sampleOpenWater(
                        worldHandle,
                        originX + (x + 0.5) * stepBlocks,
                        originZ + (z + 0.5) * stepBlocks,
                        blendScratch);

                if (open)
                    openCount++;
            }
        }

        return (byte) openCount;
    }
}
