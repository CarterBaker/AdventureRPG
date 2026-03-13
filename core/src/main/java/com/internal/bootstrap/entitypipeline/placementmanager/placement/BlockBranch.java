package com.internal.bootstrap.entitypipeline.placementmanager.placement;

import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import com.internal.bootstrap.itempipeline.tooltypemanager.ToolTypeManager;
import com.internal.bootstrap.physicspipeline.util.BlockCastStruct;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.Extras.Coordinate3Int;

public class BlockBranch extends BranchPackage {

    // Internal
    private BlockManager blockManager;
    private ChunkStreamManager chunkStreamManager;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;
    private WorldRenderManager worldRenderManager;

    private int CHUNK_SIZE;
    private int WORLD_HEIGHT;

    // Block IDs
    private short AIR_BLOCK_ID;

    // Break tracking
    private int currentHits;
    private long targetChunkCoord;
    private int targetPackedBlock;
    private int targetSubChunkY;

    // Internal \\

    @Override
    protected void create() {
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        this.WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT;
        resetBreakTarget();
    }

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.dynamicGeometryAsyncContainer = dynamicGeometryManager.getDynamicGeometryAsyncInstance();
        this.worldRenderManager = get(WorldRenderManager.class);
    }

    @Override
    protected void awake() {
        this.AIR_BLOCK_ID = (short) blockManager.getBlockIDFromBlockName("TerraArcana/Air");
    }

    // Break \\

    /*
     * Returns true if a hit was registered (caller should reset placement timer).
     */
    public boolean tryBreak(EntityHandle entity, BlockCastStruct castStruct) {

        BlockHandle block = castStruct.block;

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
                castStruct.blockX, castStruct.blockY, castStruct.blockZ);

        boolean sameTarget = castStruct.chunkCoordinate == targetChunkCoord
                && packedTarget == targetPackedBlock
                && castStruct.subChunkY == targetSubChunkY;

        if (!sameTarget) {
            currentHits = 0;
            targetChunkCoord = castStruct.chunkCoordinate;
            targetPackedBlock = packedTarget;
            targetSubChunkY = castStruct.subChunkY;
        }

        currentHits++;

        if (currentHits < block.getDurability())
            return true;

        ChunkInstance chunk = chunkStreamManager.getChunkInstance(castStruct.chunkCoordinate);
        if (chunk == null)
            return true;

        writeBlock(chunk,
                castStruct.blockX, castStruct.blockY, castStruct.blockZ,
                castStruct.subChunkY, AIR_BLOCK_ID);

        rebuildAffected(chunk, castStruct.chunkCoordinate,
                castStruct.blockX, castStruct.blockY, castStruct.blockZ,
                castStruct.subChunkY);

        resetBreakTarget();
        return true;
    }

    // Break Target \\

    public void resetBreakTarget() {
        currentHits = 0;
        targetChunkCoord = Long.MIN_VALUE;
        targetPackedBlock = Integer.MIN_VALUE;
        targetSubChunkY = Integer.MIN_VALUE;
    }

    // Tool Helpers \\

    private int getBreakTier(EntityHandle entity) {
        // TODO: read from entity.getInventoryHandle().getMainHand() tool tier
        return 0;
    }

    private boolean isCorrectTool(EntityHandle entity, BlockHandle block) {
        // TODO: read tool type from entity.getInventoryHandle().getMainHand()
        return block.getRequiredToolTypeID() == ToolTypeManager.TOOL_NONE;
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
        if (blockY == CHUNK_SIZE - 1 && subChunkY < WORLD_HEIGHT - 1)
            rebuildSubChunk(chunk, subChunkY + 1);

        mergeAndRender(chunk, chunkCoordinate);

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);

        if (blockX == 0)
            rebuildNeighbour(chunkX - 1, chunkZ, subChunkY);
        if (blockX == CHUNK_SIZE - 1)
            rebuildNeighbour(chunkX + 1, chunkZ, subChunkY);
        if (blockZ == 0)
            rebuildNeighbour(chunkX, chunkZ - 1, subChunkY);
        if (blockZ == CHUNK_SIZE - 1)
            rebuildNeighbour(chunkX, chunkZ + 1, subChunkY);
    }

    private void rebuildSubChunk(ChunkInstance chunk, int subChunkY) {
        chunk.getSubChunk(subChunkY).getDynamicPacketInstance().clear();
        dynamicGeometryManager.buildSubChunk(dynamicGeometryAsyncContainer, chunk, subChunkY);
    }

    private void rebuildNeighbour(int chunkX, int chunkZ, int subChunkY) {
        long coord = Coordinate2Long.pack(chunkX, chunkZ);
        ChunkInstance neighbour = chunkStreamManager.getChunkInstance(coord);
        if (neighbour == null)
            return;
        rebuildSubChunk(neighbour, subChunkY);
        mergeAndRender(neighbour, coord);
    }

    private void mergeAndRender(ChunkInstance chunk, long chunkCoordinate) {
        chunk.merge();
        worldRenderManager.addChunkInstance(chunk);
        chunkStreamManager.invalidateMegaForChunk(chunkCoordinate);
        chunkStreamManager.invalidateChunkBatch(chunkCoordinate);
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