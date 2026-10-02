package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.itempipeline.itemdefinition.LidClearanceStruct;
import application.bootstrap.itempipeline.itemrotationmanager.ItemRotationBufferSystem;
import application.bootstrap.vehiclepipeline.util.VehicleSpaceUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCargoInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCastStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleGridStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VehicleCargoSystem extends SystemPackage {

    /*
     * Items aboard a vehicle, held to every rule a world item keeps. cast()
     * finds the vehicle surface or cargo an entity aims at, nearer than
     * anything in the world; place() sets one item from the main hand against
     * the face the ray met, turned and pushed out along that face exactly as a
     * world item is, on the vehicle's own sub-voxel grid, where its turned
     * shape claims no solid sub-voxel, no other cargo and no open lid's
     * clearance; land() rests a thrown item on the vehicle the same way, from
     * a corner, pushed up until it fits; pickUp() hands the real item back,
     * contents and all, once it fits. A container aboard opens where it lies
     * once nothing rests in its lid's way, and claims that clearance while it
     * stands open. Cargo lives in the vehicle's frame, so all of this holds
     * while the vehicle sails and rolls, and wherever the vehicle goes the
     * cargo stays exactly where it was set down.
     */

    // Internal
    private VehicleManager vehicleManager;
    private VehicleCastSystem vehicleCastSystem;
    private ItemRotationBufferSystem itemRotationBufferSystem;

    // Cast
    private VehicleCastStruct castStruct;

    // Scratch
    private Vector3 directionScratch;
    private Matrix4 cargoMatrix;
    private int[] cornerScratch;
    private int[] anchorScratch;
    private int[] faceScratch;

    // Base \\

    @Override
    protected void create() {

        // Cast
        this.castStruct = new VehicleCastStruct();

        // Scratch
        this.directionScratch = new Vector3();
        this.cargoMatrix = new Matrix4();
        this.cornerScratch = new int[EngineSetting.AXIS_COUNT];
        this.anchorScratch = new int[EngineSetting.AXIS_COUNT];
        this.faceScratch = new int[EngineSetting.AXIS_COUNT];
    }

    @Override
    protected void get() {
        this.vehicleManager = get(VehicleManager.class);
        this.vehicleCastSystem = get(VehicleCastSystem.class);
        this.itemRotationBufferSystem = get(ItemRotationBufferSystem.class);
    }

    // Cast \\

    // True when the entity aims at a vehicle's surface, cargo or canvas nearer than the distance given
    public boolean cast(EntityInstance entity, Vector3 origin, Vector3 direction, float maxDistance) {

        vehicleCastSystem.cast(
                entity.getWorldHandle(),
                entity.getWorldPositionStruct().getChunkCoordinate(),
                origin,
                direction,
                maxDistance,
                true,
                castStruct);

        return castStruct.isHit();
    }

    // True when a ray meets a vehicle's solid surface or cargo nearer than the distance given, the hit written out
    public boolean castSolid(
            WorldHandle worldHandle,
            long chunkCoordinate,
            Vector3 origin,
            Vector3 direction,
            float maxDistance,
            VehicleCastStruct out) {

        vehicleCastSystem.cast(worldHandle, chunkCoordinate, origin, direction, maxDistance, false, out);

        return out.isHit();
    }

    public boolean isCargoHit() {
        return castStruct.isCargoHit();
    }

    public VehicleInstance getCastVehicle() {
        return castStruct.getVehicleInstance();
    }

    public VehicleCargoInstance getCastCargo() {
        return castStruct.getCargoInstance();
    }

    // Pick Up \\

    // The cargo cast() last met handed to the entity, and taken off the vehicle once it fits
    public boolean pickUp(EntityInstance entity) {

        if (!castStruct.isCargoHit())
            return false;

        VehicleCargoInstance cargo = castStruct.getCargoInstance();

        if (!entity.getInventoryHandle().give(cargo.getItemInstance()))
            return false;

        castStruct.getVehicleInstance().getCargo().remove(cargo);

        return true;
    }

    // Place \\

    // One item from the main hand set against the face cast() last met, once the vehicle has room for it
    public boolean place(EntityInstance entity, Vector3 direction) {

        InventoryHandle inventoryHandle = entity.getInventoryHandle();

        if (!castStruct.isFaceHit() || !inventoryHandle.hasMainHand())
            return false;

        ItemDefinitionHandle item = inventoryHandle.getMainHand().getItemDefinitionHandle();

        if (item.isBlockPiece())
            return false;

        VehicleInstance vehicle = castStruct.getVehicleInstance();
        Direction3Vector face = castStruct.getHitFace();

        VehicleSpaceUtility.toModelDirection(vehicle, direction.x, direction.y, direction.z, directionScratch);

        int orientation = itemRotationBufferSystem.resolvePlacementOrientation(face, directionScratch);

        if (!resolvePlacement(vehicle, item.getShape(), orientation, face))
            return false;

        addCargo(vehicle, inventoryHandle.takeOne(EquipmentSlot.MAIN_HAND), orientation);

        return true;
    }

    // The shape flush on the face, centred on the anchor outside it, pushed out along the face until it fits
    private boolean resolvePlacement(
            VehicleInstance vehicle,
            ItemShapeStruct shape,
            int orientation,
            Direction3Vector face) {

        anchorScratch[EngineSetting.AXIS_X] = castStruct.getAnchorX();
        anchorScratch[EngineSetting.AXIS_Y] = castStruct.getAnchorY();
        anchorScratch[EngineSetting.AXIS_Z] = castStruct.getAnchorZ();
        faceScratch[EngineSetting.AXIS_X] = face.x;
        faceScratch[EngineSetting.AXIS_Y] = face.y;
        faceScratch[EngineSetting.AXIS_Z] = face.z;

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
            cornerScratch[axis] = itemRotationBufferSystem.resolveFlushCorner(
                    shape, orientation, axis, anchorScratch[axis], faceScratch[axis]);

        return pushUntilFits(vehicle, shape, orientation);
    }

    // Land \\

    // A thrown item rested on the vehicle, its model grid cornered here and pushed up until it fits
    public boolean land(
            VehicleInstance vehicle,
            ItemInstance itemInstance,
            int orientation,
            int cornerX,
            int cornerY,
            int cornerZ) {

        cornerScratch[EngineSetting.AXIS_X] = cornerX;
        cornerScratch[EngineSetting.AXIS_Y] = cornerY;
        cornerScratch[EngineSetting.AXIS_Z] = cornerZ;
        faceScratch[EngineSetting.AXIS_X] = Direction3Vector.UP.x;
        faceScratch[EngineSetting.AXIS_Y] = Direction3Vector.UP.y;
        faceScratch[EngineSetting.AXIS_Z] = Direction3Vector.UP.z;

        if (!pushUntilFits(vehicle, itemInstance.getItemDefinitionHandle().getShape(), orientation))
            return false;

        addCargo(vehicle, itemInstance, orientation);

        return true;
    }

    // The corner pushed along the face until the turned shape fits there
    private boolean pushUntilFits(VehicleInstance vehicle, ItemShapeStruct shape, int orientation) {

        for (int push = 0; push <= EngineSetting.ITEM_PLACEMENT_PUSH_LIMIT; push++) {

            if (fits(vehicle, shape, orientation))
                return true;

            for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
                cornerScratch[axis] += faceScratch[axis];
        }

        return false;
    }

    // The one way an item comes aboard: cornered where the last fit left it
    private void addCargo(VehicleInstance vehicle, ItemInstance itemInstance, int orientation) {

        VehicleCargoInstance cargo = create(VehicleCargoInstance.class);
        cargo.constructor(
                itemInstance,
                cornerScratch[EngineSetting.AXIS_X],
                cornerScratch[EngineSetting.AXIS_Y],
                cornerScratch[EngineSetting.AXIS_Z],
                orientation);
        resolveBounds(cargo);

        vehicle.getCargo().add(cargo);
    }

    // Open \\

    // True when the cargo cast() last met is a container that can open where it lies
    public boolean canOpenCastCargo() {
        return castStruct.isCargoHit() && canOpen(castStruct.getVehicleInstance(), castStruct.getCargoInstance());
    }

    public boolean canOpen(VehicleInstance vehicle, VehicleCargoInstance cargo) {
        return cargo.getItemInstance().getItemDefinitionHandle().isContainer() && isLidClear(vehicle, cargo);
    }

    // True when nothing solid, no other cargo and no other open lid fills the container's lid clearance
    private boolean isLidClear(VehicleInstance vehicle, VehicleCargoInstance container) {

        LidClearanceStruct clearance = container.getItemInstance().getItemDefinitionHandle()
                .getContainerSpace().getLidClearance();
        VehicleGridStruct grid = vehicle.getVehicleHandle().getSolidGrid();
        int orientation = container.getOrientation();

        for (int i = 0; i < clearance.getCellCount(); i++) {

            int gridX = clearance.getCellX(i);
            int gridY = clearance.getCellY(i);
            int gridZ = clearance.getCellZ(i);

            int x = container.getCornerX()
                    + itemRotationBufferSystem.rotateCell(orientation, EngineSetting.AXIS_X, gridX, gridY, gridZ);
            int y = container.getCornerY()
                    + itemRotationBufferSystem.rotateCell(orientation, EngineSetting.AXIS_Y, gridX, gridY, gridZ);
            int z = container.getCornerZ()
                    + itemRotationBufferSystem.rotateCell(orientation, EngineSetting.AXIS_Z, gridX, gridY, gridZ);

            if (grid.isFilled(x, y, z) || isCellTaken(vehicle, x, y, z, container))
                return false;
        }

        return true;
    }

    // An open container claims its clearance until it closes, so nothing is set where its lid comes down
    public void setOpen(VehicleCargoInstance cargo, boolean open) {
        cargo.setOpen(open);
    }

    // True while the vehicle is in the world and the cargo still aboard it
    public boolean isAboard(VehicleInstance vehicle, VehicleCargoInstance cargo) {
        return vehicleManager.getVehicles().contains(vehicle) && vehicle.getCargo().contains(cargo);
    }

    // Fit \\

    // True when no cell the turned shape claims at the current corner is solid or taken
    private boolean fits(VehicleInstance vehicle, ItemShapeStruct shape, int orientation) {

        VehicleGridStruct grid = vehicle.getVehicleHandle().getSolidGrid();

        for (int i = 0; i < shape.getCellCount(); i++) {

            int gridX = shape.getOffsetX() + shape.getCellX(i);
            int gridY = shape.getOffsetY() + shape.getCellY(i);
            int gridZ = shape.getOffsetZ() + shape.getCellZ(i);

            int x = cornerScratch[EngineSetting.AXIS_X]
                    + itemRotationBufferSystem.rotateCell(orientation, EngineSetting.AXIS_X, gridX, gridY, gridZ);
            int y = cornerScratch[EngineSetting.AXIS_Y]
                    + itemRotationBufferSystem.rotateCell(orientation, EngineSetting.AXIS_Y, gridX, gridY, gridZ);
            int z = cornerScratch[EngineSetting.AXIS_Z]
                    + itemRotationBufferSystem.rotateCell(orientation, EngineSetting.AXIS_Z, gridX, gridY, gridZ);

            if (grid.isFilled(x, y, z) || isCellTaken(vehicle, x, y, z, null))
                return false;
        }

        return true;
    }

    // True when other cargo claims the cell, or an open container's lid clearance holds it
    private boolean isCellTaken(VehicleInstance vehicle, int x, int y, int z, VehicleCargoInstance ignored) {

        ObjectArrayList<VehicleCargoInstance> cargo = vehicle.getCargo();

        for (int i = 0; i < cargo.size(); i++) {

            VehicleCargoInstance item = cargo.get(i);

            if (item == ignored)
                continue;

            if (claimsCell(item, x, y, z) || item.isOpen() && holdsClearanceCell(item, x, y, z))
                return true;
        }

        return false;
    }

    // True when a cargo item's turned shape claims a model sub-voxel
    private boolean claimsCell(VehicleCargoInstance cargo, int x, int y, int z) {

        if (x < cargo.getMinX() || y < cargo.getMinY() || z < cargo.getMinZ()
                || x >= cargo.getMaxX() || y >= cargo.getMaxY() || z >= cargo.getMaxZ())
            return false;

        int orientation = cargo.getOrientation();
        int gridX = x - cargo.getCornerX();
        int gridY = y - cargo.getCornerY();
        int gridZ = z - cargo.getCornerZ();

        return cargo.getItemInstance().getItemDefinitionHandle().getShape().claimsGridCell(
                itemRotationBufferSystem.unrotateCell(orientation, EngineSetting.AXIS_X, gridX, gridY, gridZ),
                itemRotationBufferSystem.unrotateCell(orientation, EngineSetting.AXIS_Y, gridX, gridY, gridZ),
                itemRotationBufferSystem.unrotateCell(orientation, EngineSetting.AXIS_Z, gridX, gridY, gridZ));
    }

    // True when an open container's turned lid clearance holds a model sub-voxel
    private boolean holdsClearanceCell(VehicleCargoInstance container, int x, int y, int z) {

        int orientation = container.getOrientation();
        int gridX = x - container.getCornerX();
        int gridY = y - container.getCornerY();
        int gridZ = z - container.getCornerZ();

        return container.getItemInstance().getItemDefinitionHandle().getContainerSpace().getLidClearance().holdsCell(
                itemRotationBufferSystem.unrotateCell(orientation, EngineSetting.AXIS_X, gridX, gridY, gridZ),
                itemRotationBufferSystem.unrotateCell(orientation, EngineSetting.AXIS_Y, gridX, gridY, gridZ),
                itemRotationBufferSystem.unrotateCell(orientation, EngineSetting.AXIS_Z, gridX, gridY, gridZ));
    }

    // The rough box around every cell the cargo's turned shape claims
    private void resolveBounds(VehicleCargoInstance cargo) {

        ItemShapeStruct shape = cargo.getItemInstance().getItemDefinitionHandle().getShape();
        int orientation = cargo.getOrientation();

        cargo.setBounds(
                cargo.getCornerX() + itemRotationBufferSystem.getShapeMin(shape, orientation, EngineSetting.AXIS_X),
                cargo.getCornerY() + itemRotationBufferSystem.getShapeMin(shape, orientation, EngineSetting.AXIS_Y),
                cargo.getCornerZ() + itemRotationBufferSystem.getShapeMin(shape, orientation, EngineSetting.AXIS_Z),
                cargo.getCornerX() + itemRotationBufferSystem.getShapeMax(shape, orientation, EngineSetting.AXIS_X)
                        + 1,
                cargo.getCornerY() + itemRotationBufferSystem.getShapeMax(shape, orientation, EngineSetting.AXIS_Y)
                        + 1,
                cargo.getCornerZ() + itemRotationBufferSystem.getShapeMax(shape, orientation, EngineSetting.AXIS_Z)
                        + 1);
    }

    // Transform \\

    // Where a cargo item stands in the vehicle's model blocks: its corner and its turn on the vehicle's grid
    public Matrix4 composeCargoMatrix(VehicleCargoInstance cargo, Matrix4 out) {

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        return itemRotationBufferSystem.composeTransform(
                cargo.getCornerX() / resolution,
                cargo.getCornerY() / resolution,
                cargo.getCornerZ() / resolution,
                cargo.getOrientation(),
                out);
    }

    // Where a cargo item stands relative to a viewer's chunk — false beyond the radius vehicles are drawn in
    public boolean composeTransform(
            VehicleInstance vehicle,
            VehicleCargoInstance cargo,
            long viewerChunkCoordinate,
            Matrix4 out) {

        if (!VehicleSpaceUtility.composeModelMatrix(
                vehicle, viewerChunkCoordinate, EngineSetting.VEHICLE_RENDER_CHUNK_RADIUS, out))
            return false;

        out.multiply(composeCargoMatrix(cargo, cargoMatrix));

        return true;
    }
}
