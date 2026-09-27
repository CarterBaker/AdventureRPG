package application.bootstrap.worldpipeline.macrostreammanager;

import java.util.Arrays;

import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class MacroRingBranch extends BranchPackage {

    /*
     * Resolves the macro tiles a grid wants. Tile offsets around the active
     * macro tile are sorted by distance once; each time the active chunk moves,
     * every candidate is measured against the chunk grid's footprint and the
     * macro render distance, and the survivors, wrapped around the world,
     * become the grid's macro coordinates in near-to-far load order. A tile
     * whose every chunk lies inside the chunk grid is never wanted.
     */

    // Settings
    private int macroChunkSize;
    private float renderDistanceChunks;
    private float anchorCenter;

    // Candidates — tile offsets sorted near to far
    private long[] candidateOffsets;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.macroChunkSize = EngineSetting.MACRO_CHUNK_SIZE;
        this.renderDistanceChunks = EngineSetting.MACRO_RENDER_DISTANCE_BLOCKS / EngineSetting.CHUNK_SIZE;
        this.anchorCenter = EngineSetting.MACRO_ANCHOR_CENTER_CHUNKS;

        // Candidates
        this.candidateOffsets = buildCandidateOffsets();
    }

    // Candidates \\

    private long[] buildCandidateOffsets() {

        int reach = (int) Math.ceil(renderDistanceChunks / macroChunkSize) + 1;
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

        int gridHalf = settings.maxRenderDistance / 2;
        float gridRadius = settings.maxRenderDistance / 2f;
        float gridRadiusSq = gridRadius * gridRadius;
        float renderDistanceSq = renderDistanceChunks * renderDistanceChunks;

        LongArrayList macroLoadOrder = grid.getMacroLoadOrder();
        LongOpenHashSet macroCoordinates = grid.getMacroCoordinates();

        macroLoadOrder.clear();
        macroCoordinates.clear();

        for (long offset : candidateOffsets) {

            int relativeX = baseX + Coordinate2Long.unpackX(offset) * macroChunkSize - activeX;
            int relativeZ = baseZ + Coordinate2Long.unpackY(offset) * macroChunkSize - activeZ;

            if (isCoveredByChunkGrid(relativeX, relativeZ, gridHalf, gridRadiusSq))
                continue;

            if (nearestDistanceSq(relativeX, relativeZ) > renderDistanceSq)
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

    // Measurement — chunk units, relative to the active chunk's origin corner \\

    private boolean isCoveredByChunkGrid(int relativeX, int relativeZ, int gridHalf, float gridRadiusSq) {

        int lastX = relativeX + macroChunkSize - 1;
        int lastZ = relativeZ + macroChunkSize - 1;

        if (relativeX < -gridHalf || relativeZ < -gridHalf || lastX >= gridHalf || lastZ >= gridHalf)
            return false;

        int farX = Math.max(Math.abs(relativeX), Math.abs(lastX));
        int farZ = Math.max(Math.abs(relativeZ), Math.abs(lastZ));

        return farX * farX + farZ * farZ <= gridRadiusSq;
    }

    private float nearestDistanceSq(int relativeX, int relativeZ) {

        float deltaX = Math.clamp(anchorCenter, relativeX, relativeX + macroChunkSize) - anchorCenter;
        float deltaZ = Math.clamp(anchorCenter, relativeZ, relativeZ + macroChunkSize) - anchorCenter;

        return deltaX * deltaX + deltaZ * deltaZ;
    }
}
