package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.vehiclepipeline.util.VehicleSpaceUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCargoInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehicleGridStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehicleRiderStruct;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public class VehicleRiderSystem extends SystemPackage {

    /*
     * Everything an entity does aboard a vehicle. A rider's feet are kept in
     * its vehicle's model blocks, and once the vehicles have stepped each
     * rider is carried to where its feet now stand on the deck, the turn the
     * deck made under it gathered for its view. MovementManager asks here
     * whether an entity holds a ladder, climbs it in place of falling, and
     * sweeps every move against nearby vehicles in their own frames: the
     * entity's box and move are turned into the vehicle's model axes, swept
     * through its solid sub-voxels and the rough boxes of solid cargo one axis
     * at a time with the same skin and stair step the world uses, and turned
     * back. An entity that lands on a deck, or climbs, boards that vehicle; a
     * rider in the air stays aboard while it stays over the vehicle, so a jump
     * on a moving deck comes down where it left, and one that swims or leaves
     * the vehicle's reach goes ashore.
     */

    // Internal
    private VehicleManager vehicleManager;

    // Riders
    private Object2ObjectOpenHashMap<EntityInstance, VehicleRiderStruct> entity2VehicleRider;

    // Move — the vehicle the entity moving now stood on or held the ladder of, null when none
    private VehicleInstance groundVehicle;
    private VehicleInstance climbVehicle;

    // Settings
    private float resolution;
    private float skin;
    private float stepHeight;

    // Box — min and max corner in model blocks, indexed by axis
    private float[] boxMin;
    private float[] boxMax;

    // Cargo Boxes — min and max corner of each solid cargo box near the move, six floats each
    private FloatArrayList cargoBoxes;

    // Scratch
    private Vector3 offsetScratch;
    private Vector3 modelScratch;
    private Vector3 moveScratch;
    private int[] cellScratch;

    // Base \\

    @Override
    protected void create() {

        // Riders
        this.entity2VehicleRider = new Object2ObjectOpenHashMap<>();

        // Settings
        this.resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        this.skin = EngineSetting.COLLISION_SKIN_BLOCKS;
        this.stepHeight = EngineSetting.STEP_UP_HEIGHT_BLOCKS;

        // Box
        this.boxMin = new float[EngineSetting.AXIS_COUNT];
        this.boxMax = new float[EngineSetting.AXIS_COUNT];

        // Cargo Boxes
        this.cargoBoxes = new FloatArrayList();

        // Scratch
        this.offsetScratch = new Vector3();
        this.modelScratch = new Vector3();
        this.moveScratch = new Vector3();
        this.cellScratch = new int[EngineSetting.AXIS_COUNT];
    }

    @Override
    protected void get() {
        this.vehicleManager = get(VehicleManager.class);
    }

    // Carry \\

    // Every rider placed where its feet now stand on its deck, and the turn under it gathered
    void carryRiders() {

        ObjectIterator<Object2ObjectMap.Entry<EntityInstance, VehicleRiderStruct>> iterator = entity2VehicleRider
                .object2ObjectEntrySet()
                .fastIterator();

        while (iterator.hasNext()) {

            Object2ObjectMap.Entry<EntityInstance, VehicleRiderStruct> entry = iterator.next();
            VehicleRiderStruct rider = entry.getValue();
            VehicleInstance vehicle = rider.getVehicleInstance();

            if (!vehicleManager.getVehicles().contains(vehicle)) {
                iterator.remove();
                continue;
            }

            placeOnDeck(entry.getKey(), rider, vehicle);
        }
    }

    private void placeOnDeck(EntityInstance entity, VehicleRiderStruct rider, VehicleInstance vehicle) {

        Vector3 feet = rider.getFeet();
        Vector3 size = entity.getSize();
        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();
        WorldPositionStruct vehiclePosition = vehicle.getWorldPositionStruct();

        VehicleSpaceUtility.modelToOffset(vehicle, feet.x, feet.y, feet.z, offsetScratch);

        worldPosition.getPosition().set(
                vehiclePosition.getPosition().x + offsetScratch.x - size.x * 0.5f,
                vehiclePosition.getPosition().y + offsetScratch.y,
                vehiclePosition.getPosition().z + offsetScratch.z - size.z * 0.5f);
        worldPosition.setChunkCoordinate(vehiclePosition.getChunkCoordinate());
        WorldPositionUtility.settleChunk(entity.getWorldHandle(), worldPosition);

        float heading = VehicleSpaceUtility.resolveHeading(vehicle);

        rider.addTurn(wrapAngle(heading - rider.getHeading()));
        rider.setHeading(heading);
    }

    // Climb \\

    // Whether the entity holds a ladder this move: inside a climb zone, and climbing or hanging off the ground; in
    // the water only a climber pulling at the ladder holds it, so a swimmer passing by swims on
    public boolean refreshClimb(EntityInstance entity, boolean touchingLiquid) {

        this.climbVehicle = null;
        this.groundVehicle = null;

        EntityInputHandle input = entity.getEntityInputHandle();
        boolean climbing = input.isForward() || input.isBack() || input.isJump();

        if (!climbing && (touchingLiquid || entity.getEntityStateHandle().isGrounded()))
            return false;

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();
        Vector3 size = entity.getSize();
        ObjectArrayList<VehicleInstance> vehicles = vehicleManager.getVehicles();

        for (int i = 0; i < vehicles.size(); i++) {

            VehicleInstance vehicle = vehicles.get(i);

            if (!isNear(vehicle, entity, 0f))
                continue;

            VehicleSpaceUtility.toModel(
                    vehicle,
                    worldPosition.getChunkCoordinate(),
                    position.x + size.x * 0.5f,
                    position.y + size.y * 0.5f,
                    position.z + size.z * 0.5f,
                    modelScratch);

            if (isInClimbZone(vehicle.getVehicleHandle(), modelScratch)) {
                this.climbVehicle = vehicle;
                return true;
            }
        }

        return false;
    }

    private boolean isInClimbZone(VehicleHandle vehicleHandle, Vector3 model) {

        float x = model.x * resolution;
        float y = model.y * resolution;
        float z = model.z * resolution;

        for (int zone = 0; zone < vehicleHandle.getClimbZoneCount(); zone++)
            if (x >= vehicleHandle.getClimbZoneBound(zone, EngineSetting.BOX_MIN_X)
                    && y >= vehicleHandle.getClimbZoneBound(zone, EngineSetting.BOX_MIN_Y)
                    && z >= vehicleHandle.getClimbZoneBound(zone, EngineSetting.BOX_MIN_Z)
                    && x < vehicleHandle.getClimbZoneBound(zone, EngineSetting.BOX_MAX_X)
                    && y < vehicleHandle.getClimbZoneBound(zone, EngineSetting.BOX_MAX_Y)
                    && z < vehicleHandle.getClimbZoneBound(zone, EngineSetting.BOX_MAX_Z))
                return true;

        return false;
    }

    // Up or down the vehicle's own up axis in place of falling, holding still on the ladder without input; only a
    // climber holding still can step off sideways
    public void climb(Vector3 movement, EntityInstance entity) {

        EntityInputHandle input = entity.getEntityInputHandle();
        EntityStateHandle state = entity.getEntityStateHandle();
        int direction = input.isForward() || input.isJump() ? 1 : (input.isBack() ? -1 : 0);
        float share = direction == 0 ? EngineSetting.VEHICLE_CLIMB_SIDESTEP_SHARE : 0f;
        float rise = direction * EngineSetting.VEHICLE_CLIMB_SPEED * internal.getDeltaTime();

        state.getGravityVelocity().set(0f, 0f, 0f);
        state.getHorizontalVelocity().multiply(share);
        state.setMovementState(EntityState.IDLE);

        VehicleSpaceUtility.toWorldDirection(climbVehicle, 0f, 1f, 0f, moveScratch);
        movement.set(
                movement.x * share + moveScratch.x * rise,
                moveScratch.y * rise,
                movement.z * share + moveScratch.z * rise);
    }

    // Collide \\

    // Sweeps the move against every vehicle near it, true when it came to stand on a deck
    public boolean collide(Vector3 movement, EntityInstance entity) {

        ObjectArrayList<VehicleInstance> vehicles = vehicleManager.getVehicles();
        float reach = movement.length();

        for (int i = 0; i < vehicles.size(); i++) {

            VehicleInstance vehicle = vehicles.get(i);

            if (isNear(vehicle, entity, reach) && collideWith(vehicle, movement, entity))
                this.groundVehicle = vehicle;
        }

        return groundVehicle != null;
    }

    // The move swept through one vehicle in its own frame — true when the vehicle's deck stopped it falling
    private boolean collideWith(VehicleInstance vehicle, Vector3 movement, EntityInstance entity) {

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();
        Vector3 size = entity.getSize();
        VehicleGridStruct grid = vehicle.getVehicleHandle().getSolidGrid();

        VehicleSpaceUtility.toModel(
                vehicle,
                worldPosition.getChunkCoordinate(),
                position.x + size.x * 0.5f,
                position.y + size.y * 0.5f,
                position.z + size.z * 0.5f,
                modelScratch);
        VehicleSpaceUtility.toModelDirection(vehicle, movement.x, movement.y, movement.z, moveScratch);

        setBox(modelScratch, size);
        gatherCargoBoxes(vehicle, moveScratch);

        float requestedX = moveScratch.x;
        float requestedY = moveScratch.y;
        float requestedZ = moveScratch.z;

        float moveY = sweep(grid, EngineSetting.AXIS_Y, requestedY);
        translate(EngineSetting.AXIS_Y, moveY);

        float moveX = sweep(grid, EngineSetting.AXIS_X, requestedX);
        translate(EngineSetting.AXIS_X, moveX);

        float moveZ = sweep(grid, EngineSetting.AXIS_Z, requestedZ);
        translate(EngineSetting.AXIS_Z, moveZ);

        boolean landed = requestedY < 0f && moveY > requestedY + skin;
        boolean blocked = moveX != requestedX || moveZ != requestedZ;

        if (blocked && entity.getEntityStateHandle().isGrounded())
            stepUp(grid, moveX, moveY, moveZ, entity);
        else
            moveScratch.set(moveX, moveY, moveZ);

        if (moveScratch.x == requestedX && moveScratch.y == requestedY && moveScratch.z == requestedZ)
            return false;

        VehicleSpaceUtility.toWorldDirection(vehicle, moveScratch.x, moveScratch.y, moveScratch.z, movement);

        return landed;
    }

    // A grounded entity cut short tries the move again a stair step higher, easing the lift into its ground offset
    private void stepUp(VehicleGridStruct grid, float moveX, float moveY, float moveZ, EntityInstance entity) {

        float intendedX = moveScratch.x;
        float intendedZ = moveScratch.z;

        translate(EngineSetting.AXIS_X, -moveX);
        translate(EngineSetting.AXIS_Z, -moveZ);

        float rise = sweep(grid, EngineSetting.AXIS_Y, stepHeight);
        translate(EngineSetting.AXIS_Y, rise);

        float stepX = sweep(grid, EngineSetting.AXIS_X, intendedX);
        translate(EngineSetting.AXIS_X, stepX);

        float stepZ = sweep(grid, EngineSetting.AXIS_Z, intendedZ);
        translate(EngineSetting.AXIS_Z, stepZ);

        float settle = sweep(grid, EngineSetting.AXIS_Y, -rise);
        float lift = rise + settle;

        boolean climbed = lift > skin
                && horizontalDistance(stepX, stepZ) > horizontalDistance(moveX, moveZ) + skin;

        if (!climbed) {
            moveScratch.set(moveX, moveY, moveZ);
            return;
        }

        moveScratch.set(stepX, moveY + lift, stepZ);

        EntityStateHandle state = entity.getEntityStateHandle();
        state.setGroundOffset(state.getGroundOffset() - lift);
    }

    private float horizontalDistance(float x, float z) {
        return (float) Math.sqrt(x * x + z * z);
    }

    // Cargo Boxes \\

    private void gatherCargoBoxes(VehicleInstance vehicle, Vector3 move) {

        cargoBoxes.clear();

        ObjectArrayList<VehicleCargoInstance> cargo = vehicle.getCargo();

        for (int i = 0; i < cargo.size(); i++) {

            VehicleCargoInstance item = cargo.get(i);

            if (!item.getItemInstance().getItemDefinitionHandle().isSolid())
                continue;

            float minX = item.getMinX() / resolution;
            float minY = item.getMinY() / resolution;
            float minZ = item.getMinZ() / resolution;
            float maxX = item.getMaxX() / resolution;
            float maxY = item.getMaxY() / resolution;
            float maxZ = item.getMaxZ() / resolution;

            if (maxX <= boxMin[EngineSetting.AXIS_X] + Math.min(move.x, 0f) - skin
                    || minX >= boxMax[EngineSetting.AXIS_X] + Math.max(move.x, 0f) + skin
                    || maxY <= boxMin[EngineSetting.AXIS_Y] + Math.min(move.y, 0f) - skin
                    || minY >= boxMax[EngineSetting.AXIS_Y] + Math.max(move.y, 0f) + stepHeight + skin
                    || maxZ <= boxMin[EngineSetting.AXIS_Z] + Math.min(move.z, 0f) - skin
                    || minZ >= boxMax[EngineSetting.AXIS_Z] + Math.max(move.z, 0f) + skin)
                continue;

            cargoBoxes.add(minX);
            cargoBoxes.add(minY);
            cargoBoxes.add(minZ);
            cargoBoxes.add(maxX);
            cargoBoxes.add(maxY);
            cargoBoxes.add(maxZ);
        }
    }

    // The distance left once the first cargo box ahead along the axis stops it
    private float sweepCargo(int axis, float distance) {

        if (distance == 0f || cargoBoxes.isEmpty())
            return distance;

        int tangentA = (axis + 1) % EngineSetting.AXIS_COUNT;
        int tangentB = (axis + 2) % EngineSetting.AXIS_COUNT;
        float[] boxes = cargoBoxes.elements();

        for (int box = 0; box < cargoBoxes.size(); box += EngineSetting.BOX_INT_STRIDE) {

            int min = box;
            int max = box + EngineSetting.AXIS_COUNT;

            if (boxes[max + tangentA] <= boxMin[tangentA] + skin || boxes[min + tangentA] >= boxMax[tangentA] - skin)
                continue;

            if (boxes[max + tangentB] <= boxMin[tangentB] + skin || boxes[min + tangentB] >= boxMax[tangentB] - skin)
                continue;

            if (distance > 0f) {

                float face = boxes[min + axis];
                float lead = boxMax[axis];

                if (face >= lead - skin)
                    distance = Math.min(distance, Math.max(face - lead - skin, 0f));

                continue;
            }

            float face = boxes[max + axis];
            float lead = boxMin[axis];

            if (face <= lead + skin)
                distance = Math.max(distance, -Math.max(lead - face - skin, 0f));
        }

        return distance;
    }

    // Sweep \\

    private float sweep(VehicleGridStruct grid, int axis, float distance) {
        return sweepCargo(axis, sweepGrid(grid, axis, distance));
    }

    private float sweepGrid(VehicleGridStruct grid, int axis, float distance) {

        if (distance == 0f)
            return 0f;

        int tangentA = (axis + 1) % EngineSetting.AXIS_COUNT;
        int tangentB = (axis + 2) % EngineSetting.AXIS_COUNT;

        int firstA = toSubVoxel(boxMin[tangentA] + skin);
        int lastA = toSubVoxel(boxMax[tangentA] - skin);
        int firstB = toSubVoxel(boxMin[tangentB] + skin);
        int lastB = toSubVoxel(boxMax[tangentB] - skin);

        if (distance > 0f) {

            float lead = boxMax[axis];
            int lastLayer = toSubVoxel(lead + distance);

            for (int layer = toSubVoxel(lead - skin); layer <= lastLayer; layer++) {

                float face = layer / resolution;

                if (face < lead - skin)
                    continue;

                if (isLayerSolid(grid, axis, layer, tangentA, firstA, lastA, tangentB, firstB, lastB))
                    return Math.min(distance, Math.max(face - lead - skin, 0f));
            }

            return distance;
        }

        float lead = boxMin[axis];
        int lastLayer = toSubVoxel(lead + distance);

        for (int layer = toSubVoxel(lead + skin); layer >= lastLayer; layer--) {

            float face = (layer + 1) / resolution;

            if (face > lead + skin)
                continue;

            if (isLayerSolid(grid, axis, layer, tangentA, firstA, lastA, tangentB, firstB, lastB))
                return Math.max(distance, -Math.max(lead - face - skin, 0f));
        }

        return distance;
    }

    private boolean isLayerSolid(
            VehicleGridStruct grid,
            int axis, int layer,
            int tangentA, int firstA, int lastA,
            int tangentB, int firstB, int lastB) {

        cellScratch[axis] = layer;

        for (int a = firstA; a <= lastA; a++)
            for (int b = firstB; b <= lastB; b++) {

                cellScratch[tangentA] = a;
                cellScratch[tangentB] = b;

                if (grid.isFilled(
                        cellScratch[EngineSetting.AXIS_X],
                        cellScratch[EngineSetting.AXIS_Y],
                        cellScratch[EngineSetting.AXIS_Z]))
                    return true;
            }

        return false;
    }

    // Box \\

    // The entity's box around its centre in model blocks, kept upright in the vehicle's frame
    private void setBox(Vector3 center, Vector3 size) {

        boxMin[EngineSetting.AXIS_X] = center.x - size.x * 0.5f;
        boxMin[EngineSetting.AXIS_Y] = center.y - size.y * 0.5f;
        boxMin[EngineSetting.AXIS_Z] = center.z - size.z * 0.5f;

        boxMax[EngineSetting.AXIS_X] = center.x + size.x * 0.5f;
        boxMax[EngineSetting.AXIS_Y] = center.y + size.y * 0.5f;
        boxMax[EngineSetting.AXIS_Z] = center.z + size.z * 0.5f;
    }

    private void translate(int axis, float distance) {
        boxMin[axis] += distance;
        boxMax[axis] += distance;
    }

    // Record \\

    // After the move: an entity that stood on a deck or held a ladder boards that vehicle; a rider in the air stays
    // aboard over its vehicle; one that swims or leaves its reach goes ashore
    public void record(EntityInstance entity, boolean swimming) {

        VehicleInstance vehicle = climbVehicle != null ? climbVehicle : groundVehicle;
        VehicleRiderStruct rider = entity2VehicleRider.get(entity);

        if (vehicle == null && rider != null && !swimming
                && isNear(rider.getVehicleInstance(), entity, EngineSetting.VEHICLE_RIDER_REACH))
            vehicle = rider.getVehicleInstance();

        if (vehicle == null) {
            entity2VehicleRider.remove(entity);
            return;
        }

        if (rider == null) {
            rider = new VehicleRiderStruct();
            entity2VehicleRider.put(entity, rider);
        }

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();
        Vector3 size = entity.getSize();

        rider.board(vehicle);
        rider.setHeading(VehicleSpaceUtility.resolveHeading(vehicle));
        VehicleSpaceUtility.toModel(
                vehicle,
                worldPosition.getChunkCoordinate(),
                position.x + size.x * 0.5f,
                position.y,
                position.z + size.z * 0.5f,
                rider.getFeet());
    }

    // Riders \\

    // The turn the deck made under an entity since this was last asked, zero for an entity ashore
    public float takeTurn(EntityInstance entity) {

        VehicleRiderStruct rider = entity2VehicleRider.get(entity);

        return rider == null ? 0f : rider.takeTurn();
    }

    public boolean isRiding(EntityInstance entity) {
        return entity2VehicleRider.containsKey(entity);
    }

    // Every rider of a vehicle leaving the world goes ashore where it stands
    void releaseVehicle(VehicleInstance vehicle) {
        entity2VehicleRider.values().removeIf(rider -> rider.getVehicleInstance() == vehicle);
    }

    // Utility \\

    // Whether the entity lies within the vehicle's bounding radius plus its own size and the margin given
    private boolean isNear(VehicleInstance vehicle, EntityInstance entity, float margin) {

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();
        Vector3 size = entity.getSize();

        if (vehicle.getWorldHandle() != entity.getWorldHandle())
            return false;

        VehicleSpaceUtility.toOffset(
                vehicle,
                worldPosition.getChunkCoordinate(),
                position.x + size.x * 0.5f,
                position.y + size.y * 0.5f,
                position.z + size.z * 0.5f,
                offsetScratch);

        float reach = vehicle.getVehicleHandle().getBoundingRadius() + size.length() + margin;

        return offsetScratch.lengthSquared() <= reach * reach;
    }

    private int toSubVoxel(float blocks) {
        return (int) Math.floor(blocks * resolution);
    }

    private float wrapAngle(float angle) {

        float turn = (float) Math.PI * 2f;

        return angle - turn * (float) Math.floor((angle + Math.PI) / turn);
    }
}
