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
     * The world is flat, so the ring reaches MACRO_RENDER_DISTANCE_BLOCKS in
     * every direction and is rebuilt only when the active chunk moves; every
     * candidate is measured against the chunk grid's settled footprint and
     * that distance, and the survivors, wrapped around the world, become the
     * grid's macro coordinates in near-to-far load order. A tile is left out
     * only when it lies wholly inside the settled footprint, so the grid's
     * streaming rim is always backed by macro terrain, and a tile's target is
     * its lattice resolution by distance.
     */

    // Settings
    private int macroChunkSize;
    private int chunkSize;
    private float tileSizeBlocks;
    private float renderDistanceBlocks;

    // Candidates — tile offsets sorted near to far
    private long[] candidateOffsets;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.macroChunkSize = EngineSetting.MACRO_CHUNK_SIZE;
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.tileSizeBlocks = EngineSetting.MACRO_TILE_SIZE_BLOCKS;
        this.renderDistanceBlocks = EngineSetting.MACRO_RENDER_DISTANCE_BLOCKS;

        // Candidates
        this.candidateOffsets = buildCandidateOffsets();
    }

    // Candidates \\

    private long[] buildCandidateOffsets() {

        int reach = EngineSetting.MACRO_RING_REACH_TILES;
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

        if (activeChunkCoordinate == grid.getMacroAnchorCoordinate())
            return false;

        rebuildRing(grid, activeChunkCoordinate);
        grid.anchorMacroRing(activeChunkCoordinate);

        return true;
    }

    private void rebuildRing(GridInstance grid, long activeChunkCoordinate) {

        WorldHandle worldHandle = grid.getWorldHandle();
        validateWorldScale(worldHandle);

        int activeX = Coordinate2Long.unpackX(activeChunkCoordinate);
        int activeZ = Coordinate2Long.unpackY(activeChunkCoordinate);
        int baseX = Math.floorDiv(activeX, macroChunkSize) * macroChunkSize;
        int baseZ = Math.floorDiv(activeZ, macroChunkSize) * macroChunkSize;

        int settledHalf = MacroTerrainUtility.resolveSettledHalf(settings.maxRenderDistance);
        float settledRadiusSq = MacroTerrainUtility.resolveSettledRadiusSq(settings.maxRenderDistance);
        float renderDistanceChunks = renderDistanceBlocks / chunkSize;

        LongArrayList macroLoadOrder = grid.getMacroLoadOrder();
        LongOpenHashSet macroCoordinates = grid.getMacroCoordinates();

        macroLoadOrder.clear();
        macroCoordinates.clear();

        for (long offset : candidateOffsets) {

            int relativeX = baseX + Coordinate2Long.unpackX(offset) * macroChunkSize - activeX;
            int relativeZ = baseZ + Coordinate2Long.unpackY(offset) * macroChunkSize - activeZ;

            if (MacroTerrainUtility.isCoveredByChunkGrid(
                    relativeX, relativeZ, macroChunkSize, settledHalf, settledRadiusSq))
                continue;

            if (MacroTerrainUtility.nearestDistanceChunks(relativeX, relativeZ, macroChunkSize) > renderDistanceChunks)
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

        macro.setTarget(MacroTerrainUtility.resolveCellsPerSide(nearestDistanceBlocks, tileSizeBlocks));
    }
}
