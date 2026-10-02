package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.vehiclepipeline.util.VehicleSpaceUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCastStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartRole;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class VehicleControlSystem extends SystemPackage {

    /*
     * An entity's hands on a vehicle's controls. findControl() decides which
     * control an entity faces within its reach, with no block standing in
     * front of it, a fitting of a control answering for it, and activate()
     * works it: the helm is taken or left, every sail of a yard is set or
     * taken in together, the anchor dropped or weighed, and a door opened or
     * shut. While an entity holds a helm, activate() lets it go wherever the
     * entity looks, and steer() is its turn at the wheel: its sideways input
     * swings the rudder and it stands fast; it lets go once it moves off the
     * helm or starts to swim.
     */

    // Internal
    private VehicleManager vehicleManager;
    private VehicleCastSystem vehicleCastSystem;
    private RaycastManager raycastManager;

    // Cast
    private VehicleCastStruct castStruct;
    private BlockCastStruct blockCastStruct;

    // Scratch
    private Vector3 offsetScratch;

    // Base \\

    @Override
    protected void create() {

        // Cast
        this.castStruct = new VehicleCastStruct();
        this.blockCastStruct = new BlockCastStruct();

        // Scratch
        this.offsetScratch = new Vector3();
    }

    @Override
    protected void get() {
        this.vehicleManager = get(VehicleManager.class);
        this.vehicleCastSystem = get(VehicleCastSystem.class);
        this.raycastManager = get(RaycastManager.class);
    }

    // Steer \\

    // The helmsman's sideways input turns the wheel and every other move is held, until it leaves the helm
    public void steer(EntityInstance entity) {

        VehicleInstance vehicle = vehicleManager.findHelmFor(entity);

        if (vehicle == null)
            return;

        if (!isAtHelm(entity, vehicle)) {
            vehicleManager.releaseHelm(entity);
            return;
        }

        EntityInputHandle input = entity.getEntityInputHandle();

        vehicle.setWheelInput(input.getHorizontalX());
        input.setForward(false);
        input.setBack(false);
        input.setLeft(false);
        input.setRight(false);
        input.setJump(false);
    }

    private boolean isAtHelm(EntityInstance entity, VehicleInstance vehicle) {

        if (entity.getEntityStateHandle().isSwimming() || !vehicle.getVehicleHandle().hasHelm())
            return false;

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();
        Vector3 size = entity.getSize();
        Vector3 pivot = vehicle.getVehicleHandle().getHelm().getPivot();

        VehicleSpaceUtility.toModel(
                vehicle,
                worldPosition.getChunkCoordinate(),
                position.x + size.x * 0.5f,
                position.y + size.y * 0.5f,
                position.z + size.z * 0.5f,
                offsetScratch);

        float reach = entity.getStatisticsHandle().getReach() * EngineSetting.REACH_SCALE
                + EngineSetting.VEHICLE_HELM_SLACK;

        return offsetScratch.subtract(pivot).lengthSquared() <= reach * reach;
    }

    // Activate \\

    // Works the control the entity faces, or lets go of the helm it holds — true when the activation was taken
    public boolean activate(EntityInstance entity, Vector3 origin, Vector3 direction) {

        if (vehicleManager.findHelmFor(entity) != null) {
            vehicleManager.releaseHelm(entity);
            return true;
        }

        if (!findControl(entity, origin, direction))
            return false;

        VehicleInstance vehicle = castStruct.getVehicleInstance();
        int controlIndex = vehicle.getVehicleHandle().getPart(castStruct.getPartIndex()).getControlIndex();
        VehiclePartStruct part = vehicle.getVehicleHandle().getPart(controlIndex);

        switch (part.getRole().getControl()) {
            case STEER -> vehicleManager.takeHelm(vehicle, entity);
            case HOIST -> vehicleManager.toggleYard(vehicle, resolveYard(part, controlIndex));
            case MOOR -> vehicleManager.toggleAnchor(vehicle);
            case OPEN -> vehicleManager.toggleDoor(vehicle, part.getDoorIndex());
            default -> {
                return false;
            }
        }

        return true;
    }

    // A sail answers for the yard it hangs from
    private int resolveYard(VehiclePartStruct part, int partIndex) {
        return part.getRole() == VehiclePartRole.SAIL ? part.getYardIndex() : partIndex;
    }

    // Find \\

    // True when the entity faces a vehicle control within its reach with no block in front of it
    public boolean findControl(EntityInstance entity, Vector3 origin, Vector3 direction) {

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();
        float reach = entity.getStatisticsHandle().getReach() * EngineSetting.REACH_SCALE;

        raycastManager.castBlock(worldPosition.getChunkCoordinate(), origin, direction, reach, blockCastStruct);

        vehicleCastSystem.cast(
                entity.getWorldHandle(),
                worldPosition.getChunkCoordinate(),
                origin,
                direction,
                blockCastStruct.isHit() ? blockCastStruct.getDistance() : reach,
                true,
                castStruct);

        if (!castStruct.isPartHit())
            return false;

        VehiclePartStruct part = castStruct.getVehicleInstance().getVehicleHandle().getPart(castStruct.getPartIndex());

        return part.getControlIndex() != EngineSetting.INDEX_NOT_FOUND;
    }

    public boolean isSteering(EntityInstance entity) {
        return vehicleManager.findHelmFor(entity) != null;
    }
}
