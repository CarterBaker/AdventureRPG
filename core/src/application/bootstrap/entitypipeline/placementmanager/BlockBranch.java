package application.bootstrap.entitypipeline.placementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.core.engine.BranchPackage;
import application.core.settings.EngineSetting;
import application.core.util.mathematics.extras.Coordinate2Long;
import application.core.util.mathematics.extras.Coordinate3Int;

class BlockBranch extends BranchPackage {

    /*
     * Handles block breaking for PlacementManager. Tracks the current break
     * target across frames, accumulates hits against block durability, and
     * triggers a chunk rebuild when a block is destroyed.
     */

    // Internal
    private BlockManager blockManager;
    private WorldStreamManager worldStreamManager;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;
    private WorldRenderManager worldRenderManager;

    // Settings
    private int chunkSize;
    private int worldHeight;

    // Block IDs
    private short airBlockID;

    // Break Tracking
    private int currentHits;
    private long targetChunkCoord;
    private int targetPackedBlock;
    private int targetSubChunkY;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.worldHeight = EngineSetting.WORLD_HEIGHT;

        resetBreakTarget();
    }

    @Override
    protected void get() {

        // Internal
        this.blockManager = get(BlockManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.dynamicGeometryAsyncContainer = dynamicGeometryManager.getDynamicGeometryAsyncInstance();
        this.worldRenderManager = get(WorldRenderManager.class);
    }

    @Override
    protected void awake() {

        // Block IDs
        this.airBlockID = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);
    }

    // Break \\

    boolean tryBreak(EntityInstance entity, BlockCastStruct castStruct) {

        BlockHandle block = castStruct.getBlock();

        if (block.isUnbreakable())
            return false;

        int breakTier = getBreakTier(entity);

        if (breakTier >= 0) {

            if (breakTier < block.getBreakTier())
                return false;

            if (!isCorrectTool(entity, block))
                return false;
        }

        int packedTarget = Coordinate3Int.pack(
                castStruct.getBlockX(),
                castStruct.getBlockY(),
                castStruct.getBlockZ());

        boolean sameTarget = castStruct.getChunkCoordinate() == targetChunkCoord
                && packedTarget == targetPackedBlock
                && castStruct.getSubChunkY() == targetSubChunkY;

        if (!sameTarget) {
            currentHits = 0;
            targetChunkCoord = castStruct.getChunkCoordinate();
            targetPackedBlock = packedTarget;
            targetSubChunkY = castStruct.getSubChunkY();
        }

        currentHits++;

        if (currentHits < block.getDurability())
            return true;

        ChunkInstance chunk = worldStreamManager.getChunkInstance(castStruct.getChunkCoordinate());

        if (chunk == null)
            return true;

        writeBlock(
                chunk,
                castStruct.getBlockX(),
                castStruct.getBlockY(),
                castStruct.getBlockZ(),
                castStruct.getSubChunkY(),
                airBlockID);

        rebuildAffected(
                chunk,
                castStruct.getChunkCoordinate(),
                castStruct.getBlockX(),
                castStruct.getBlockY(),
                castStruct.getBlockZ(),
                castStruct.getSubChunkY());

        resetBreakTarget();

        return true;
    }

    // Break Target \\

    void resetBreakTarget() {
        currentHits = 0;
        targetChunkCoord = Long.MIN_VALUE;
        targetPackedBlock = Integer.MIN_VALUE;
        targetSubChunkY = Integer.MIN_VALUE;
    }

    // Tool Helpers \\

    private int getBreakTier(EntityInstance entity) {
        return 0;
    }

    private boolean isCorrectTool(EntityInstance entity, BlockHandle block) {
        return block.getRequiredToolTypeID() == EngineSetting.TOOL_NONE;
    }

    // Rebuild \\

    private void rebuildAffected(
            ChunkInstance chunk,
            long chunkCoordinate,
            int blockX, int blockY, int blockZ,
            int subChunkY) {

        rebuildSubChunk(chunk, subChunkY);

        if (blockY == 0 && subChunkY > 0)
            rebuildSubChunk(chunk, subChunkY - 1);

        if (blockY == chunkSize - 1 && subChunkY < worldHeight - 1)
            rebuildSubChunk(chunk, subChunkY + 1);

        mergeAndRender(chunk, chunkCoordinate);

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        if (blockX == 0)
            rebuildNeighbour(chunkX - 1, chunkZ, subChunkY);

        if (blockX == chunkSize - 1)
            rebuildNeighbour(chunkX + 1, chunkZ, subChunkY);

        if (blockZ == 0)
            rebuildNeighbour(chunkX, chunkZ - 1, subChunkY);

        if (blockZ == chunkSize - 1)
            rebuildNeighbour(chunkX, chunkZ + 1, subChunkY);
    }

    private void rebuildSubChunk(ChunkInstance chunk, int subChunkY) {
        chunk.getSubChunk(subChunkY).getDynamicPacketInstance().clear();
        dynamicGeometryManager.buildSubChunk(dynamicGeometryAsyncContainer, chunk, subChunkY);
    }

    private void rebuildNeighbour(int chunkX, int chunkZ, int subChunkY) {

        long coord = Coordinate2Long.pack(chunkX, chunkZ);
        ChunkInstance neighbour = worldStreamManager.getChunkInstance(coord);

        if (neighbour == null)
            return;

        rebuildSubChunk(neighbour, subChunkY);
        mergeAndRender(neighbour, coord);
    }

    private void mergeAndRender(ChunkInstance chunk, long chunkCoordinate) {
        chunk.merge();
        worldRenderManager.addChunkInstance(chunk);
        worldStreamManager.invalidateMegaForChunk(chunkCoordinate);
        worldStreamManager.invalidateChunkBatch(chunkCoordinate);
    }

    // Write \\

    private void writeBlock(
            ChunkInstance chunk,
            int blockX, int blockY, int blockZ,
            int subChunkY, short blockID) {

        SubChunkInstance subChunk = chunk.getSubChunk(subChunkY);

        if (subChunk == null)
            return;

        subChunk.getBlockPaletteHandle().setBlock(blockX, blockY, blockZ, blockID);
    }
}