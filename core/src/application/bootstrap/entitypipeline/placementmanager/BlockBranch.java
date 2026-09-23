package application.bootstrap.entitypipeline.placementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.blockplacementsystem.BlockPlacementSystem;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Int;

class BlockBranch extends BranchPackage {

    /*
     * Handles block breaking and placement for PlacementManager. Tracks the
     * current break target across frames and accumulates hits against block
     * durability. Every world edit it makes, a destroyed block or a placed
     * one, goes through BlockPlacementSystem.
     */

    // Internal
    private BlockManager blockManager;
    private BlockPlacementSystem blockPlacementSystem;

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
        resetBreakTarget();
    }

    @Override
    protected void get() {

        // Internal
        this.blockManager = get(BlockManager.class);
        this.blockPlacementSystem = get(BlockPlacementSystem.class);
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

        if (!blockPlacementSystem.replaceBlock(castStruct, airBlockID))
            return true;

        resetBreakTarget();

        return true;
    }

    // Place \\

    boolean tryPlace(BlockCastStruct castStruct, short blockID) {
        return blockPlacementSystem.placeBlockAgainstFace(castStruct, blockID);
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
}