package application.bootstrap.entitypipeline.placementmanager;

import application.bootstrap.combatpipeline.projectilemanager.ProjectileManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemActionStruct;
import application.bootstrap.itempipeline.itemdefinition.ItemActionTrigger;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.itempipeline.itemmanager.ItemManager;
import application.bootstrap.itempipeline.itemrotationmanager.ItemRotationBufferSystem;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCargoInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleCargoSystem;
import application.bootstrap.worldpipeline.worlditem.WorldItemCastStruct;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worlditemmanager.WorldItemPlacementSystem;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate4Long;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;

class ItemActionBranch extends BranchPackage {

    /*
     * Works the actions items carry for PlacementManager, on an item standing
     * in the world or aboard a vehicle alike. The action answers the trigger,
     * the item in the main hand and the cell of the item's model grid the ray
     * struck; it turns the item into the one it names in the same place, and
     * only once that one fits there. Then it uses up one of the held item if
     * it consumes it, and fires its item from the muzzle, turned and carried
     * with the item it is fired from, through ProjectileManager — the same
     * flight a thrown item takes.
     */

    // Internal
    private ItemDefinitionManager itemDefinitionManager;
    private ItemManager itemManager;
    private ItemRotationBufferSystem itemRotationBufferSystem;
    private WorldItemPlacementSystem worldItemPlacementSystem;
    private VehicleCargoSystem vehicleCargoSystem;
    private ProjectileManager projectileManager;

    // Scratch
    private Matrix4 transformScratch;
    private Vector3 muzzleScratch;
    private Vector3 velocityScratch;
    private Vector3 spinAxisScratch;
    private Quaternion orientationScratch;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.transformScratch = new Matrix4();
        this.muzzleScratch = new Vector3();
        this.velocityScratch = new Vector3();
        this.spinAxisScratch = new Vector3(1f, 0f, 0f);
        this.orientationScratch = new Quaternion();
    }

    @Override
    protected void get() {

        // Internal
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.itemManager = get(ItemManager.class);
        this.itemRotationBufferSystem = get(ItemRotationBufferSystem.class);
        this.worldItemPlacementSystem = get(WorldItemPlacementSystem.class);
        this.vehicleCargoSystem = get(VehicleCargoSystem.class);
        this.projectileManager = get(ProjectileManager.class);
    }

    // World \\

    // The action the world item answers where the cast struck it, worked — true when one was
    boolean actOnItem(
            EntityInstance entity,
            ItemActionTrigger trigger,
            WorldItemInstance target,
            WorldItemCastStruct itemCastStruct) {

        Direction3Vector face = itemCastStruct.getHitFace();
        long packed = target.getPackedPosition();
        int orientation = Coordinate4Long.unpackW(packed);
        int x = itemCastStruct.getAnchorX() - face.x - Coordinate4Long.unpackX(packed);
        int y = itemCastStruct.getAnchorY() - face.y - Coordinate4Long.unpackY(packed);
        int z = itemCastStruct.getAnchorZ() - face.z - Coordinate4Long.unpackZ(packed);

        ItemActionStruct action = findAction(
                entity,
                target.getItemDefinitionHandle(),
                trigger,
                itemRotationBufferSystem.unrotateCell(orientation, EngineSetting.AXIS_X, x, y, z),
                itemRotationBufferSystem.unrotateCell(orientation, EngineSetting.AXIS_Y, x, y, z),
                itemRotationBufferSystem.unrotateCell(orientation, EngineSetting.AXIS_Z, x, y, z));

        if (action == null)
            return false;

        WorldItemInstance replaced = worldItemPlacementSystem.replaceItem(
                target, itemDefinitionManager.getItemHandleFromItemName(action.getBecomesItemName()));

        if (replaced == null)
            return false;

        consumeHeld(entity, action);

        if (action.fires() && worldItemPlacementSystem.composeTransform(
                replaced, entity.getWorldHandle(), replaced.getChunkCoordinate(), transformScratch))
            fire(entity, action, replaced.getChunkCoordinate(), null);

        return true;
    }

    // Cargo \\

    // The action the cargo VehicleCargoSystem last cast at answers where it was struck, worked — true when one was
    boolean actOnCargo(EntityInstance entity, ItemActionTrigger trigger) {

        if (!vehicleCargoSystem.isCargoHit())
            return false;

        VehicleInstance vehicle = vehicleCargoSystem.getCastVehicle();
        VehicleCargoInstance target = vehicleCargoSystem.getCastCargo();
        ItemActionStruct action = findAction(
                entity,
                target.getItemInstance().getItemDefinitionHandle(),
                trigger,
                vehicleCargoSystem.getCastGridCell(EngineSetting.AXIS_X),
                vehicleCargoSystem.getCastGridCell(EngineSetting.AXIS_Y),
                vehicleCargoSystem.getCastGridCell(EngineSetting.AXIS_Z));

        if (action == null)
            return false;

        VehicleCargoInstance replaced = vehicleCargoSystem.replaceCargo(
                vehicle, target, itemDefinitionManager.getItemHandleFromItemName(action.getBecomesItemName()));

        if (replaced == null)
            return false;

        consumeHeld(entity, action);

        long frameChunk = entity.getWorldPositionStruct().getChunkCoordinate();

        if (action.fires() && vehicleCargoSystem.composeTransform(vehicle, replaced, frameChunk, transformScratch))
            fire(entity, action, frameChunk, vehicle.getVelocity());

        return true;
    }

    // Action \\

    private ItemActionStruct findAction(
            EntityInstance entity,
            ItemDefinitionHandle target,
            ItemActionTrigger trigger,
            int gridX,
            int gridY,
            int gridZ) {

        if (!target.hasActions())
            return null;

        ItemInstance held = entity.getInventoryHandle().getMainHand();
        String heldName = held != null
                ? held.getItemDefinitionHandle().getItemName()
                : EngineSetting.ITEM_ACTION_HELD_ANY;

        return target.findAction(trigger, heldName, gridX, gridY, gridZ);
    }

    private void consumeHeld(EntityInstance entity, ItemActionStruct action) {

        InventoryHandle inventoryHandle = entity.getInventoryHandle();

        if (action.isConsumingHeld())
            inventoryHandle.takeOne(EquipmentSlot.MAIN_HAND);
    }

    // Fire \\

    // The action's item launched from its muzzle along its aim, both carried through the transform last composed into
    // the frame chunk's blocks, with the speed of whatever carries it added
    private void fire(EntityInstance entity, ItemActionStruct action, long frameChunk, Vector3 carrierVelocity) {

        float[] m = transformScratch.val;
        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        Vector3 muzzle = action.getMuzzle();
        Vector3 aim = action.getAim();
        float x = muzzle.x / resolution;
        float y = muzzle.y / resolution;
        float z = muzzle.z / resolution;
        float speed = action.getFireSpeed();

        muzzleScratch.set(
                m[0] * x + m[4] * y + m[8] * z + m[12],
                m[1] * x + m[5] * y + m[9] * z + m[13],
                m[2] * x + m[6] * y + m[10] * z + m[14]);
        velocityScratch.set(
                (m[0] * aim.x + m[4] * aim.y + m[8] * aim.z) * speed,
                (m[1] * aim.x + m[5] * aim.y + m[9] * aim.z) * speed,
                (m[2] * aim.x + m[6] * aim.y + m[10] * aim.z) * speed);

        if (carrierVelocity != null)
            velocityScratch.add(carrierVelocity.x, carrierVelocity.y, carrierVelocity.z);

        projectileManager.launch(
                entity,
                itemManager.createItem(action.getFireItemName()),
                frameChunk,
                muzzleScratch,
                velocityScratch,
                spinAxisScratch,
                0f,
                orientationScratch);
    }
}
