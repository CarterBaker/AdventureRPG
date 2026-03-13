package com.internal.bootstrap.entitypipeline.placementmanager;

import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.entitypipeline.placementmanager.placement.BlockBranch;
import com.internal.bootstrap.entitypipeline.placementmanager.placement.ItemBranch;
import com.internal.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import com.internal.bootstrap.physicspipeline.util.BlockCastStruct;
import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.vectors.Vector3;

/*
 * Entity-agnostic placement manager.
 * Owns the raycast and cooldown, routes to BlockBranch or ItemBranch.
 * Player passes mouse input. Enemies will pass AI input. Same code path either way.
 */
public class PlacementManager extends ManagerPackage {

    // Internal
    private RaycastManager raycastManager;
    private BlockBranch blockBranch;
    private ItemBranch itemBranch;

    private float PLACEMENT_INTERVAL;
    private float timeSinceLastPlacement;

    // Reused per frame
    private final BlockCastStruct castStruct = new BlockCastStruct();

    // Internal \\

    @Override
    protected void create() {
        this.blockBranch = create(BlockBranch.class);
        this.itemBranch = create(ItemBranch.class);
        this.PLACEMENT_INTERVAL = EngineSetting.BLOCK_PLACEMENT_INTERVAL;
        this.timeSinceLastPlacement = PLACEMENT_INTERVAL;
    }

    @Override
    protected void get() {
        this.raycastManager = get(RaycastManager.class);
    }

    // Update \\

    public void update(
            EntityHandle entity,
            Vector3 origin,
            Vector3 direction,
            boolean breakAction,
            boolean placeAction) {

        timeSinceLastPlacement += internal.getDeltaTime();

        if (!breakAction && !placeAction) {
            blockBranch.resetBreakTarget();
            return;
        }

        if (timeSinceLastPlacement < PLACEMENT_INTERVAL)
            return;

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();

        raycastManager.castBlock(
                worldPosition.getChunkCoordinate(),
                origin,
                direction,
                entity.getStatisticsInstance().reach * EngineSetting.REACH_SCALE,
                castStruct);

        if (!castStruct.hit) {
            blockBranch.resetBreakTarget();
            return;
        }

        if (breakAction) {
            blockBranch.resetBreakTarget();
            handleBreakAction(entity, castStruct);
            return;
        }

        if (placeAction) {
            blockBranch.resetBreakTarget();
            handlePlaceAction(entity, direction, castStruct);
        }
    }

    // Routing — TODO: route based on main hand contents \\

    private void handleBreakAction(EntityHandle entity, BlockCastStruct castStruct) {
        // TODO: check main hand — tool routes to BlockBranch, etc.
    }

    private void handlePlaceAction(EntityHandle entity, Vector3 direction, BlockCastStruct castStruct) {
        // TODO: check main hand — world item routes to ItemBranch, etc.
    }
}