package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.util.StructurePlacementUtility;
import application.bootstrap.worldpipeline.util.TerrainCarveUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class CaveLakeBranch extends BranchPackage {

    /*
     * Async — finds the underground lakes reaching a frame. The world is cut
     * into tall cells, each holding at most one lake, its basin kept far
     * enough inside the cell that no two lakes' guards ever meet. A column of
     * cells shares one footprint, read once from the ground around it: a
     * lake stands only where all of that ground is dry, away from the sea
     * and from still water on the surface, and rises high enough above the
     * dome to keep a full roof of rock. Each cell then rolls the share of
     * lakes its biomes hold and sets its waterline anywhere its basin fits,
     * so lakes lie at every height, on mountains and below sea level alike.
     * Every value is pure in seed and cell, so every chunk agrees.
     */

    // Internal
    private WorldGenerationManager worldGenerationManager;
    private BiomeManager biomeManager;

    // Settings
    private int cellBlocks;
    private int cellHeightBlocks;
    private float anchorMargin;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.cellBlocks = EngineSetting.CAVE_LAKE_CELL_BLOCKS;
        this.cellHeightBlocks = EngineSetting.CAVE_LAKE_CELL_HEIGHT_BLOCKS;
        this.anchorMargin = EngineSetting.CAVE_LAKE_MAX_RADIUS_BLOCKS + EngineSetting.CAVE_LAKE_GUARD_BLOCKS
                + EngineSetting.CAVE_LAKE_TAPER_BLOCKS;
    }

    @Override
    protected void get() {
        this.worldGenerationManager = get(WorldGenerationManager.class);
        this.biomeManager = get(BiomeManager.class);
    }

    // Collection \\

    // Every lake reaching widthBlocks from the frame's origin along each axis between minY and maxY, centred in the
    // frame's coordinates
    void collectLakes(
            WorldHandle worldHandle,
            CaveFrameStruct frame,
            long frameOriginX,
            long frameOriginZ,
            int widthBlocks,
            int minY,
            int maxY) {

        CaveProbeAsyncContainer probe = worldGenerationManager.getCaveProbeContainer().getInstance();
        int reach = (int) Math.ceil(anchorMargin);

        frame.clearLakeSites();

        StructurePlacementUtility.collectCells(
                frameOriginX - reach, frameOriginX + widthBlocks - 1 + reach,
                worldHandle.getWorldScale().x, cellBlocks, probe.lakeCellsX);
        StructurePlacementUtility.collectCells(
                frameOriginZ - reach, frameOriginZ + widthBlocks - 1 + reach,
                worldHandle.getWorldScale().y, cellBlocks, probe.lakeCellsZ);

        int lowestCellY = Math.floorDiv(minY, cellHeightBlocks);
        int highestCellY = Math.floorDiv(maxY, cellHeightBlocks);

        for (int indexZ = 0; indexZ < probe.lakeCellsZ.size(); indexZ++)
            for (int indexX = 0; indexX < probe.lakeCellsX.size(); indexX++)
                collectColumn(
                        worldHandle, probe, frame, frameOriginX, frameOriginZ,
                        probe.lakeCellsX.getInt(indexX), probe.lakeCellsZ.getInt(indexZ),
                        lowestCellY, highestCellY, minY, maxY);
    }

    private void collectColumn(
            WorldHandle worldHandle,
            CaveProbeAsyncContainer probe,
            CaveFrameStruct frame,
            long frameOriginX,
            long frameOriginZ,
            int cellX,
            int cellZ,
            int lowestCellY,
            int highestCellY,
            int minY,
            int maxY) {

        long seed = worldHandle.getSeed() ^ EngineSetting.CAVE_LAKE_SEED_SALT;

        long anchorX = computeAnchor(cellX, TerrainCarveUtility.rollCell(
                seed, EngineSetting.CAVE_LAKE_OFFSET_X_SALT, cellX, 0, cellZ));
        long anchorZ = computeAnchor(cellZ, TerrainCarveUtility.rollCell(
                seed, EngineSetting.CAVE_LAKE_OFFSET_Z_SALT, cellX, 0, cellZ));
        float radiusX = lerp(
                EngineSetting.CAVE_LAKE_MIN_RADIUS_BLOCKS, EngineSetting.CAVE_LAKE_MAX_RADIUS_BLOCKS,
                TerrainCarveUtility.rollCell(seed, EngineSetting.CAVE_LAKE_RADIUS_X_SALT, cellX, 0, cellZ));
        float radiusZ = lerp(
                EngineSetting.CAVE_LAKE_MIN_RADIUS_BLOCKS, EngineSetting.CAVE_LAKE_MAX_RADIUS_BLOCKS,
                TerrainCarveUtility.rollCell(seed, EngineSetting.CAVE_LAKE_RADIUS_Z_SALT, cellX, 0, cellZ));

        int slot = resolveCellGround(worldHandle, probe, cellX, cellZ, anchorX, anchorZ, radiusX, radiusZ);

        if (!probe.lakeCellDry[slot] || probe.lakeCellLakes[slot] <= 0f)
            return;

        float centerX = WorldWrapUtility.wrappedBlockDeltaX(worldHandle, anchorX, frameOriginX) + 0.5f;
        float centerZ = WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, anchorZ, frameOriginZ) + 0.5f;

        for (int cellY = lowestCellY; cellY <= highestCellY; cellY++) {

            if (TerrainCarveUtility.rollCell(seed, EngineSetting.CAVE_LAKE_CHANCE_SALT, cellX, cellY, cellZ)
                    >= probe.lakeCellLakes[slot])
                continue;

            float depth = lerp(
                    EngineSetting.CAVE_LAKE_MIN_DEPTH_BLOCKS, EngineSetting.CAVE_LAKE_MAX_DEPTH_BLOCKS,
                    TerrainCarveUtility.rollCell(seed, EngineSetting.CAVE_LAKE_DEPTH_SALT, cellX, cellY, cellZ));
            float headroom = lerp(
                    EngineSetting.CAVE_LAKE_MIN_HEADROOM_BLOCKS, EngineSetting.CAVE_LAKE_MAX_HEADROOM_BLOCKS,
                    TerrainCarveUtility.rollCell(seed, EngineSetting.CAVE_LAKE_HEADROOM_SALT, cellX, cellY, cellZ));

            float lowestSurface = cellY * cellHeightBlocks + depth
                    + EngineSetting.CAVE_LAKE_GUARD_BLOCKS + EngineSetting.CAVE_LAKE_TAPER_BLOCKS;
            float highestSurface = (cellY + 1) * cellHeightBlocks - headroom - 1f;

            if (highestSurface <= lowestSurface)
                continue;

            int waterY = (int) Math.floor(lerp(lowestSurface, highestSurface, TerrainCarveUtility.rollCell(
                    seed, EngineSetting.CAVE_LAKE_OFFSET_Y_SALT, cellX, cellY, cellZ))) - 1;

            if (!fitsGround(probe, slot, waterY, depth, headroom))
                continue;

            CaveLakeSiteStruct site = frame.addLakeSite();

            if (site == null)
                return;

            site.set(centerX, centerZ, waterY, radiusX, radiusZ, depth, headroom);

            if (site.getHighestY() < minY || site.getLowestY() > maxY + 1)
                frame.lakeSiteCount--;
        }
    }

    private long computeAnchor(int cell, float roll) {
        return (long) cell * cellBlocks + (long) Math.ceil(anchorMargin)
                + (long) (roll * (cellBlocks - 2 * Math.ceil(anchorMargin)));
    }

    // Above the floor its caves fade toward, and under a full roof of the lowest ground around its basin
    private boolean fitsGround(CaveProbeAsyncContainer probe, int slot, int waterY, float depth, float headroom) {

        float surfaceY = waterY + 1f;

        return surfaceY - depth - EngineSetting.CAVE_LAKE_GUARD_BLOCKS
                >= probe.lakeCellFloor[slot] + EngineSetting.CAVE_FLOOR_FADE_BLOCKS
                && surfaceY + headroom + EngineSetting.CAVE_LAKE_ROOF_BLOCKS <= probe.lakeCellGround[slot];
    }

    // Ground \\

    // The memo slot holding what the ground keeps around a cell column's basin, read at its centre and the four
    // edges of its guard
    private int resolveCellGround(
            WorldHandle worldHandle,
            CaveProbeAsyncContainer probe,
            int cellX,
            int cellZ,
            long anchorX,
            long anchorZ,
            float radiusX,
            float radiusZ) {

        long key = ((long) cellX << Integer.SIZE) | (cellZ & 0xFFFFFFFFL);
        int revision = biomeManager.getRevision();

        for (int slot = 0; slot < CaveProbeAsyncContainer.LAKE_CACHE_SIZE; slot++)
            if (probe.lakeCellWorlds[slot] == worldHandle
                    && probe.lakeCellKeys[slot] == key
                    && probe.lakeCellRevisions[slot] == revision)
                return slot;

        int slot = probe.lakeCellNext;
        probe.lakeCellNext = (slot + 1) % CaveProbeAsyncContainer.LAKE_CACHE_SIZE;

        long reachX = (long) Math.ceil(radiusX + EngineSetting.CAVE_LAKE_GUARD_BLOCKS);
        long reachZ = (long) Math.ceil(radiusZ + EngineSetting.CAVE_LAKE_GUARD_BLOCKS);

        CaveSiteProbeStruct point = probe.point;

        worldGenerationManager.probeCaveSite(worldHandle, anchorX, anchorZ, probe);

        boolean dry = point.dry;
        int ground = point.groundHeight;
        float lakes = point.caveLakes;
        int floor = point.caveFloorY;

        for (int edge = 0; edge < 4 && dry; edge++) {

            long edgeX = anchorX + (edge == 0 ? reachX : edge == 1 ? -reachX : 0L);
            long edgeZ = anchorZ + (edge == 2 ? reachZ : edge == 3 ? -reachZ : 0L);

            worldGenerationManager.probeCaveSite(worldHandle, edgeX, edgeZ, probe);

            dry = point.dry;
            ground = Math.min(ground, point.groundHeight);
            floor = Math.max(floor, point.caveFloorY);
        }

        probe.lakeCellKeys[slot] = key;
        probe.lakeCellWorlds[slot] = worldHandle;
        probe.lakeCellRevisions[slot] = revision;
        probe.lakeCellDry[slot] = dry;
        probe.lakeCellGround[slot] = ground;
        probe.lakeCellLakes[slot] = lakes;
        probe.lakeCellFloor[slot] = floor;

        return slot;
    }

    // Math \\

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
