package application.bootstrap.entitypipeline.placementmanager;

import application.bootstrap.contentpipeline.content.ContentHandle;
import application.bootstrap.contentpipeline.contentmanager.ContentManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.itempipeline.itemmanager.ItemManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.blockmanager.BlockPlacementSystem;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Int;

class BlockBranch extends BranchPackage {

    /*
     * Handles block breaking and placement for PlacementManager. Tracks the
     * current break target across strikes and counts each strike against block
     * durability by the striker's work power, so a stronger worker breaks a
     * block in fewer strikes. A block that names a tool only takes strikes
     * from that tool held in the main hand, at a tier at least its own. A
     * block subdivided into sub-blocks breaks one sub-block at a time — the
     * one the strike met — each taking the block's full durability, and hands
     * out one block piece; a whole block breaks whole and hands out a piece
     * for each of its eight sub-blocks. A whole block holding a content needs
     * a tool of the content's tier too, takes the content's durability on top
     * of its own, and, when the content breaks into pieces, hands out the
     * content's yield of pieces in place of its own. The pieces must fit in
     * the striker's inventory, or the block holds. A whole block broken under
     * a fully grown covering also hands out the covering's drop through
     * CoverageBranch. A held piece is placed back as a sub-block of its
     * block. Every world edit it makes goes through BlockPlacementSystem.
     */

    // Internal
    private BlockManager blockManager;
    private BlockPlacementSystem blockPlacementSystem;
    private ItemDefinitionManager itemDefinitionManager;
    private ItemManager itemManager;
    private CoverageBranch coverageBranch;
    private StatManager statManager;
    private ContentManager contentManager;

    // Block IDs
    private short airBlockID;

    // Break Tracking
    private float currentHits;
    private long targetChunkCoord;
    private int targetPackedBlock;
    private int targetSubChunkY;
    private int targetOctant;

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
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.itemManager = get(ItemManager.class);
        this.coverageBranch = get(CoverageBranch.class);
        this.statManager = get(StatManager.class);
        this.contentManager = get(ContentManager.class);
    }

    @Override
    protected void awake() {

        // Block IDs
        this.airBlockID = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);
    }

    // Break \\

    boolean tryBreak(EntityInstance entity, BlockCastStruct castStruct) {

        BlockHandle block = castStruct.getBlock();
        ContentHandle content = contentManager.getContentHandleFromContent(
                blockPlacementSystem.getContent(castStruct));

        if (!canBreak(entity, block, content))
            return false;

        int packedTarget = Coordinate3Int.pack(
                castStruct.getBlockX(),
                castStruct.getBlockY(),
                castStruct.getBlockZ());

        boolean subdivided = SubBlockUtility.isSubdivided(blockPlacementSystem.getSubBlockMask(castStruct));
        int octantTarget = subdivided ? castStruct.getHitOctant() : EngineSetting.INDEX_NOT_FOUND;

        boolean sameTarget = castStruct.getChunkCoordinate() == targetChunkCoord
                && packedTarget == targetPackedBlock
                && castStruct.getSubChunkY() == targetSubChunkY
                && octantTarget == targetOctant;

        if (!sameTarget) {
            currentHits = 0f;
            targetChunkCoord = castStruct.getChunkCoordinate();
            targetPackedBlock = packedTarget;
            targetSubChunkY = castStruct.getSubChunkY();
            targetOctant = octantTarget;
        }

        currentHits += Math.max(0f, entity.getStat(statManager.getWorkPowerStatID()));

        if (currentHits < resolveDurability(block, content))
            return true;

        if (!givePieces(entity, block, content, subdivided ? 1 : SubBlockUtility.OCTANT_COUNT))
            return true;

        String coveringDrop = subdivided
                ? EngineSetting.COVERING_DROP_NONE
                : coverageBranch.resolveDrop(castStruct);

        boolean broken = subdivided
                ? blockPlacementSystem.removeSubBlock(castStruct)
                : blockPlacementSystem.replaceBlock(castStruct, airBlockID);

        if (!broken)
            return true;

        coverageBranch.giveDrop(entity, coveringDrop);

        resetBreakTarget();

        return true;
    }

    // Pieces \\

    // The content's yield of its pieces when it breaks into any, otherwise a count of the block's own
    private boolean givePieces(EntityInstance entity, BlockHandle block, ContentHandle content, int count) {

        if (content != null && content.hasPiece()) {

            ItemDefinitionHandle contentPiece = itemDefinitionManager.getContentPieceHandle(content.getContentID());

            return entity.getInventoryHandle().give(itemManager.createStack(contentPiece, content.getYield()));
        }

        if (!block.hasPiece())
            return true;

        ItemDefinitionHandle piece = itemDefinitionManager.getBlockPieceHandle(block.getBlockID());

        return entity.getInventoryHandle().give(itemManager.createStack(piece, count));
    }

    // Place \\

    boolean tryPlace(BlockCastStruct castStruct, short blockID) {
        return blockPlacementSystem.placeBlockAgainstFace(castStruct, blockID);
    }

    boolean tryPlaceSubBlock(BlockCastStruct castStruct, short blockID) {
        return blockPlacementSystem.placeSubBlockAgainstFace(castStruct, blockID);
    }

    // The block piece in the main hand, built back into the world as one sub-block on the face that was hit
    boolean tryPlacePiece(EntityInstance entity, BlockCastStruct castStruct) {

        InventoryHandle inventoryHandle = entity.getInventoryHandle();
        ItemDefinitionHandle piece = inventoryHandle.getMainHand().getItemDefinitionHandle();

        if (!blockPlacementSystem.placeSubBlockAgainstFace(castStruct, piece.getBlockID()))
            return false;

        inventoryHandle.takeOne(EquipmentSlot.MAIN_HAND);

        return true;
    }

    // Break Target \\

    void resetBreakTarget() {
        currentHits = 0f;
        targetChunkCoord = Long.MIN_VALUE;
        targetPackedBlock = Integer.MIN_VALUE;
        targetSubChunkY = Integer.MIN_VALUE;
        targetOctant = Integer.MIN_VALUE;
    }

    // Tool Helpers \\

    // Strikes a block takes, more for the content it holds
    private int resolveDurability(BlockHandle block, ContentHandle content) {
        return block.getDurability() + (content != null ? content.getDurability() : 0);
    }

    private boolean canBreak(EntityInstance entity, BlockHandle block, ContentHandle content) {

        if (block.isUnbreakable())
            return false;

        if (block.getRequiredToolTypeID() == EngineSetting.TOOL_NONE)
            return true;

        ItemInstance held = entity.getInventoryHandle().getMainHand();

        if (held == null)
            return false;

        ItemDefinitionHandle tool = held.getItemDefinitionHandle();

        int breakTier = content != null
                ? Math.max(block.getBreakTier(), content.getBreakTier())
                : block.getBreakTier();

        return tool.getToolTypeID() == block.getRequiredToolTypeID()
                && tool.getToolTier() >= breakTier;
    }
}
