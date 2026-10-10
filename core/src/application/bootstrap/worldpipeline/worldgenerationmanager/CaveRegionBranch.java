package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.util.TerrainCarveUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CaveRegionBranch extends BranchPackage {

    /*
     * Async — decides which cave biome lines each stretch of the underground.
     * The underground is cut into broad, squat cells whose walls wander with
     * the cave field, and each cell belongs to the surface biome standing
     * over its centre: one roll per cell picks among the cave biomes that
     * biome holds beneath it by their chances, or none. A block keeps its
     * cell's cave biome only within the band of heights and depth below its
     * own ground the cave biome is listed for. Pure in seed and position, so
     * every chunk agrees on every region.
     */

    // Internal
    private WorldGenerationManager worldGenerationManager;
    private BiomeManager biomeManager;
    private CaveCarveBranch caveCarveBranch;

    // Settings
    private int cellBlocks;
    private int cellHeightBlocks;
    private float warpBlocks;
    private float warpVerticalBlocks;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.cellBlocks = EngineSetting.CAVE_REGION_CELL_BLOCKS;
        this.cellHeightBlocks = EngineSetting.CAVE_REGION_CELL_HEIGHT_BLOCKS;
        this.warpBlocks = EngineSetting.CAVE_REGION_WARP_BLOCKS;
        this.warpVerticalBlocks = EngineSetting.CAVE_REGION_WARP_VERTICAL_BLOCKS;
    }

    @Override
    protected void get() {
        this.worldGenerationManager = get(WorldGenerationManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.caveCarveBranch = get(CaveCarveBranch.class);
    }

    // Region \\

    // The cave profile lining a block, null where no cave biome does — the frame's origin lies at frameOriginX and
    // frameOriginZ in the world, and the point is read where the block's centre falls in the frame
    TerrainCaveProfileStruct resolveProfile(
            WorldHandle worldHandle,
            CaveFrameStruct frame,
            long frameOriginX,
            long frameOriginZ,
            float frameX,
            int worldY,
            float frameZ,
            int groundHeight) {

        float pointY = worldY + 0.5f;

        double warpedX = frameOriginX + frameX
                + caveCarveBranch.sampleChannel(
                        frame, TerrainCarveUtility.CHANNEL_SPAGHETTI_A, frameX, pointY, frameZ) * warpBlocks;
        double warpedY = pointY
                + caveCarveBranch.sampleChannel(
                        frame, TerrainCarveUtility.CHANNEL_CHEESE, frameX, pointY, frameZ) * warpVerticalBlocks;
        double warpedZ = frameOriginZ + frameZ
                + caveCarveBranch.sampleChannel(
                        frame, TerrainCarveUtility.CHANNEL_SPAGHETTI_B, frameX, pointY, frameZ) * warpBlocks;

        int cellX = resolveCell(warpedX, worldHandle.getWorldScale().x);
        int cellY = Math.floorDiv((int) Math.floor(warpedY), cellHeightBlocks);
        int cellZ = resolveCell(warpedZ, worldHandle.getWorldScale().y);

        TerrainCaveEntryStruct entry = pickEntry(
                worldHandle, resolveCellSurface(worldHandle, cellX, cellZ), cellX, cellY, cellZ);

        if (entry == null || !entry.reaches(worldY, groundHeight))
            return null;

        return entry.profile;
    }

    // Every cave profile any region reaching the chunk can hold, each once
    void collectProfiles(
            WorldHandle worldHandle,
            long chunkOriginX,
            long chunkOriginZ,
            ObjectArrayList<TerrainCaveProfileStruct> outProfiles) {

        outProfiles.clear();

        int fromX = resolveCell(chunkOriginX - warpBlocks, worldHandle.getWorldScale().x);
        int toX = resolveCell(chunkOriginX + EngineSetting.CHUNK_SIZE + warpBlocks, worldHandle.getWorldScale().x);
        int fromZ = resolveCell(chunkOriginZ - warpBlocks, worldHandle.getWorldScale().y);
        int toZ = resolveCell(chunkOriginZ + EngineSetting.CHUNK_SIZE + warpBlocks, worldHandle.getWorldScale().y);

        addCellProfiles(worldHandle, fromX, fromZ, outProfiles);
        addCellProfiles(worldHandle, toX, fromZ, outProfiles);
        addCellProfiles(worldHandle, fromX, toZ, outProfiles);
        addCellProfiles(worldHandle, toX, toZ, outProfiles);
    }

    private void addCellProfiles(
            WorldHandle worldHandle,
            int cellX,
            int cellZ,
            ObjectArrayList<TerrainCaveProfileStruct> outProfiles) {

        TerrainCaveEntryStruct[] entries = resolveCellSurface(worldHandle, cellX, cellZ).caveEntries;

        for (int i = 0; i < entries.length; i++)
            if (!outProfiles.contains(entries[i].profile))
                outProfiles.add(entries[i].profile);
    }

    // Cells \\

    // A wrapped coordinate's cell; the last cell absorbs whatever part of a cell the world's width leaves over
    private int resolveCell(double coordinate, int worldSizeBlocks) {

        int cellCount = Math.max(1, worldSizeBlocks / cellBlocks);
        long wrapped = Math.floorMod((long) Math.floor(coordinate), (long) worldSizeBlocks);

        return (int) Math.min(wrapped / cellBlocks, cellCount - 1);
    }

    // One roll per cell walks the surface biome's cave biomes by their chances, landing on none past their total
    private TerrainCaveEntryStruct pickEntry(
            WorldHandle worldHandle,
            TerrainSurfaceProfileStruct surface,
            int cellX,
            int cellY,
            int cellZ) {

        TerrainCaveEntryStruct[] entries = surface.caveEntries;

        if (entries.length == 0)
            return null;

        float roll = TerrainCarveUtility.rollCell(
                worldHandle.getSeed(), EngineSetting.CAVE_REGION_SEED_SALT, cellX, cellY, cellZ);

        for (int i = 0; i < entries.length; i++) {

            if (roll < entries[i].chance)
                return entries[i];

            roll -= entries[i].chance;
        }

        return null;
    }

    // The surface biome over a cell's centre, memoized per worker
    private TerrainSurfaceProfileStruct resolveCellSurface(WorldHandle worldHandle, int cellX, int cellZ) {

        CaveProbeAsyncContainer probe = worldGenerationManager.getCaveProbeContainer().getInstance();
        long key = ((long) cellX << Integer.SIZE) | (cellZ & 0xFFFFFFFFL);
        int revision = biomeManager.getRevision();

        for (int slot = 0; slot < CaveProbeAsyncContainer.REGION_CACHE_SIZE; slot++)
            if (probe.regionCellWorlds[slot] == worldHandle
                    && probe.regionCellKeys[slot] == key
                    && probe.regionCellRevisions[slot] == revision)
                return probe.regionCellProfiles[slot];

        double centerX = (double) cellX * cellBlocks + cellBlocks * 0.5;
        double centerZ = (double) cellZ * cellBlocks + cellBlocks * 0.5;

        biomeManager.sampleBiomeField(worldHandle, centerX, centerZ, probe.blend);

        TerrainSurfaceProfileStruct profile = worldGenerationManager.resolveSurfaceProfile(
                probe.blend.getDominantBiome());
        int slot = probe.regionCellNext;

        probe.regionCellNext = (slot + 1) % CaveProbeAsyncContainer.REGION_CACHE_SIZE;
        probe.regionCellKeys[slot] = key;
        probe.regionCellWorlds[slot] = worldHandle;
        probe.regionCellRevisions[slot] = revision;
        probe.regionCellProfiles[slot] = profile;

        return profile;
    }
}
