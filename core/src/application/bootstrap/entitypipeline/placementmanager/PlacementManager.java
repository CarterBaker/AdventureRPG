package application.bootstrap.entitypipeline.placementmanager;

import application.bootstrap.combatpipeline.combatmanager.CombatManager;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.worlditem.WorldItemCastStruct;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemPlacementSystem;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemSpaceSystem;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;

public class PlacementManager extends ManagerPackage {

    /*
     * Entity-agnostic placement manager. Owns the raycast result, placement
     * cooldown, and routes to BlockBranch or ItemBranch based on the action.
     * Player passes mouse input. Enemies pass AI input. Same code path either way.
     * A world item in reach and nearer than any block takes the action first:
     * the primary action picks it up, and the activate action sets the held
     * item against the face it was hit on, so items stack into piles —
     * containers included, unless the container can open, since activating
     * one that can opens it in the runtime, never here. Otherwise the primary
     * action swings whatever is held through CombatManager, whose strike lands
     * back here on a block through strikeBlock(), and the activate action
     * places the held item — a block piece as a sub-block, anything else as a
     * world item. Nothing happens here while a stance is held: an aim's throw
     * and a guard belong to CombatManager. findTargetItem() is the one place
     * that decides which world item an entity is aiming at, and where on it.
     */

    // Internal
    private RaycastManager raycastManager;
    private WorldItemSpaceSystem worldItemSpaceSystem;
    private WorldItemPlacementSystem worldItemPlacementSystem;
    private CombatManager combatManager;

    // Branches
    private BlockBranch blockBranch;
    private ItemBranch itemBranch;

    // Settings
    private float placementInterval;

    // State
    private float timeSinceLastPlacement;
    private BlockCastStruct castStruct;
    private WorldItemCastStruct itemCastStruct;

    // Internal \\

    @Override
    protected void create() {

        // Branches
        this.blockBranch = create(BlockBranch.class);
        this.itemBranch = create(ItemBranch.class);

        // Settings
        this.placementInterval = EngineSetting.BLOCK_PLACEMENT_INTERVAL;

        // State
        this.castStruct = new BlockCastStruct();
        this.itemCastStruct = new WorldItemCastStruct();
        this.timeSinceLastPlacement = placementInterval;
    }

    @Override
    protected void get() {

        // Internal
        this.raycastManager = get(RaycastManager.class);
        this.worldItemSpaceSystem = get(WorldItemSpaceSystem.class);
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
        this.combatManager = get(CombatManager.class);
    }

    // Update \\

    public void update(
            EntityInstance entity,
            Vector3 origin,
            Vector3 direction,
            boolean primaryAction,
            boolean activateAction) {

        timeSinceLastPlacement += internal.getDeltaTime();

        if (!(primaryAction || activateAction) || entity.getEntityActionHandle().isHolding())
            return;

        if (timeSinceLastPlacement < placementInterval)
            return;

        WorldItemInstance targetItem = findTargetItem(entity, origin, direction);

        if (targetItem != null) {

            if (primaryAction && itemBranch.pickUp(entity, targetItem)) {
                timeSinceLastPlacement = 0;
                combatManager.gesture(entity, EntityAction.PICK_UP);
                return;
            }

            if (activateAction && handlePlaceOnItem(entity, direction, targetItem)) {
                timeSinceLastPlacement = 0;
                combatManager.gesture(entity, EntityAction.PLACE);
            }

            return;
        }

        if (primaryAction) {
            combatManager.swing(entity);
            return;
        }

        if (!castFrom(entity, origin, direction))
            return;

        if (handlePlaceAction(entity, direction, castStruct)) {
            timeSinceLastPlacement = 0;
            combatManager.gesture(entity, EntityAction.PLACE);
        }
    }

    // Strike \\

    // A landed swing against the block the entity aims at — true when the strike counted
    public boolean strikeBlock(EntityInstance entity, Vector3 origin, Vector3 direction) {

        if (!castFrom(entity, origin, direction)) {
            blockBranch.resetBreakTarget();
            return false;
        }

        return blockBranch.tryBreak(entity, castStruct);
    }

    // How far the entity's reach meets a block, Float.MAX_VALUE when it meets none
    public float findBlockDistance(EntityInstance entity, Vector3 origin, Vector3 direction) {
        return castFrom(entity, origin, direction) ? castStruct.getDistance() : Float.MAX_VALUE;
    }

    // Placement \\

    public boolean placeBlock(
            EntityInstance entity,
            Vector3 origin,
            Vector3 direction,
            short blockID) {

        if (!castFrom(entity, origin, direction))
            return false;

        return blockBranch.tryPlace(castStruct, blockID);
    }

    public boolean placeSubBlock(
            EntityInstance entity,
            Vector3 origin,
            Vector3 direction,
            short blockID) {

        if (!castFrom(entity, origin, direction))
            return false;

        return blockBranch.tryPlaceSubBlock(castStruct, blockID);
    }

    // Raycast \\

    // The world item the entity aims at within reach, unless a block stands in front of it — the face and
    // the cell outside it stay in the item cast for placing against it
    public WorldItemInstance findTargetItem(EntityInstance entity, Vector3 origin, Vector3 direction) {

        float maxDistance = castFrom(entity, origin, direction)
                ? castStruct.getDistance()
                : entity.getStatisticsHandle().getReach() * EngineSetting.REACH_SCALE;

        worldItemSpaceSystem.cast(
                entity.getWorldHandle(),
                entity.getWorldPositionStruct().getChunkCoordinate(),
                origin,
                direction,
                maxDistance,
                itemCastStruct);

        return itemCastStruct.isHit() ? itemCastStruct.getWorldItemInstance() : null;
    }

    private boolean castFrom(EntityInstance entity, Vector3 origin, Vector3 direction) {

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();

        raycastManager.castBlock(
                worldPosition.getChunkCoordinate(),
                origin,
                direction,
                entity.getStatisticsHandle().getReach() * EngineSetting.REACH_SCALE,
                castStruct);

        return castStruct.isHit();
    }

    // Routing \\

    private boolean handlePlaceAction(EntityInstance entity, Vector3 direction, BlockCastStruct castStruct) {

        ItemInstance held = entity.getInventoryHandle().getMainHand();

        if (held == null)
            return false;

        if (held.getItemDefinitionHandle().isBlockPiece())
            return blockBranch.tryPlacePiece(entity, castStruct);

        return itemBranch.place(entity, direction, castStruct);
    }

    // Against the item findTargetItem() last met — a block piece is a sub-block, which only builds on blocks, and a
    // container that can open is opened instead
    private boolean handlePlaceOnItem(EntityInstance entity, Vector3 direction, WorldItemInstance targetItem) {

        ItemInstance held = entity.getInventoryHandle().getMainHand();

        if (held == null
                || held.getItemDefinitionHandle().isBlockPiece()
                || worldItemPlacementSystem.canOpen(targetItem))
            return false;

        return itemBranch.placeOnItem(entity, direction, itemCastStruct);
    }
}