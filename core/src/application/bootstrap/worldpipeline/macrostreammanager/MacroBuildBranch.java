package application.bootstrap.worldpipeline.macrostreammanager;

import application.bootstrap.mappipeline.util.MapShadeUtility;
import application.bootstrap.worldpipeline.macrochunk.MacroChunkInstance;
import application.bootstrap.worldpipeline.macrochunk.MacroDataSyncContainer;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSurfaceSampleStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

public class MacroBuildBranch extends BranchPackage {

    /*
     * Async — builds a macro's mesh on the MacroStreaming pool, away from
     * chunk streaming. The tile's lattice is sampled straight from the terrain
     * through WorldGenerationManager.sampleSurface(), at the resolution its
     * distance calls for: the ground with its detail, down to the sea floor,
     * colored from the average albedo of the blocks a chunk would dress it
     * with, and where the sea stands over it, which becomes the tile's patch
     * of the open water mask. Still water at its own level is no part of the
     * sea's plane, so the lattice rises to its surface and wears its water,
     * shaded by depth as the map shades it. No block, neighbor or chunk is
     * ever touched.
     * The target is read on the main thread when the build is reserved,
     * sampling runs outside the macro's lock since a reserved build pins the
     * macro, and only the assembly into its shared buffers runs under it.
     */

    // Internal
    private ThreadHandle threadHandle;
    private WorldGenerationManager worldGenerationManager;
    private MacroMeshBranch macroMeshBranch;
    private MacroBuildAsyncContainer macroBuildAsyncContainer;

    // Settings
    private float tileSizeBlocks;
    private float surfaceOffsetBlocks;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.macroBuildAsyncContainer = create(MacroBuildAsyncContainer.class);

        // Settings
        this.tileSizeBlocks = EngineSetting.MACRO_TILE_SIZE_BLOCKS;
        this.surfaceOffsetBlocks = EngineSetting.MACRO_SURFACE_OFFSET_BLOCKS;
    }

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.MACRO_STREAMING_THREAD_NAME);
        this.worldGenerationManager = get(WorldGenerationManager.class);
        this.macroMeshBranch = get(MacroMeshBranch.class);
    }

    // Build \\

    public void buildMacro(MacroChunkInstance macro) {

        MacroDataSyncContainer sync = macro.getMacroDataSyncContainer();
        WorldHandle worldHandle = macro.getWorldHandle();
        long coordinate = macro.getCoordinate();
        int cellsPerSide = macro.getTargetCellsPerSide();

        executeAsync(threadHandle, () -> {

            MacroBuildAsyncContainer scratch = macroBuildAsyncContainer.getInstance();

            try {
                scratch.cellsPerSide = cellsPerSide;
                sampleLattice(scratch, worldHandle, coordinate);

                sync.acquire();
                try {
                    macroMeshBranch.assembleLand(scratch, sync.getVertices(), sync.getIndices());
                    macroMeshBranch.assembleWaterMask(scratch, sync.getWaterMask());
                    sync.markBuilt(cellsPerSide);
                } finally {
                    sync.release();
                }
            } finally {
                sync.endWork();
                scratch.reset();
            }
        });
    }

    // Lattice \\

    private void sampleLattice(MacroBuildAsyncContainer scratch, WorldHandle worldHandle, long coordinate) {

        double originX = (double) Coordinate2Long.unpackX(coordinate) * EngineSetting.CHUNK_SIZE;
        double originZ = (double) Coordinate2Long.unpackY(coordinate) * EngineSetting.CHUNK_SIZE;
        int samplesPerSide = scratch.getSamplesPerSide();
        float cellSizeBlocks = tileSizeBlocks / scratch.cellsPerSide;
        TerrainSurfaceSampleStruct sample = scratch.sample;

        scratch.minHeightBlocks = Float.MAX_VALUE;
        scratch.openWaterCount = 0;

        for (int z = 0; z < samplesPerSide; z++) {
            for (int x = 0; x < samplesPerSide; x++) {

                int index = z * samplesPerSide + x;

                worldGenerationManager.sampleSurface(
                        worldHandle,
                        originX + x * cellSizeBlocks,
                        originZ + z * cellSizeBlocks,
                        sample);

                boolean lakeWater = sample.isLakeWater();
                int topColor = resolveTopColor(sample);
                float height = (lakeWater ? sample.getWaterSurfaceBlocks() : sample.getGroundHeightBlocks())
                        + surfaceOffsetBlocks;

                scratch.heightBlocks[index] = height;
                scratch.topColors[index] = topColor;
                scratch.sideColors[index] = lakeWater ? topColor : sample.getSideColor();
                scratch.openWater[index] = sample.isOpenWater();
                scratch.minHeightBlocks = Math.min(scratch.minHeightBlocks, height);

                if (sample.isOpenWater())
                    scratch.openWaterCount++;
            }
        }
    }

    // Still water wears the map's water shade over its floor; everything else its own top
    private int resolveTopColor(TerrainSurfaceSampleStruct sample) {

        if (!sample.isLakeWater())
            return sample.getTopColor();

        return MapShadeUtility.shadeWater(
                sample.getTopColor(), sample.getWaterSurfaceBlocks() - sample.getGroundHeightBlocks());
    }
}
