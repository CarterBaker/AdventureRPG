package application.bootstrap.entitypipeline.placementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
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
     * the break action picks it up, and the use action is left to whoever
     * handles using items — a chest opens in the runtime, never here.
     * findTargetItem() is the one place that decides which world item an
     * entity is aiming at.
     */

    // Internal
    private RaycastManager raycastManager;
    private WorldItemPlacementSystem worldItemPlacementSystem;

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
    }

    // Update \\

    public void update(
            EntityInstance entity,
            Vector3 origin,
            Vector3 direction,
            boolean breakAction,
            boolean placeAction) {

        timeSinceLastPlacement += internal.getDeltaTime();

        if (!breakAction && !placeAction) {
            blockBranch.resetBreakTarget();
            return;
        }

        if (timeSinceLastPlacement < placementInterval)
            return;

        WorldItemInstance targetItem = findTargetItem(entity, origin, direction);

        if (targetItem != null) {

            blockBranch.resetBreakTarget();

            if (breakAction && itemBranch.pickUp(entity, targetItem))
                timeSinceLastPlacement = 0;

            return;
        }

        if (!castFrom(entity, origin, direction)) {
            blockBranch.resetBreakTarget();
            return;
        }

        if (breakAction) {
            if (handleBreakAction(entity, castStruct))
                timeSinceLastPlacement = 0;
            return;
        }

        if (placeAction) {
            blockBranch.resetBreakTarget();
            if (handlePlaceAction(entity, direction, castStruct))
                timeSinceLastPlacement = 0;
        }
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

    private boolean handleBreakAction(EntityInstance entity, BlockCastStruct castStruct) {
        return blockBranch.tryBreak(entity, castStruct);
    }

    private boolean handlePlaceAction(EntityInstance entity, Vector3 direction, BlockCastStruct castStruct) {
        return itemBranch.place(entity, direction, castStruct);
    }
}