package application.bootstrap.entitypipeline.placementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import engine.root.ManagerPackage;
import engine.settings.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

public class PlacementManager extends ManagerPackage {

    /*
     * Entity-agnostic placement manager. Owns the raycast result, placement
     * cooldown, and routes to BlockBranch or ItemBranch based on the action.
     * Player passes mouse input. Enemies pass AI input. Same code path either way.
     */

    // Internal
    private RaycastManager raycastManager;

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

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();

        raycastManager.castBlock(
                worldPosition.getChunkCoordinate(),
                origin,
                direction,
                entity.getStatisticsHandle().getReach() * EngineSetting.REACH_SCALE,
                castStruct);

        if (!castStruct.isHit()) {
            blockBranch.resetBreakTarget();
            return;
        }

        if (breakAction) {
            blockBranch.resetBreakTarget();
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

    // Routing \\

    // TODO: route based on main hand contents
    private boolean handleBreakAction(EntityInstance entity, BlockCastStruct castStruct) {
        // TODO: check main hand — tool routes to BlockBranch, etc.
        return false;
    }

    private boolean handlePlaceAction(EntityInstance entity, Vector3 direction, BlockCastStruct castStruct) {
        // TODO: check main hand — world item routes to ItemBranch, etc.
        return false;
    }
}