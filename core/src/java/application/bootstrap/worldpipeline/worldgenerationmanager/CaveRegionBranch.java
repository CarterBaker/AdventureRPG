package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.util.GlobeUtility;
import application.bootstrap.worldpipeline.util.SurfacePointStruct;
import application.bootstrap.worldpipeline.util.TerrainCarveUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
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
            SurfacePointStruct home,
            CaveFrameStruct frame,
            long frameOriginX,
            long frameOriginZ,
            float frameX,
            int worldY,
            float frameZ,
            int groundHeight) {

        float pointY = worldY + 0.5f;

        caveCarveBranch.resolveFramePoint(frame, frameX, pointY, frameZ);

        double warpedX = frameOriginX + frameX
                + caveCarveBranch.sampleFramePoint(frame, TerrainCarveUtility.CHANNEL_SPAGHETTI_A) * warpBlocks;
        double warpedY = pointY
                + caveCarveBranch.sampleFramePoint(frame, TerrainCarveUtility.CHANNEL_CHEESE) * warpVerticalBlocks;
        double warpedZ = frameOriginZ + frameZ
                + caveCarveBranch.sampleFramePoint(frame, TerrainCarveUtility.CHANNEL_SPAGHETTI_B) * warpBlocks;

        long cell = resolveRegionCell(home, warpedX, warpedZ);
        int cellX = Coordinate2Long.unpackX(cell);
        int cellY = Math.floorDiv((int) Math.floor(warpedY), cellHeightBlocks);
        int cellZ = Coordinate2Long.unpackY(cell);

        TerrainCaveEntryStruct entry = pickEntry(
                worldHandle, resolveCellSurface(worldHandle, cellX, cellZ), cellX, cellY, cellZ);

        if (entry == null || !entry.reaches(worldY, groundHeight))
            return null;

        return entry.profile;
    }

    // Every cave profile any region reaching the chunk can hold, each once
    void collectProfiles(
            SurfacePointStruct home,
            long chunkOriginX,
            long chunkOriginZ,
            ObjectArrayList<TerrainCaveProfileStruct> outProfiles) {

        outProfiles.clear();

        double fromX = chunkOriginX - warpBlocks;
        double toX = chunkOriginX + EngineSetting.CHUNK_SIZE + warpBlocks;
        double fromZ = chunkOriginZ - warpBlocks;
        double toZ = chunkOriginZ + EngineSetting.CHUNK_SIZE + warpBlocks;

        addCellProfiles(home.getWorldHandle(), resolveRegionCell(home, fromX, fromZ), outProfiles);
        addCellProfiles(home.getWorldHandle(), resolveRegionCell(home, toX, fromZ), outProfiles);
        addCellProfiles(home.getWorldHandle(), resolveRegionCell(home, fromX, toZ), outProfiles);
        addCellProfiles(home.getWorldHandle(), resolveRegionCell(home, toX, toZ), outProfiles);
    }

    private void addCellProfiles(
            WorldHandle worldHandle,
            long cell,
            ObjectArrayList<TerrainCaveProfileStruct> outProfiles) {

        TerrainCaveEntryStruct[] entries = resolveCellSurface(
                worldHandle, Coordinate2Long.unpackX(cell), Coordinate2Long.unpackY(cell)).caveEntries;

        for (int i = 0; i < entries.length; i++)
            if (!outProfiles.contains(entries[i].profile))
                outProfiles.add(entries[i].profile);
    }

    // Cells \\

    // The region cell a place in a home's coordinates falls in, packed: its wrapped cell on a torus, on a globe the
    // cell of the face it truly lies on, counted from that face's origin
    private long resolveRegionCell(SurfacePointStruct home, double x, double z) {

        WorldHandle worldHandle = home.getWorldHandle();

        if (!worldHandle.isGlobe())
            return Coordinate2Long.pack(
                    resolveCell(x, worldHandle.getWorldScale().x), resolveCell(z, worldHandle.getWorldScale().y));

        home.resolve(x, z);

        int cellsPerFace = resolveCellsPerFace(worldHandle);

        return Coordinate2Long.pack(
                (int) GlobeUtility.faceOriginX(home.getFace(), cellsPerFace)
                        + Math.min((int) (home.getFaceU() / cellBlocks), cellsPerFace - 1),
                Math.min((int) (home.getFaceV() / cellBlocks), cellsPerFace - 1));
    }

    private int resolveCellsPerFace(WorldHandle worldHandle) {
        return Math.max(1, GlobeUtility.getFaceBlocks(worldHandle) / cellBlocks);
    }

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

        if (worldHandle.isGlobe()) {

            int cellsPerFace = resolveCellsPerFace(worldHandle);
            int face = GlobeUtility.faceOf(cellX, cellsPerFace);

            centerX = GlobeUtility.faceOriginX(face, GlobeUtility.getFaceBlocks(worldHandle))
                    + (cellX - GlobeUtility.faceOriginX(face, cellsPerFace) + 0.5) * cellBlocks;
        }

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
