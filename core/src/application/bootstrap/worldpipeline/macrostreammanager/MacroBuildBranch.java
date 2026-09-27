package application.bootstrap.worldpipeline.macrostreammanager;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.macrochunk.MacroChunkInstance;
import application.bootstrap.worldpipeline.macrochunk.MacroDataSyncContainer;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.graphics.color.Color;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

public class MacroBuildBranch extends BranchPackage {

    /*
     * Async — builds a macro's mesh on the MacroStreaming pool, away from
     * chunk streaming. The tile's lattice is sampled straight from the terrain
     * noise, ground or sea surface only, and colored from the biome field's
     * map colors, so no block, neighbor or chunk is ever touched. Sampling
     * runs outside the macro's lock since a reserved build pins the macro;
     * only the mesh assembly into its shared lists runs under it.
     */

    // Internal
    private ThreadHandle threadHandle;
    private WorldGenerationManager worldGenerationManager;
    private BiomeManager biomeManager;
    private MacroMeshBranch macroMeshBranch;
    private MacroBuildAsyncContainer macroBuildAsyncContainer;

    // Settings
    private int samplesPerSide;
    private float cellSizeBlocks;
    private float surfaceOffsetBlocks;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.macroBuildAsyncContainer = create(MacroBuildAsyncContainer.class);

        // Settings
        this.samplesPerSide = MacroBuildAsyncContainer.SAMPLES_PER_SIDE;
        this.cellSizeBlocks = (float) (EngineSetting.MACRO_CHUNK_SIZE * EngineSetting.CHUNK_SIZE)
                / EngineSetting.MACRO_CELLS_PER_SIDE;
        this.surfaceOffsetBlocks = EngineSetting.MACRO_SURFACE_OFFSET_BLOCKS;
    }

    @Override
    protected void get() {

        // Internal
        this.threadHandle = getThreadHandleFromThreadName(EngineSetting.MACRO_STREAMING_THREAD_NAME);
        this.worldGenerationManager = get(WorldGenerationManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.macroMeshBranch = get(MacroMeshBranch.class);
    }

    // Build \\

    public void buildMacro(MacroChunkInstance macro) {

        MacroDataSyncContainer sync = macro.getMacroDataSyncContainer();
        WorldHandle worldHandle = macro.getWorldHandle();
        long coordinate = macro.getCoordinate();

        executeAsync(threadHandle, () -> {

            MacroBuildAsyncContainer scratch = macroBuildAsyncContainer.getInstance();

            try {
                sampleLattice(scratch, worldHandle, coordinate);

                sync.acquire();
                try {
                    macroMeshBranch.assembleMesh(scratch, sync.getVertices(), sync.getIndices());
                    sync.markBuilt();
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
        BiomeBlendStruct blend = scratch.blend;

        for (int z = 0; z < samplesPerSide; z++) {
            for (int x = 0; x < samplesPerSide; x++) {

                int index = z * samplesPerSide + x;

                scratch.heightBlocks[index] = worldGenerationManager.sampleSurfaceHeight(
                        worldHandle,
                        originX + x * cellSizeBlocks,
                        originZ + z * cellSizeBlocks,
                        blend) + surfaceOffsetBlocks;

                scratch.packedColors[index] = resolvePackedColor(blend);
            }
        }
    }

    // Color \\

    private float resolvePackedColor(BiomeBlendStruct blend) {

        float red = 0f;
        float green = 0f;
        float blue = 0f;
        float weightSum = 0f;

        for (int i = 0; i < blend.getCount(); i++) {

            int mapColor = biomeManager.getMapColor(blend.getBiome(i));

            if (mapColor == EngineSetting.BIOME_MAP_COLOR_UNDEFINED)
                continue;

            float weight = blend.getWeight(i);

            red += ((mapColor >> 16) & 0xFF) * weight;
            green += ((mapColor >> 8) & 0xFF) * weight;
            blue += (mapColor & 0xFF) * weight;
            weightSum += weight;
        }

        if (weightSum <= 0f)
            return packColor(blend.getDominantBiome());

        float inverse = 1f / weightSum;

        return packChannels(Math.round(red * inverse), Math.round(green * inverse), Math.round(blue * inverse));
    }

    private float packColor(BiomeHandle biomeHandle) {

        Color color = biomeHandle.getBiomeColor();

        return packChannels(
                Math.round(color.r * EngineSetting.COLOR_CHANNEL_BYTE_MAX),
                Math.round(color.g * EngineSetting.COLOR_CHANNEL_BYTE_MAX),
                Math.round(color.b * EngineSetting.COLOR_CHANNEL_BYTE_MAX));
    }

    private float packChannels(int red, int green, int blue) {
        return (float) ((red << 16) | (green << 8) | blue);
    }
}
