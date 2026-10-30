package application.bootstrap.entitypipeline.placementmanager;

import application.bootstrap.combatpipeline.combat.BlowOutcome;
import application.bootstrap.combatpipeline.combat.BlowStruct;
import application.bootstrap.combatpipeline.projectilemanager.ProjectileManager;
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
import application.bootstrap.soundpipeline.soundcue.SoundSurfaceStruct;
import application.bootstrap.soundpipeline.soundcuemanager.SoundCueManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.blockmanager.BlockPlacementSystem;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;

class BlockBranch extends BranchPackage {

    /*
     * Handles block breaking and placement for PlacementManager. Every blow,
     * swung or thrown, breaks by one rule: its work — the momentum it drives
     * in — counts against the block's durability, so a stronger arm or a
     * heavier tool breaks a block in fewer strokes. A block that names a tool
     * takes work across strokes only from that tool, at a tier at least its
     * own, the current break target tracked from one stroke to the next; any
     * other blow breaks it only by driving its whole durability in at once,
     * as a hard throw shatters what it meets. A block that names no tool takes
     * work from anything. A block subdivided into sub-blocks breaks one
     * sub-block at a time — the one the blow met — each taking the block's
     * full durability, and yields one block piece; a whole block breaks whole
     * and yields a piece for each of its eight sub-blocks. A whole block
     * holding a content needs a tool of the content's tier too, takes the
     * content's durability on top of its own, and, when the content breaks
     * into pieces, yields the content's pieces in place of its own; a whole
     * block broken under a fully grown covering also yields the covering's
     * drop through CoverageBranch. A blow struck by hand hands what it yields
     * to its striker, and whatever does not fit, or anything a thrown blow
     * breaks loose, is let fall from where it stood through
     * ProjectileManager, carrying a share of the blow's speed. A blow that
     * breaks through spends the share of its work the break took. A held
     * piece is placed back as a sub-block of its block. Every world edit it
     * makes goes through BlockPlacementSystem.
     */

    // Internal
    private BlockManager blockManager;
    private BlockPlacementSystem blockPlacementSystem;
    private ItemDefinitionManager itemDefinitionManager;
    private ItemManager itemManager;
    private CoverageBranch coverageBranch;
    private ContentManager contentManager;
    private ProjectileManager projectileManager;
    private SoundCueManager soundCueManager;

    // Cues
    private int turnedCueID;

    // Block IDs
    private short airBlockID;

    // Scratch
    private Vector3 pieceScratch;
    private Vector3 velocityScratch;
    private Vector3 spinAxisScratch;
    private Quaternion orientationScratch;

    // Break Tracking
    private float currentHits;
    private long targetChunkCoord;
    private int targetPackedBlock;
    private int targetSubChunkY;
    private int targetOctant;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.pieceScratch = new Vector3();
        this.velocityScratch = new Vector3();
        this.spinAxisScratch = new Vector3();
        this.orientationScratch = new Quaternion();

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
        this.contentManager = get(ContentManager.class);
        this.projectileManager = get(ProjectileManager.class);
        this.soundCueManager = get(SoundCueManager.class);
    }

    @Override
    protected void awake() {

        // Cues
        this.turnedCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_TURNED);

        // Block IDs
        this.airBlockID = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);
    }

    // Break \\

    // The blow's work against the block a cast met — counted across strokes for the right tool, all at once otherwise
    BlowOutcome tryBreak(BlowStruct blow, BlockCastStruct castStruct) {

        BlockHandle block = castStruct.getBlock();
        ContentHandle content = contentManager.getContentHandleFromContent(
                blockPlacementSystem.getContent(castStruct));

        if (block == null)
            return BlowOutcome.TURNED;

        WorldHandle worldHandle = blow.getSource().getWorldHandle();

        if (block.isUnbreakable()) {
            playAt(turnedCueID, worldHandle, castStruct);
            return BlowOutcome.TURNED;
        }

        boolean subdivided = SubBlockUtility.isSubdivided(blockPlacementSystem.getSubBlockMask(castStruct));
        boolean tooled = canWork(blow.getItem(), block, content);
        float durability = resolveDurability(block, content);
        float work = blow.getWork();

        if (!tooled && work < durability) {
            playAt(turnedCueID, worldHandle, castStruct);
            return BlowOutcome.TURNED;
        }

        float prior = tooled ? resolvePriorWork(castStruct, subdivided) : 0f;

        if (prior + work < durability) {
            currentHits = prior + work;
            playSurfaceAt(block.getSoundSurface(), false, worldHandle, castStruct);
            return BlowOutcome.WORKED;
        }

        String coveringDrop = subdivided
                ? EngineSetting.COVERING_DROP_NONE
                : coverageBranch.resolveDrop(castStruct);

        boolean broken = subdivided
                ? blockPlacementSystem.removeSubBlock(castStruct)
                : blockPlacementSystem.replaceBlock(castStruct, airBlockID);

        if (!broken)
            return BlowOutcome.WORKED;

        blow.spend(durability - prior);
        playSurfaceAt(block.getSoundSurface(), true, worldHandle, castStruct);
        yieldPieces(blow, castStruct, block, content, subdivided);

        if (!coveringDrop.equals(EngineSetting.COVERING_DROP_NONE))
            yieldItem(blow, itemManager.createItem(coveringDrop), castStruct, 0.5f, 0.5f, 0.5f);

        resetBreakTarget();

        return BlowOutcome.BROKEN;
    }

    // The work already counted against the block a cast met, nothing when it is not the one last struck
    private float resolvePriorWork(BlockCastStruct castStruct, boolean subdivided) {

        int packedTarget = Coordinate3Int.pack(
                castStruct.getBlockX(),
                castStruct.getBlockY(),
                castStruct.getBlockZ());
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

        return currentHits;
    }

    // Pieces \\

    // The content's yield of its pieces from the block's middle when it breaks into any, otherwise the one sub-block
    // struck, or every sub-block of a whole block, each from its own middle
    private void yieldPieces(
            BlowStruct blow,
            BlockCastStruct castStruct,
            BlockHandle block,
            ContentHandle content,
            boolean subdivided) {

        if (content != null && content.hasPiece()) {

            ItemDefinitionHandle contentPiece = itemDefinitionManager.getContentPieceHandle(content.getContentID());

            for (int i = 0; i < content.getYield(); i++)
                yieldItem(blow, itemManager.createItem(contentPiece), castStruct, 0.5f, 0.5f, 0.5f);

            return;
        }

        if (!block.hasPiece())
            return;

        ItemDefinitionHandle piece = itemDefinitionManager.getBlockPieceHandle(block.getBlockID());
        int first = subdivided ? castStruct.getHitOctant() : 0;
        int last = subdivided ? first + 1 : SubBlockUtility.OCTANT_COUNT;

        for (int octant = first; octant < last; octant++)
            yieldItem(
                    blow,
                    itemManager.createItem(piece),
                    castStruct,
                    (SubBlockUtility.getOctantX(octant) + 0.5f) * SubBlockUtility.SIZE,
                    (SubBlockUtility.getOctantY(octant) + 0.5f) * SubBlockUtility.SIZE,
                    (SubBlockUtility.getOctantZ(octant) + 0.5f) * SubBlockUtility.SIZE);
    }

    // One thing the break yields: into the striker's hands when struck by hand and it fits, else let fall from a
    // point within the block struck, with a share of the blow's speed and a scatter and tumble of its own
    private void yieldItem(
            BlowStruct blow,
            ItemInstance itemInstance,
            BlockCastStruct castStruct,
            float offsetX,
            float offsetY,
            float offsetZ) {

        EntityInstance source = blow.getSource();

        if (!blow.isThrown() && source.getInventoryHandle().give(itemInstance))
            return;

        Vector3 velocity = blow.getVelocity();
        float share = EngineSetting.PROJECTILE_DEBRIS_SPEED_SHARE;
        float scatter = EngineSetting.PROJECTILE_DEBRIS_SCATTER;

        pieceScratch.set(
                castStruct.getBlockX() + offsetX,
                castStruct.getSubChunkY() * EngineSetting.CHUNK_SIZE + castStruct.getBlockY() + offsetY,
                castStruct.getBlockZ() + offsetZ);
        velocityScratch.set(
                velocity.x * share + resolveScatter(scatter),
                velocity.y * share + Math.abs(resolveScatter(scatter)),
                velocity.z * share + resolveScatter(scatter));
        spinAxisScratch.set(resolveScatter(1f), resolveScatter(1f), resolveScatter(1f));

        if (spinAxisScratch.lengthSquared() <= EngineSetting.DIVISION_EPSILON)
            spinAxisScratch.set(1f, 0f, 0f);

        spinAxisScratch.normalize();
        orientationScratch.set(1f, 0f, 0f, 0f);

        projectileManager.drop(
                source,
                itemInstance,
                castStruct.getChunkCoordinate(),
                pieceScratch,
                velocityScratch,
                spinAxisScratch,
                EngineSetting.PROJECTILE_DEBRIS_SPIN_RATE,
                orientationScratch);
    }

    private float resolveScatter(float scatter) {
        return (float) (Math.random() * 2.0 - 1.0) * scatter;
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

        SoundSurfaceStruct surface = blockManager.getBlockHandleFromBlockID(piece.getBlockID()).getSoundSurface();

        if (surface != null)
            playAt(surface.getPlaceCueID(), entity.getWorldHandle(), castStruct);

        return true;
    }

    // Sound \\

    // The block's surface struck while it is worked, or as it breaks
    private void playSurfaceAt(
            SoundSurfaceStruct surface,
            boolean broken,
            WorldHandle worldHandle,
            BlockCastStruct castStruct) {
        if (surface != null)
            playAt(broken ? surface.getBreakCueID() : surface.getDigCueID(), worldHandle, castStruct);
    }

    // At the middle of the block cast at
    private void playAt(int cueID, WorldHandle worldHandle, BlockCastStruct castStruct) {
        soundCueManager.playCueAt(
                cueID,
                1f,
                worldHandle,
                castStruct.getChunkCoordinate(),
                castStruct.getBlockX() + 0.5f,
                castStruct.getSubChunkY() * EngineSetting.CHUNK_SIZE + castStruct.getBlockY() + 0.5f,
                castStruct.getBlockZ() + 0.5f);
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

    // Work a block takes, more for the content it holds
    private float resolveDurability(BlockHandle block, ContentHandle content) {
        return block.getDurability() + (content != null ? content.getDurability() : 0);
    }

    // True when a blow struck with an item counts across strokes — any blow on a block that names no tool, otherwise
    // only that tool at the tier of the block and its content
    private boolean canWork(ItemInstance itemInstance, BlockHandle block, ContentHandle content) {

        if (block.getRequiredToolTypeID() == EngineSetting.TOOL_NONE)
            return true;

        if (itemInstance == null)
            return false;

        ItemDefinitionHandle tool = itemInstance.getItemDefinitionHandle();

        int breakTier = content != null
                ? Math.max(block.getBreakTier(), content.getBreakTier())
                : block.getBreakTier();

        return tool.getToolTypeID() == block.getRequiredToolTypeID()
                && tool.getToolTier() >= breakTier;
    }
}
