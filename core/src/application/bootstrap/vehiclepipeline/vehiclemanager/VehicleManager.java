package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.vehiclepipeline.util.VehicleSpaceUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VehicleManager extends ManagerPackage {

    /*
     * Owns the vehicle palette and every vehicle in the world; spawn() and
     * despawn() are the one way a vehicle enters and leaves it. Vehicles step
     * in UPDATE on their own fixed clock, ahead of the entity pipeline, so
     * every entity moved and every camera placed this frame meets a vehicle
     * where it is drawn. Each step a vehicle whose chunk is loaded steers and
     * trims through VehicleRigBranch, samples the sea and floods through
     * VehicleHullBranch, then over every sub-step gathers gravity, the sea's
     * lift and drag, its sails and rudder, the land and its anchor, and
     * VehicleMotionBranch integrates them. A vehicle is never pinned to the
     * world: at anchor it still floats and rocks, and its anchor only drags.
     * Riders are carried to where the deck now stands once every vehicle has
     * stepped, and every door swings toward open or shut. The helm, the
     * sails, the doors and the anchor change only through here, and an entity
     * leaving the world lets go of every vehicle through releaseEntity().
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private VehicleRiderSystem vehicleRiderSystem;
    private VehicleCargoSystem vehicleCargoSystem;

    // Branches
    private VehicleMotionBranch vehicleMotionBranch;
    private VehicleHullBranch vehicleHullBranch;
    private VehicleRigBranch vehicleRigBranch;
    private VehicleGroundBranch vehicleGroundBranch;
    private VehicleMooringBranch vehicleMooringBranch;
    private VehicleDoorBranch vehicleDoorBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, VehicleHandle> vehicleName2VehicleHandle;
    private ObjectArrayList<VehicleHandle> vehicleHandles;

    // Vehicles
    private ObjectArrayList<VehicleInstance> vehicles;

    // Clock
    private float timeStep;
    private float accumulatedTime;

    // Scratch
    private Vector3 offsetScratch;
    private Vector3 modelScratch;
    private Quaternion orientationScratch;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.vehicleName2VehicleHandle = new Object2ObjectOpenHashMap<>();
        this.vehicleHandles = new ObjectArrayList<>();
        create(VehicleLoader.class);

        // Branches
        this.vehicleMotionBranch = create(VehicleMotionBranch.class);
        this.vehicleHullBranch = create(VehicleHullBranch.class);
        this.vehicleRigBranch = create(VehicleRigBranch.class);
        this.vehicleGroundBranch = create(VehicleGroundBranch.class);
        this.vehicleMooringBranch = create(VehicleMooringBranch.class);
        this.vehicleDoorBranch = create(VehicleDoorBranch.class);

        // Systems
        this.vehicleRiderSystem = create(VehicleRiderSystem.class);
        create(VehicleCastSystem.class);
        create(VehicleControlSystem.class);
        this.vehicleCargoSystem = create(VehicleCargoSystem.class);
        create(VehicleRenderSystem.class);
        create(VehicleHullMaskSystem.class);

        // Vehicles
        this.vehicles = new ObjectArrayList<>();

        // Clock
        this.timeStep = EngineSetting.FIXED_TIME_STEP;

        // Scratch
        this.offsetScratch = new Vector3();
        this.modelScratch = new Vector3();
        this.orientationScratch = new Quaternion();
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Update \\

    @Override
    protected void update() {

        if (vehicles.isEmpty()) {
            accumulatedTime = 0f;
            return;
        }

        accumulatedTime += internal.getDeltaTime();
        int steps = 0;

        while (accumulatedTime >= timeStep && steps < EngineSetting.MAX_FIXED_STEPS_PER_FRAME) {

            for (int i = 0; i < vehicles.size(); i++)
                stepVehicle(vehicles.get(i));

            accumulatedTime -= timeStep;
            steps++;
        }

        if (steps == EngineSetting.MAX_FIXED_STEPS_PER_FRAME)
            accumulatedTime = Math.min(accumulatedTime, timeStep);

        vehicleRiderSystem.carryRiders();
    }

    private void stepVehicle(VehicleInstance vehicle) {

        if (worldStreamManager.getChunkInstance(vehicle.getWorldPositionStruct().getChunkCoordinate()) == null)
            return;

        vehicleDoorBranch.swing(vehicle, timeStep);
        vehicleRigBranch.trim(vehicle, timeStep);
        vehicleHullBranch.sampleSea(vehicle);
        vehicleHullBranch.flood(vehicle, timeStep);

        float subStep = timeStep / EngineSetting.VEHICLE_SUB_STEPS;

        for (int i = 0; i < EngineSetting.VEHICLE_SUB_STEPS; i++) {

            vehicle.clearForces();
            vehicleMotionBranch.applyGravity(vehicle);
            vehicleHullBranch.applyForces(vehicle);
            vehicleRigBranch.applyForces(vehicle);
            vehicleGroundBranch.applyForces(vehicle);
            vehicleMooringBranch.applyForces(vehicle);
            vehicleMotionBranch.integrate(vehicle, subStep);
        }

        WorldPositionUtility.settleChunk(vehicle.getWorldHandle(), vehicle.getWorldPositionStruct());
    }

    // Management \\

    void addVehicleHandle(VehicleHandle vehicleHandle) {

        if (vehicleName2VehicleHandle.containsKey(vehicleHandle.getVehicleName()))
            throwException("Vehicle '" + vehicleHandle.getVehicleName() + "' is registered twice.");

        vehicleName2VehicleHandle.put(vehicleHandle.getVehicleName(), vehicleHandle);
        vehicleHandles.add(vehicleHandle);
    }

    // Spawns a vehicle at anchor and furnished, level, its bow on the heading given and its centre of mass over the
    // position given, its design waterline on the water there — the one way a vehicle enters the world
    public VehicleInstance spawn(
            VehicleHandle vehicleHandle,
            WorldHandle worldHandle,
            long chunkCoordinate,
            Vector3 position,
            float heading) {

        VehicleInstance vehicle = create(VehicleInstance.class);
        vehicle.constructor(
                vehicleHandle,
                worldHandle,
                position,
                chunkCoordinate,
                vehicleMooringBranch.resolveLevelOrientation(heading, orientationScratch));

        vehicle.getWorldPositionStruct().getPosition().y = vehicleMooringBranch.resolveRestHeight(vehicle);
        vehicles.add(vehicle);
        vehicleMooringBranch.dropAnchor(vehicle);
        vehicleCargoSystem.stow(vehicle);

        return vehicle;
    }

    // The one way a vehicle leaves the world — its helmsman, riders and cargo go with it
    public void despawn(VehicleInstance vehicle) {

        if (!vehicles.remove(vehicle))
            return;

        vehicle.setHelmsman(null);
        vehicleRiderSystem.releaseVehicle(vehicle);
    }

    // Helm \\

    public void takeHelm(VehicleInstance vehicle, EntityInstance entity) {

        releaseHelm(entity);
        vehicle.setHelmsman(entity);
    }

    // Lets go of whatever helm an entity holds
    public void releaseHelm(EntityInstance entity) {

        for (int i = 0; i < vehicles.size(); i++)
            if (vehicles.get(i).getHelmsman() == entity)
                vehicles.get(i).setHelmsman(null);
    }

    // An entity leaving the world lets go of its helm and goes ashore
    public void releaseEntity(EntityInstance entity) {
        releaseHelm(entity);
        vehicleRiderSystem.releaseEntity(entity);
    }

    public VehicleInstance findHelmFor(EntityInstance entity) {

        for (int i = 0; i < vehicles.size(); i++)
            if (vehicles.get(i).getHelmsman() == entity)
                return vehicles.get(i);

        return null;
    }

    // Rig \\

    public void setSail(VehicleInstance vehicle, int sailIndex, boolean set) {
        vehicle.setSailSet(sailIndex, set);
    }

    // Every sail on a yard set or taken in together, as one hand at that yard would
    public void toggleYard(VehicleInstance vehicle, int yardPartIndex) {

        VehicleHandle vehicleHandle = vehicle.getVehicleHandle();
        boolean anySet = false;

        for (int sailIndex = 0; sailIndex < vehicleHandle.getSailCount(); sailIndex++)
            if (vehicleHandle.getSail(sailIndex).getYardIndex() == yardPartIndex && vehicle.isSailSet(sailIndex))
                anySet = true;

        for (int sailIndex = 0; sailIndex < vehicleHandle.getSailCount(); sailIndex++)
            if (vehicleHandle.getSail(sailIndex).getYardIndex() == yardPartIndex)
                setSail(vehicle, sailIndex, !anySet);
    }

    // Doors \\

    // Opens a door that is shut or shutting, or shuts one that is open unless cargo stands where it closes
    public void toggleDoor(VehicleInstance vehicle, int doorIndex) {

        boolean open = !vehicle.isDoorOpen(doorIndex);

        if (!open && !vehicleCargoSystem.isPartClear(vehicle, vehicle.getVehicleHandle().getDoor(doorIndex)))
            return;

        vehicle.setDoorOpen(doorIndex, open);
    }

    // Anchor \\

    public void toggleAnchor(VehicleInstance vehicle) {

        if (vehicle.isAnchored())
            vehicleMooringBranch.weighAnchor(vehicle);
        else
            vehicleMooringBranch.dropAnchor(vehicle);
    }

    // Query \\

    // The vehicle whose model grid holds a chunk-local point nearest its centre, within its bounding radius plus
    // the margin given — null when none reaches it
    public VehicleInstance findVehicleNear(
            WorldHandle worldHandle,
            long chunkCoordinate,
            float x,
            float y,
            float z,
            float margin) {

        VehicleInstance nearest = null;
        float nearestDistance = Float.MAX_VALUE;

        for (int i = 0; i < vehicles.size(); i++) {

            VehicleInstance vehicle = vehicles.get(i);

            if (vehicle.getWorldHandle() != worldHandle)
                continue;

            float reach = vehicle.getVehicleHandle().getBoundingRadius() + margin;
            float distance = VehicleSpaceUtility.toOffset(vehicle, chunkCoordinate, x, y, z, offsetScratch)
                    .lengthSquared();

            if (distance > reach * reach || distance >= nearestDistance)
                continue;

            nearest = vehicle;
            nearestDistance = distance;
        }

        return nearest;
    }

    // True when a chunk-local point lies in the dry inside of a hull not yet swamped
    public boolean isInsideHull(WorldHandle worldHandle, long chunkCoordinate, float x, float y, float z) {

        for (int i = 0; i < vehicles.size(); i++) {

            VehicleInstance vehicle = vehicles.get(i);

            if (vehicle.getWorldHandle() != worldHandle
                    || vehicle.getFlood() >= EngineSetting.VEHICLE_DRY_FLOOD_LIMIT)
                continue;

            float reach = vehicle.getVehicleHandle().getBoundingRadius();

            if (VehicleSpaceUtility.toOffset(vehicle, chunkCoordinate, x, y, z, offsetScratch)
                    .lengthSquared() > reach * reach)
                continue;

            VehicleSpaceUtility.offsetToModel(vehicle, offsetScratch, modelScratch);

            if (vehicle.getVehicleHandle().getHull().isDry(modelScratch.x, modelScratch.y, modelScratch.z))
                return true;
        }

        return false;
    }

    // Accessible \\

    public ObjectArrayList<VehicleInstance> getVehicles() {
        return vehicles;
    }

    public ObjectArrayList<VehicleHandle> getVehicleHandles() {
        return vehicleHandles;
    }

    public boolean hasVehicle(String vehicleName) {
        return vehicleName2VehicleHandle.containsKey(vehicleName);
    }

    public VehicleHandle getVehicleHandleFromVehicleName(String vehicleName) {

        VehicleHandle handle = vehicleName2VehicleHandle.get(vehicleName);

        if (handle == null) {
            ((VehicleLoader) internalLoader).request(vehicleName);
            handle = vehicleName2VehicleHandle.get(vehicleName);
        }

        if (handle == null)
            throwException("[VehicleManager] Vehicle could not be loaded: \"" + vehicleName + "\"");

        return handle;
    }

    // A vehicle type by its full name, or by a local or display name only one type carries — null when none matches
    public VehicleHandle findVehicleHandle(String query) {

        VehicleHandle match = null;

        for (int i = 0; i < vehicleHandles.size(); i++) {

            VehicleHandle vehicleHandle = vehicleHandles.get(i);
            String vehicleName = vehicleHandle.getVehicleName();
            String localName = vehicleName.substring(vehicleName.lastIndexOf(EngineSetting.PATH_SEPARATOR) + 1);

            if (vehicleName.equalsIgnoreCase(query))
                return vehicleHandle;

            if (!localName.equalsIgnoreCase(query) && !vehicleHandle.getDisplayName().equalsIgnoreCase(query))
                continue;

            if (match != null && match != vehicleHandle)
                return null;

            match = vehicleHandle;
        }

        return match;
    }
}
