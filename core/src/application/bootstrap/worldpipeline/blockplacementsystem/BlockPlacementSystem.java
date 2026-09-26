package application.bootstrap.worldpipeline.blockplacementsystem;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.liquidmanager.LiquidManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.ChunkCoordinate3Int;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction2Vector;
import engine.util.mathematics.extras.Direction3Vector;

public class BlockPlacementSystem extends SystemPackage {

    /*
     * The engine's single entry point for editing a block in the loaded
     * world, whether breaking, building, or pouring liquid, a whole block or
     * a single sub-block at a time. Every edit goes through editCell(): it
     * writes the cell, wakes any liquid the edit could set moving, and
     * rebuilds and re-merges every subchunk whose geometry the edit touched —
     * the cell's own, the one above or below it on a vertical border, and
     * every neighbor chunk it touches across a lateral border, diagonals
     * included, since a surface's edges read the sub-blocks diagonally
     * beside it. A subdivided cell holds one material, so a sub-block only
     * joins an empty cell or a cell already subdivided in that same block.
     * Each chunk is written and rebuilt under its own ChunkDataSyncContainer
     * lock, since the streaming pool may be building or merging that same
     * chunk concurrently.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private LiquidManager liquidManager;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;
    private WorldRenderManager worldRenderManager;

    // Settings
    private int worldHeight;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.worldHeight = EngineSetting.WORLD_HEIGHT;
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
        this.liquidManager = get(LiquidManager.class);
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.dynamicGeometryAsyncContainer = dynamicGeometryManager.getDynamicGeometryAsyncInstance();
        this.worldRenderManager = get(WorldRenderManager.class);
    }

    // Placement \\

    public boolean replaceBlock(BlockCastStruct castStruct, short blockID) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(castStruct.getChunkCoordinate());

        if (chunk == null)
            return false;

        editCell(chunk, castStruct.getSubChunkY(), toHitXYZ(castStruct), blockID, SubBlockUtility.MASK_FULL);

        return true;
    }

    /*
     * Breaks the one sub-block the ray met out of its cell; the last one
     * leaves air behind.
     */
    public boolean removeSubBlock(BlockCastStruct castStruct) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(castStruct.getChunkCoordinate());

        if (chunk == null)
            return false;

        int packedXYZ = toHitXYZ(castStruct);
        SubChunkInstance subChunk = chunk.getSubChunk(castStruct.getSubChunkY());
        int mask = subChunk.getSubBlockMask(packedXYZ) & ~SubBlockUtility.getOctantBit(castStruct.getHitOctant());

        editCell(chunk, castStruct.getSubChunkY(), packedXYZ, subChunk.getBlock(packedXYZ), mask);

        return true;
    }

    public boolean placeBlockAgainstFace(BlockCastStruct castStruct, short blockID) {
        return placeAgainstFace(castStruct, blockID, false);
    }

    public boolean placeSubBlockAgainstFace(BlockCastStruct castStruct, short blockID) {
        return placeAgainstFace(castStruct, blockID, true);
    }

    /*
     * Places against the face of the sub-block the ray met. The target is
     * the sub-block beyond that face, which lies in the same cell when the
     * face is internal to a subdivided block — the top of a half-block slab
     * — and in the neighboring cell otherwise.
     */
    private boolean placeAgainstFace(BlockCastStruct castStruct, short blockID, boolean subBlock) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(castStruct.getChunkCoordinate());

        if (chunk == null)
            return false;

        Direction3Vector hitFace = castStruct.getHitFace();
        int hitOctant = castStruct.getHitOctant();
        int targetOctant = SubBlockUtility.stepOctant(hitOctant, hitFace);
        int subChunkY = castStruct.getSubChunkY();
        int hitPackedXYZ = toHitXYZ(castStruct);

        if (!SubBlockUtility.leavesCell(hitOctant, hitFace))
            return placeInCell(chunk, subChunkY, hitPackedXYZ, targetOctant, blockID, subBlock);

        int packedXYZ = ChunkCoordinate3Int.getNeighborAndWrap(hitPackedXYZ, hitFace);

        if (!ChunkCoordinate3Int.isAtEdge(hitPackedXYZ, hitFace))
            return placeInCell(chunk, subChunkY, packedXYZ, targetOctant, blockID, subBlock);

        if (hitFace == Direction3Vector.UP || hitFace == Direction3Vector.DOWN) {

            int targetSubChunkY = subChunkY + hitFace.y;

            if (targetSubChunkY < 0 || targetSubChunkY >= worldHeight)
                return false;

            return placeInCell(chunk, targetSubChunkY, packedXYZ, targetOctant, blockID, subBlock);
        }

        ChunkInstance targetChunk = chunk.getChunkNeighbors().getNeighborChunk(hitFace.to2D().index);

        if (targetChunk == null)
            return false;

        return placeInCell(targetChunk, subChunkY, packedXYZ, targetOctant, blockID, subBlock);
    }

    private boolean placeInCell(
            ChunkInstance chunk,
            int subChunkY,
            int packedXYZ,
            int octant,
            short blockID,
            boolean subBlock) {

        SubChunkInstance subChunk = chunk.getSubChunk(subChunkY);
        short currentBlockID = subChunk.getBlock(packedXYZ);
        int currentMask = subChunk.getSubBlockMask(packedXYZ);

        if (!subBlock) {

            if (SubBlockUtility.isSubdivided(currentMask) && currentBlockID != blockID)
                return false;

            editCell(chunk, subChunkY, packedXYZ, blockID, SubBlockUtility.MASK_FULL);
            return true;
        }

        if (blockManager.getGeometryFromBlockID(blockID) != DynamicGeometryType.FULL)
            return false;

        int mask;

        if (blockManager.getGeometryFromBlockID(currentBlockID) == DynamicGeometryType.NONE)
            mask = SubBlockUtility.getOctantBit(octant);
        else if (currentBlockID == blockID)
            mask = currentMask | SubBlockUtility.getOctantBit(octant);
        else
            return false;

        if (mask == currentMask)
            return false;

        editCell(chunk, subChunkY, packedXYZ, blockID, mask);
        return true;
    }

    private int toHitXYZ(BlockCastStruct castStruct) {
        return Coordinate3Int.pack(castStruct.getBlockX(), castStruct.getBlockY(), castStruct.getBlockZ());
    }

    // Edit \\

    private void editCell(ChunkInstance chunk, int subChunkY, int packedXYZ, short blockID, int mask) {

        ChunkDataSyncContainer syncContainer = chunk.getChunkDataSyncContainer();
        syncContainer.acquire();

        try {
            writeCell(chunk.getSubChunk(subChunkY), packedXYZ, blockID, mask);
            liquidManager.wakeLiquid(chunk, subChunkY, packedXYZ);

            rebuildColumn(chunk, subChunkY, packedXYZ);
            mergeAndRender(chunk);
        } finally {
            syncContainer.release();
        }

        invalidateChunk(chunk.getCoordinate());

        for (Direction2Vector direction : Direction2Vector.VALUES)
            if (isAtLateralEdge(packedXYZ, direction))
                rebuildNeighbourChunk(chunk, subChunkY, packedXYZ, direction);
    }

    private void writeCell(SubChunkInstance subChunk, int packedXYZ, short blockID, int mask) {

        subChunk.setSubBlocks(packedXYZ, blockID, mask);

        if (mask == SubBlockUtility.MASK_FULL
                && blockManager.getGeometryFromBlockID(blockID) == DynamicGeometryType.LIQUID)
            subChunk.setLiquidLevel(packedXYZ, EngineSetting.LIQUID_LEVEL_MAX);
    }

    // Rebuild \\

    private boolean isAtLateralEdge(int packedXYZ, Direction2Vector direction) {

        if (direction.x != 0 && !ChunkCoordinate3Int.isAtEdge(packedXYZ, Direction3Vector.getDirectionX(direction.x)))
            return false;

        return direction.y == 0 || ChunkCoordinate3Int.isAtEdge(packedXYZ, Direction3Vector.getDirectionZ(direction.y));
    }

    private void rebuildNeighbourChunk(
            ChunkInstance chunk,
            int subChunkY,
            int packedXYZ,
            Direction2Vector direction) {

        ChunkInstance neighbour = chunk.getChunkNeighbors().getNeighborChunk(direction.index);

        if (neighbour == null)
            return;

        ChunkDataSyncContainer neighbourSync = neighbour.getChunkDataSyncContainer();
        neighbourSync.acquire();

        try {
            rebuildColumn(neighbour, subChunkY, packedXYZ);
            mergeAndRender(neighbour);
        } finally {
            neighbourSync.release();
        }

        invalidateChunk(neighbour.getCoordinate());
    }

    // The edited cell's subchunk, and the one across its vertical border when it sits on one
    private void rebuildColumn(ChunkInstance chunk, int subChunkY, int packedXYZ) {

        rebuildSubChunk(chunk, subChunkY);

        if (ChunkCoordinate3Int.isAtEdge(packedXYZ, Direction3Vector.DOWN) && subChunkY > 0)
            rebuildSubChunk(chunk, subChunkY - 1);

        if (ChunkCoordinate3Int.isAtEdge(packedXYZ, Direction3Vector.UP) && subChunkY < worldHeight - 1)
            rebuildSubChunk(chunk, subChunkY + 1);
    }

    private void rebuildSubChunk(ChunkInstance chunk, int subChunkY) {
        chunk.getSubChunk(subChunkY).getDynamicPacketInstance().clear();
        dynamicGeometryManager.buildSubChunk(dynamicGeometryAsyncContainer, chunk, subChunkY);
    }

    private void mergeAndRender(ChunkInstance chunk) {
        chunk.merge();
        worldRenderManager.addChunkInstance(chunk);
    }

    private void invalidateChunk(long chunkCoordinate) {
        worldStreamManager.invalidateMegaForChunk(chunkCoordinate);
        worldStreamManager.invalidateChunkBatch(chunkCoordinate);
    }

    // Accessible \\

    public int getSubBlockMask(BlockCastStruct castStruct) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(castStruct.getChunkCoordinate());

        if (chunk == null)
            return SubBlockUtility.MASK_FULL;

        return chunk.getSubChunk(castStruct.getSubChunkY()).getSubBlockMask(toHitXYZ(castStruct));
    }
}
