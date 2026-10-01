package application.bootstrap.entitypipeline.placementmanager;

import application.bootstrap.combatpipeline.combatmanager.CombatManager;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemPlacementSystem;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;

public class PlacementManager extends ManagerPackage {

    /*
     * Entity-agnostic placement manager. Owns the raycast result, placement
     * cooldown, and routes to BlockBranch or ItemBranch based on the action.
     * Player passes mouse input. Enemies pass AI input. Same code path either way.
     * A world item in reach and nearer than any block takes the action first:
     * the primary action picks it up, and the use action is left to whoever
     * handles using items — a chest opens in the runtime, never here.
     * Otherwise the primary action swings whatever is held through
     * CombatManager, whose strike lands back here on a block through
     * strikeBlock(), and the use action places the held item — a block piece
     * as a sub-block, anything else as a world item. Nothing is placed while
     * a stance is held. findTargetItem() is the one place that decides which
     * world item an entity is aiming at.
     */

    // Internal
    private RaycastManager raycastManager;
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
        this.timeSinceLastPlacement = placementInterval;
    }

    @Override
    protected void get() {

        // Internal
        this.raycastManager = get(RaycastManager.class);
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
        this.combatManager = get(CombatManager.class);
    }

    // Update \\

    public void update(
            EntityInstance entity,
            Vector3 origin,
            Vector3 direction,
            boolean primaryAction,
            boolean secondaryAction) {

        timeSinceLastPlacement += internal.getDeltaTime();

        if (!primaryAction && !secondaryAction)
            return;

        if (timeSinceLastPlacement < placementInterval)
            return;

        WorldItemInstance targetItem = findTargetItem(entity, origin, direction);

        if (targetItem != null) {

            if (primaryAction && itemBranch.pickUp(entity, targetItem)) {
                timeSinceLastPlacement = 0;
                combatManager.gesture(entity, EntityAction.PICK_UP);
            }

            return;
        }

        if (primaryAction) {
            combatManager.swing(entity);
            return;
        }

        if (entity.getEntityActionHandle().isHolding() || !castFrom(entity, origin, direction))
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

    // The world item the entity aims at within reach, unless a block stands in front of it
    public WorldItemInstance findTargetItem(EntityInstance entity, Vector3 origin, Vector3 direction) {

        float maxDistance = castFrom(entity, origin, direction)
                ? castStruct.getDistance()
                : entity.getStatisticsHandle().getReach() * EngineSetting.REACH_SCALE;

        return worldItemPlacementSystem.raycastItem(
                entity.getWorldHandle(),
                entity.getWorldPositionStruct().getChunkCoordinate(),
                origin,
                direction,
                maxDistance);
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
}