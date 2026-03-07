package com.internal.bootstrap.entitypipeline.playermanager;

// TODO: This class needs substantial reworking once the inventory and item systems are complete.
//       - Rename to PlacementSystem and move to a more general package
//       - Break logic should read held tool type and tier from InventoryHandle.getMainHand()
//       - Place logic should read block ID from held item's linked block definition
//       - Raycast should move to a shared InteractionSystem — chest, NPC, pickup all need it
//       - GRASS_BLOCK_ID hardcode removed entirely once block items exist
//       - getPlayerBreakTier() and isCorrectTool() stubs wired to real inventory data

import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.entitypipeline.statistics.StatisticsStruct;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import com.internal.bootstrap.itempipeline.tooltypemanager.ToolTypeManager;
import com.internal.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import com.internal.bootstrap.physicspipeline.util.BlockCastStruct;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.block.BlockRotationType;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.Extras.Coordinate3Int;
import com.internal.core.util.mathematics.Extras.Direction3Vector;
import com.internal.core.util.mathematics.vectors.Vector3;

public class BlockPlacementSystem extends SystemPackage {

    // Internal
    private RaycastManager raycastManager;
    private BlockManager blockManager;
    private ChunkStreamManager chunkStreamManager;
    private DynamicGeometryManager dynamicGeometryManager;
    private DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer;
    private WorldRenderManager worldRenderSystem;

    private int CHUNK_SIZE;
    private int WORLD_HEIGHT;
    private float PLACEMENT_INTERVAL;

    // Block IDs
    private short AIR_BLOCK_ID;
    private short GRASS_BLOCK_ID; // TODO: remove once block items exist — read from held item instead

    // Orientation
    private short DEFAULT_ORIENTATION;

    // Placement cooldown
    private float timeSinceLastPlacement;

    // Break tracking
    private int currentHits;
    private long targetChunkCoord;
    private int targetPackedBlock;
    private int targetSubChunkY;

    // Reused per frame
    private final BlockCastStruct castStruct = new BlockCastStruct();

    // Internal \\

    @Override
    protected void create() {
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        this.WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT;
        this.PLACEMENT_INTERVAL = EngineSetting.BLOCK_PLACEMENT_INTERVAL;
        this.timeSinceLastPlacement = PLACEMENT_INTERVAL;
        resetBreakTarget();
    }

    @Override
    protected void get() {
        this.raycastManager = get(RaycastManager.class);
        this.blockManager = get(BlockManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.dynamicGeometryAsyncContainer = dynamicGeometryManager.getDynamicGeometryAsyncInstance();
        this.worldRenderSystem = get(WorldRenderManager.class);
    }

    @Override
    protected void awake() {
        this.AIR_BLOCK_ID = (short) blockManager.getBlockIDFromBlockName("TerraArcana/Air");
        this.GRASS_BLOCK_ID = (short) blockManager.getBlockIDFromBlockName("TerraArcana/Grass Block");
        this.DEFAULT_ORIENTATION = (short) (EngineSetting.DEFAULT_BLOCK_DIRECTION * 4);
    }

    // Update \\

    public void update(
            EntityHandle player,
            Vector3 cameraPosition,
            Vector3 cameraDirection,
            StatisticsStruct statistics,
            boolean breakBlock,
            boolean placeBlock) {

        timeSinceLastPlacement += internal.getDeltaTime();

        // Nothing to do if no input
        if (!breakBlock && !placeBlock) {
            resetBreakTarget();
            return;
        }

        // Nothing to do if nothing in hand
        if (!player.getInventoryHandle().hasMainHand()) {
            resetBreakTarget();
            return;
        }

        if (timeSinceLastPlacement < PLACEMENT_INTERVAL)
            return;

        WorldPositionStruct worldPositionStruct = player.getWorldPositionStruct();

        raycastManager.castBlock(
                worldPositionStruct.getChunkCoordinate(),
                cameraPosition,
                cameraDirection,
                statistics.reach,
                castStruct);

        if (!castStruct.hit) {
            resetBreakTarget();
            return;
        }

        if (placeBlock) {
            resetBreakTarget();
            handlePlace(player, cameraDirection);
            return;
        }

        handleBreak(player, statistics);
    }

    // Break \\

    private void handleBreak(EntityHandle player, StatisticsStruct statistics) {

        BlockHandle block = castStruct.block;

        // Unbreakable — hard reject
        if (block.isUnbreakable())
            return;

        int playerTier = getPlayerBreakTier(player);

        // Negative tier bypasses all rules — dev/enchant overflow path
        if (playerTier >= 0) {
            if (playerTier < block.getBreakTier())
                return;
            if (!isCorrectTool(player, block))
                return;
        }

        // Check if still targeting the same block
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
        timeSinceLastPlacement = 0f;

        if (currentHits < block.getDurability())
            return;

        // Durability reached — break the block
        ChunkInstance chunk = chunkStreamManager.getChunkInstance(castStruct.chunkCoordinate);
        if (chunk == null)
            return;

        writeBlock(chunk,
                castStruct.blockX, castStruct.blockY, castStruct.blockZ,
                castStruct.subChunkY, AIR_BLOCK_ID);

        rebuildAffected(chunk, castStruct.chunkCoordinate,
                castStruct.blockX, castStruct.blockY, castStruct.blockZ,
                castStruct.subChunkY);

        resetBreakTarget();
    }

    // Place \\

    private void handlePlace(EntityHandle player, Vector3 cameraDirection) {

        // TODO: resolve block ID from player.getInventoryHandle().getMainHand()
        // once block items exist — hardcoded grass removed at that point

        int placeX = castStruct.blockX + castStruct.hitFace.x;
        int placeY = castStruct.blockY + castStruct.hitFace.y;
        int placeZ = castStruct.blockZ + castStruct.hitFace.z;
        int placeSubChunkY = castStruct.subChunkY;

        int placeChunkX = Coordinate2Long.unpackX(castStruct.chunkCoordinate);
        int placeChunkZ = Coordinate2Long.unpackY(castStruct.chunkCoordinate);

        if (placeX < 0) {
            placeChunkX--;
            placeX += CHUNK_SIZE;
        } else if (placeX >= CHUNK_SIZE) {
            placeChunkX++;
            placeX -= CHUNK_SIZE;
        }

        if (placeZ < 0) {
            placeChunkZ--;
            placeZ += CHUNK_SIZE;
        } else if (placeZ >= CHUNK_SIZE) {
            placeChunkZ++;
            placeZ -= CHUNK_SIZE;
        }

        if (placeY < 0) {
            placeSubChunkY--;
            placeY += CHUNK_SIZE;
        } else if (placeY >= CHUNK_SIZE) {
            placeSubChunkY++;
            placeY -= CHUNK_SIZE;
        }

        long placeChunkCoord = Coordinate2Long.pack(placeChunkX, placeChunkZ);

        int packedPlaceCoord = Coordinate3Int.pack(placeX, placeY, placeZ);
        if (player.getBlockCompositionStruct().getBlockCompositionMap().containsKey(packedPlaceCoord))
            return;

        ChunkInstance placeChunk = chunkStreamManager.getChunkInstance(placeChunkCoord);
        if (placeChunk == null)
            return;

        BlockHandle grassHandle = blockManager.getBlockFromBlockID(GRASS_BLOCK_ID);
        Direction3Vector hitFaceDir = Direction3Vector.getDirection(
                castStruct.hitFace.x,
                castStruct.hitFace.y,
                castStruct.hitFace.z);
        short orientation = resolveOrientation(grassHandle, hitFaceDir, cameraDirection);

        writeBlock(placeChunk, placeX, placeY, placeZ, placeSubChunkY, GRASS_BLOCK_ID);
        writeOrientation(placeChunk, placeX, placeY, placeZ, placeSubChunkY, orientation);
        rebuildAffected(placeChunk, placeChunkCoord, placeX, placeY, placeZ, placeSubChunkY);

        timeSinceLastPlacement = 0f;
    }

    // Tool Helpers \\

    private int getPlayerBreakTier(EntityHandle player) {
        // TODO: read from player.getInventoryHandle().getMainHand() tool tier
        // once tool items exist — returning 0 means bare hand only
        return 0;
    }

    private boolean isCorrectTool(EntityHandle player, BlockHandle block) {
        // TODO: read tool type from player.getInventoryHandle().getMainHand()
        // once tool items exist — TOOL_NONE blocks always pass
        return block.getRequiredToolTypeID() == ToolTypeManager.TOOL_NONE;
    }

    // Break Target \\

    private void resetBreakTarget() {
        currentHits = 0;
        targetChunkCoord = Long.MIN_VALUE;
        targetPackedBlock = Integer.MIN_VALUE;
        targetSubChunkY = Integer.MIN_VALUE;
    }

    // Orientation \\

    private short resolveOrientation(
            BlockHandle blockHandle,
            Direction3Vector hitFace,
            Vector3 cameraDirection) {

        BlockRotationType rot = blockHandle.getRotationType();

        if (rot == BlockRotationType.NONE || rot == BlockRotationType.NATURAL_FULL)
            return DEFAULT_ORIENTATION;

        Direction3Vector facing;
        if (rot == BlockRotationType.CARDINAL) {
            if (hitFace == Direction3Vector.UP || hitFace == Direction3Vector.DOWN)
                facing = Direction3Vector.VALUES[EngineSetting.DEFAULT_BLOCK_DIRECTION];
            else
                facing = hitFace;
        } else {
            facing = hitFace;
        }

        int spin = 0;
        if (facing == Direction3Vector.UP || facing == Direction3Vector.DOWN) {
            float ax = Math.abs(cameraDirection.x);
            float az = Math.abs(cameraDirection.z);
            if (ax >= az)
                spin = cameraDirection.x > 0 ? 1 : 3;
            else
                spin = cameraDirection.z > 0 ? 0 : 2;
        }

        return (short) (facing.ordinal() * 4 + spin);
    }

    // Rebuild \\

    private void rebuildAffected(
            ChunkInstance chunk,
            long chunkCoordinate,
            int blockX,
            int blockY,
            int blockZ,
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
        worldRenderSystem.addChunkInstance(chunk);
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

    private void writeOrientation(
            ChunkInstance chunk,
            int blockX, int blockY, int blockZ,
            int subChunkY, short orientation) {
        SubChunkInstance subChunk = chunk.getSubChunk(subChunkY);
        if (subChunk == null)
            return;
        subChunk.getBlockRotationPaletteHandle().setBlock(blockX, blockY, blockZ, orientation);
    }
}