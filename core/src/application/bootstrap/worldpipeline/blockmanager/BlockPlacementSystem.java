package application.bootstrap.worldpipeline.blockmanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.liquidmanager.LiquidManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.ChunkCoordinateUtility;
import application.bootstrap.worldpipeline.util.CoverageUtility;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemSpaceSystem;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction2Vector;
import engine.util.mathematics.extras.Direction3Vector;

public class BlockPlacementSystem extends SystemPackage {

    /*
     * Single entry point for editing blocks in the loaded world: breaking,
     * building and pouring liquid, whole blocks or single sub-blocks. A solid
     * block or sub-block is never built into space a world item claims.
     * editCell() writes the cell, wakes nearby liquid, and rebuilds every
     * subchunk the edit touched, diagonals included, each under its chunk's
     * lock. A cell whose eighth sub-block is filled in becomes a whole block
     * again, so a cell built up from pieces stores like any other block.
     * editCoverage() changes only the coverage grown over a cell, which
     * touches no surface but the cell's own, so it rebuilds the cell's
     * subchunk alone.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private LiquidManager liquidManager;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;
    private WorldRenderManager worldRenderManager;
    private WorldItemSpaceSystem worldItemSpaceSystem;

    // Settings
    private int worldHeight;
    private int chunkSize;
    private int subVoxelResolution;
    private int subVoxelsPerSubBlock;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.worldHeight = EngineSetting.WORLD_HEIGHT;
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.subVoxelResolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        this.subVoxelsPerSubBlock = subVoxelResolution / SubBlockUtility.DIVISIONS;
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
        this.worldItemSpaceSystem = get(WorldItemSpaceSystem.class);
    }

    // Placement \\

    public boolean replaceBlock(BlockCastStruct castStruct, short blockID) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(castStruct.getChunkCoordinate());

        if (chunk == null)
            return false;

        editCell(chunk, castStruct.getSubChunkY(), toHitXYZ(castStruct), blockID, SubBlockUtility.MASK_FULL);

        return true;
    }

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

    // One sub-block in a given octant of a cell — false when the cell holds another block or the octant is taken
    public boolean placeSubBlock(ChunkInstance chunk, int subChunkY, int packedXYZ, int octant, short blockID) {
        return placeInCell(chunk, subChunkY, packedXYZ, octant, blockID, true);
    }

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

        int packedXYZ = ChunkCoordinateUtility.getNeighborAndWrap(hitPackedXYZ, hitFace);

        if (!ChunkCoordinateUtility.isAtEdge(hitPackedXYZ, hitFace))
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

            if (isClaimedByItems(chunk, subChunkY, packedXYZ, blockID, 0, 0, 0, subVoxelResolution))
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

        if (isClaimedByItems(
                chunk,
                subChunkY,
                packedXYZ,
                blockID,
                SubBlockUtility.getOctantX(octant) * subVoxelsPerSubBlock,
                SubBlockUtility.getOctantY(octant) * subVoxelsPerSubBlock,
                SubBlockUtility.getOctantZ(octant) * subVoxelsPerSubBlock,
                subVoxelsPerSubBlock))
            return false;

        editCell(chunk, subChunkY, packedXYZ, blockID, mask);
        return true;
    }

    // True when a solid block would fill a cube of the cell, offset and sized in sub-voxels, that items claim
    private boolean isClaimedByItems(
            ChunkInstance chunk,
            int subChunkY,
            int packedXYZ,
            short blockID,
            int offsetX,
            int offsetY,
            int offsetZ,
            int size) {

        DynamicGeometryType geometry = blockManager.getGeometryFromBlockID(blockID);

        if (geometry == DynamicGeometryType.NONE || geometry == DynamicGeometryType.LIQUID)
            return false;

        int minX = Coordinate3Int.unpackX(packedXYZ) * subVoxelResolution + offsetX;
        int minY = (subChunkY * chunkSize + Coordinate3Int.unpackY(packedXYZ)) * subVoxelResolution + offsetY;
        int minZ = Coordinate3Int.unpackZ(packedXYZ) * subVoxelResolution + offsetZ;

        return worldItemSpaceSystem.isRegionClaimed(
                chunk.getWorldHandle(),
                chunk.getCoordinate(),
                minX,
                minY,
                minZ,
                minX + size,
                minY + size,
                minZ + size);
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

        if (direction.x != 0
                && !ChunkCoordinateUtility.isAtEdge(packedXYZ, Direction3Vector.getDirectionX(direction.x)))
            return false;

        return direction.y == 0
                || ChunkCoordinateUtility.isAtEdge(packedXYZ, Direction3Vector.getDirectionZ(direction.y));
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

        if (ChunkCoordinateUtility.isAtEdge(packedXYZ, Direction3Vector.DOWN) && subChunkY > 0)
            rebuildSubChunk(chunk, subChunkY - 1);

        if (ChunkCoordinateUtility.isAtEdge(packedXYZ, Direction3Vector.UP) && subChunkY < worldHeight - 1)
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

    // Coverage \\

    // The cell the cast struck given a new coverage — callers have checked its block hosts the covering
    public boolean editCoverage(BlockCastStruct castStruct, short coverage) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(castStruct.getChunkCoordinate());

        if (chunk == null)
            return false;

        int subChunkY = castStruct.getSubChunkY();
        ChunkDataSyncContainer syncContainer = chunk.getChunkDataSyncContainer();
        syncContainer.acquire();

        try {
            chunk.getSubChunk(subChunkY).setCoverage(toHitXYZ(castStruct), coverage);

            rebuildSubChunk(chunk, subChunkY);
            mergeAndRender(chunk);
        } finally {
            syncContainer.release();
        }

        invalidateChunk(chunk.getCoordinate());

        return true;
    }

    // Accessible \\

    public short getCoverage(BlockCastStruct castStruct) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(castStruct.getChunkCoordinate());

        if (chunk == null)
            return CoverageUtility.NONE;

        return chunk.getSubChunk(castStruct.getSubChunkY()).getCoverage(toHitXYZ(castStruct));
    }

    public int getSubBlockMask(BlockCastStruct castStruct) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(castStruct.getChunkCoordinate());

        if (chunk == null)
            return SubBlockUtility.MASK_FULL;

        return chunk.getSubChunk(castStruct.getSubChunkY()).getSubBlockMask(toHitXYZ(castStruct));
    }
}
