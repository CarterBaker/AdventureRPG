package application.bootstrap.entitypipeline.placementmanager;

import application.bootstrap.combatpipeline.combatmanager.CombatManager;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemActionTrigger;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleCargoSystem;
import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.worlditem.WorldItemCastStruct;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemPlacementSystem;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemSpaceSystem;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.vectors.Vector3;

public class PlacementManager extends ManagerPackage {

    /*
     * Entity-agnostic placement manager. Owns the raycast result, placement
     * cooldown, and routes to BlockBranch or ItemBranch based on the action.
     * Player passes mouse input. Enemies pass AI input. Same code path either
     * way. A world item in reach and nearer than any block takes the action
     * first: an action the item carries for the trigger, the held item and the
     * spot struck is worked through ItemActionBranch before anything else, as
     * a door is swung or a cannon loaded and fired; otherwise the primary
     * action picks it up, and the activate action sets the held item against
     * the face it was hit on, so items stack into piles — containers included,
     * unless the container can open, since activating one that can opens it in
     * the runtime, never here. Otherwise the primary action swings whatever is
     * held through CombatManager, whose strike lands back here on a block
     * through strikeBlock(), and the activate action places the held item — a
     * block piece as a sub-block, a seed into the ground as a tree of its
     * species, a sower or nurturer onto the block as the covering it tends,
     * anything else as a world item. A vehicle's
     * deck or cargo nearer than anything in the world takes the action
     * instead, through VehicleCargoSystem, by the same rules: cargo's own
     * actions come first, then the primary action picks cargo up or swings at
     * the deck, and the activate action sets the held item down aboard, unless
     * it is a block piece or the cargo is a container that can open.
     * findTargetVehicle() is the one place that decides whether a vehicle
     * stands in front. Nothing happens here while a stance is held: an aim's
     * throw and a guard belong to CombatManager. findTargetItem() is the one
     * place that decides which world item an entity is aiming at, and where on
     * it.
     */

    // Internal
    private RaycastManager raycastManager;
    private WorldItemSpaceSystem worldItemSpaceSystem;
    private WorldItemPlacementSystem worldItemPlacementSystem;
    private CombatManager combatManager;
    private VehicleCargoSystem vehicleCargoSystem;
    private TreeManager treeManager;

    // Branches
    private BlockBranch blockBranch;
    private CoverageBranch coverageBranch;
    private ItemBranch itemBranch;
    private ItemActionBranch itemActionBranch;

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
        this.coverageBranch = create(CoverageBranch.class);
        this.itemBranch = create(ItemBranch.class);
        this.itemActionBranch = create(ItemActionBranch.class);

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
        this.vehicleCargoSystem = get(VehicleCargoSystem.class);
        this.treeManager = get(TreeManager.class);
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

        if (castVehicleBefore(entity, origin, direction, targetItem)) {
            handleVehicleAction(entity, direction, primaryAction, activateAction);
            return;
        }

        if (targetItem != null) {

            if (itemActionBranch.actOnItem(entity, resolveTrigger(primaryAction), targetItem, itemCastStruct)) {
                timeSinceLastPlacement = 0;
                combatManager.gesture(entity, EntityAction.PLACE);
                return;
            }

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

    // True when a vehicle's surface or cargo stands nearer than anything in the world the entity aims at — the hit
    // stays in VehicleCargoSystem
    public boolean findTargetVehicle(EntityInstance entity, Vector3 origin, Vector3 direction) {
        return castVehicleBefore(entity, origin, direction, findTargetItem(entity, origin, direction));
    }

    private boolean castVehicleBefore(
            EntityInstance entity,
            Vector3 origin,
            Vector3 direction,
            WorldItemInstance targetItem) {
        return vehicleCargoSystem.cast(entity, origin, direction, resolveWorldDistance(entity, targetItem));
    }

    // How far the world findTargetItem() last cast met — its item, else its block, else the entity's reach
    private float resolveWorldDistance(EntityInstance entity, WorldItemInstance targetItem) {

        if (targetItem != null)
            return itemCastStruct.getDistance();

        return castStruct.isHit()
                ? castStruct.getDistance()
                : entity.getStatisticsHandle().getReach() * EngineSetting.REACH_SCALE;
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

        if (held.getItemDefinitionHandle().isSeed())
            return plantSeed(entity, castStruct);

        if (held.getItemDefinitionHandle().isSower() || held.getItemDefinitionHandle().isNurturer())
            return coverageBranch.tryTend(entity, castStruct);

        return itemBranch.place(entity, direction, castStruct);
    }

    // The seed in the main hand set into the top of the block that was hit, rooting a tree in the block above
    private boolean plantSeed(EntityInstance entity, BlockCastStruct castStruct) {

        if (castStruct.getHitFace() != Direction3Vector.UP)
            return false;

        long chunkCoordinate = castStruct.getChunkCoordinate();
        long anchorX = (long) Coordinate2Long.unpackX(chunkCoordinate) * EngineSetting.CHUNK_SIZE
                + castStruct.getBlockX();
        long anchorZ = (long) Coordinate2Long.unpackY(chunkCoordinate) * EngineSetting.CHUNK_SIZE
                + castStruct.getBlockZ();
        int baseY = castStruct.getSubChunkY() * EngineSetting.CHUNK_SIZE + castStruct.getBlockY() + 1;
        InventoryHandle inventoryHandle = entity.getInventoryHandle();
        TreeHandle treeHandle = treeManager.getTreeHandleFromTreeName(
                inventoryHandle.getMainHand().getItemDefinitionHandle().getPlantsTreeName());

        if (!treeManager.plant(treeHandle, entity.getWorldHandle(), anchorX, anchorZ, baseY))
            return false;

        inventoryHandle.takeOne(EquipmentSlot.MAIN_HAND);

        return true;
    }

    // Against the vehicle VehicleCargoSystem last met — the primary action takes cargo up or swings at the deck, the
    // activate action sets the held item down aboard, unless the cargo is a container that can open, which is
    // opened instead
    private void handleVehicleAction(
            EntityInstance entity,
            Vector3 direction,
            boolean primaryAction,
            boolean activateAction) {

        if (itemActionBranch.actOnCargo(entity, resolveTrigger(primaryAction))) {
            timeSinceLastPlacement = 0;
            combatManager.gesture(entity, EntityAction.PLACE);
            return;
        }

        if (primaryAction && !vehicleCargoSystem.isCargoHit()) {
            combatManager.swing(entity);
            return;
        }

        if (primaryAction && vehicleCargoSystem.pickUp(entity)) {
            timeSinceLastPlacement = 0;
            combatManager.gesture(entity, EntityAction.PICK_UP);
            return;
        }

        if (activateAction && !vehicleCargoSystem.canOpenCastCargo() && vehicleCargoSystem.place(entity, direction)) {
            timeSinceLastPlacement = 0;
            combatManager.gesture(entity, EntityAction.PLACE);
        }
    }

    // The primary action strikes, the activate action uses
    private ItemActionTrigger resolveTrigger(boolean primaryAction) {
        return primaryAction ? ItemActionTrigger.STRIKE : ItemActionTrigger.USE;
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