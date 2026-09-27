package application.bootstrap.worldpipeline.macrostreammanager;

import java.util.Arrays;

import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.macrochunk.MacroChunkInstance;
import application.bootstrap.worldpipeline.util.MacroTerrainUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class MacroRingBranch extends BranchPackage {

    /*
     * Resolves the macro tiles a grid wants and what each should be built as.
     * Tile offsets around the active macro tile are sorted by distance once.
     * The ring reaches only as far as the world's curve lets the eye see, the
     * eye's horizon reach plus that of the highest ground the world can raise,
     * so a tile sunk below the horizon is never built. The ring is rebuilt when
     * the active chunk moves or the eye climbs or drops far enough to move the
     * horizon by a whole tile; every candidate is measured against the chunk
     * grid's footprint and that horizon, and the survivors, wrapped around the
     * world, become the grid's macro coordinates in near-to-far load order. A
     * tile wholly inside the chunk grid is never wanted, and a tile's target
     * is its lattice resolution by distance. The horizon grows the ring at
     * once but shrinks it only after falling MACRO_HORIZON_SHRINK_TILES
     * tiles, so walking over a hill never streams the rim out and back in.
     */

    // Settings
    private int macroChunkSize;
    private int chunkSize;
    private float tileSizeBlocks;
    private float renderDistanceBlocks;
    private float terrainReachBlocks;

    // Candidates — tile offsets sorted near to far
    private long[] candidateOffsets;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.macroChunkSize = EngineSetting.MACRO_CHUNK_SIZE;
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.tileSizeBlocks = macroChunkSize * chunkSize;
        this.renderDistanceBlocks = EngineSetting.MACRO_RENDER_DISTANCE_BLOCKS;
        this.terrainReachBlocks = MacroTerrainUtility.resolveHorizonReachBlocks(
                EngineSetting.TERRAIN_MAX_HEIGHT_BLOCKS);

        // Candidates
        this.candidateOffsets = buildCandidateOffsets();
    }

    // Candidates \\

    private long[] buildCandidateOffsets() {

        int reach = (int) Math.ceil(renderDistanceBlocks / tileSizeBlocks) + 1;
        int side = reach * 2 + 1;
        long[] sortScratch = new long[side * side];
        int count = 0;

        for (int x = -reach; x <= reach; x++) {
            for (int z = -reach; z <= reach; z++) {
                int index = (x + reach) * side + (z + reach);
                sortScratch[count++] = ((long) (x * x + z * z) << 32) | (index & 0xFFFFFFFFL);
            }
        }

        Arrays.sort(sortScratch, 0, count);

        long[] offsets = new long[count];

        for (int i = 0; i < count; i++) {
            int index = (int) (sortScratch[i] & 0xFFFFFFFFL);
            offsets[i] = Coordinate2Long.pack(index / side - reach, index % side - reach);
        }

        return offsets;
    }

    // Ring \\

    public boolean updateRing(GridInstance grid) {

        long activeChunkCoordinate = grid.getActiveChunkCoordinate();
        int currentHorizonTiles = grid.getMacroHorizonTiles();
        int horizonTiles = resolveHorizonTiles(grid);

        boolean horizonMoved = horizonTiles > currentHorizonTiles
                || horizonTiles <= currentHorizonTiles - EngineSetting.MACRO_HORIZON_SHRINK_TILES;

        if (!horizonMoved)
            horizonTiles = currentHorizonTiles;

        if (activeChunkCoordinate == grid.getMacroAnchorCoordinate() && !horizonMoved)
            return false;

        rebuildRing(grid, activeChunkCoordinate, horizonTiles * tileSizeBlocks);
        grid.anchorMacroRing(activeChunkCoordinate, horizonTiles);

        return true;
    }

    private int resolveHorizonTiles(GridInstance grid) {

        float horizonBlocks = Math.min(
                renderDistanceBlocks,
                MacroTerrainUtility.resolveEyeReachBlocks(grid) + terrainReachBlocks);

        return (int) Math.ceil(horizonBlocks / tileSizeBlocks);
    }

    private void rebuildRing(GridInstance grid, long activeChunkCoordinate, float horizonBlocks) {

        WorldHandle worldHandle = grid.getWorldHandle();
        validateWorldScale(worldHandle);

        int activeX = Coordinate2Long.unpackX(activeChunkCoordinate);
        int activeZ = Coordinate2Long.unpackY(activeChunkCoordinate);
        int baseX = Math.floorDiv(activeX, macroChunkSize) * macroChunkSize;
        int baseZ = Math.floorDiv(activeZ, macroChunkSize) * macroChunkSize;

        int gridHalf = MacroTerrainUtility.resolveGridHalf(settings.maxRenderDistance);
        float gridRadiusSq = MacroTerrainUtility.resolveGridRadiusSq(settings.maxRenderDistance);
        float horizonChunks = horizonBlocks / chunkSize;

        LongArrayList macroLoadOrder = grid.getMacroLoadOrder();
        LongOpenHashSet macroCoordinates = grid.getMacroCoordinates();

        macroLoadOrder.clear();
        macroCoordinates.clear();

        for (long offset : candidateOffsets) {

            int relativeX = baseX + Coordinate2Long.unpackX(offset) * macroChunkSize - activeX;
            int relativeZ = baseZ + Coordinate2Long.unpackY(offset) * macroChunkSize - activeZ;

            if (MacroTerrainUtility.isCoveredByChunkGrid(relativeX, relativeZ, macroChunkSize, gridHalf, gridRadiusSq))
                continue;

            if (MacroTerrainUtility.nearestDistanceChunks(relativeX, relativeZ, macroChunkSize) > horizonChunks)
                continue;

            long macroCoordinate = WorldWrapUtility.wrapAroundWorld(
                    worldHandle,
                    Coordinate2Long.pack(activeX + relativeX, activeZ + relativeZ));

            if (macroCoordinates.add(macroCoordinate))
                macroLoadOrder.add(macroCoordinate);
        }
    }

    private void validateWorldScale(WorldHandle worldHandle) {

        int worldWidthChunks = worldHandle.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = worldHandle.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        if (worldWidthChunks % macroChunkSize != 0 || worldHeightChunks % macroChunkSize != 0)
            throwException("World \"" + worldHandle.getWorldName() + "\" spans " + worldWidthChunks + "x"
                    + worldHeightChunks + " chunks, which MACRO_CHUNK_SIZE (" + macroChunkSize
                    + ") does not divide — macro tiles would straddle the world's wrap seam.");
    }

    // Target \\

    void resolveTarget(GridInstance grid, MacroChunkInstance macro) {

        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                grid.getWorldHandle(),
                grid.getActiveChunkCoordinate(),
                macro.getCoordinate());

        int relativeX = Coordinate2Long.unpackX(delta);
        int relativeZ = Coordinate2Long.unpackY(delta);
        float nearestDistanceBlocks = MacroTerrainUtility.nearestDistanceChunks(
                relativeX, relativeZ, macroChunkSize) * chunkSize;

        macro.setTarget(
                MacroTerrainUtility.resolveCellsPerSide(nearestDistanceBlocks, tileSizeBlocks),
                nearestDistanceBlocks);
    }
}
