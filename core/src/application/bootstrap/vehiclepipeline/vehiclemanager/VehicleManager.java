package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import application.bootstrap.vehiclepipeline.util.VehicleSpaceUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleDesignStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHullStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
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
     * Every vehicle type is drawn at the drafting table, the game's own by
     * the editor and the player's in the game, and enters the palette through
     * registerVehicle(), at boot or while the game runs; a type drawn
     * again under the same name replaces the old one, which keeps sailing
     * where it already floats and frees its meshes once the last of it
     * leaves the world. spawnBeside() launches a vehicle beside an entity.
     * Every vehicle is heard through VehicleSoundBranch, and while the game
     * is paused no vehicle steps and every one falls silent. While held — a
     * recording plays back in their stead — none steps or is drawn, and
     * createPuppet() makes the drawn-only vehicles a recording poses instead.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private ClockManager clockManager;
    private VehicleRiderSystem vehicleRiderSystem;
    private VehicleCargoSystem vehicleCargoSystem;

    // Branches
    private VehicleAssemblyBranch vehicleAssemblyBranch;
    private VehicleMotionBranch vehicleMotionBranch;
    private VehicleHullBranch vehicleHullBranch;
    private VehicleRigBranch vehicleRigBranch;
    private VehicleGroundBranch vehicleGroundBranch;
    private VehicleMooringBranch vehicleMooringBranch;
    private VehicleDoorBranch vehicleDoorBranch;
    private VehicleSoundBranch vehicleSoundBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, VehicleHandle> vehicleName2VehicleHandle;
    private ObjectArrayList<VehicleHandle> vehicleHandles;

    // Retired — types replaced or removed while vehicles of them may still float
    private ObjectArrayList<VehicleHandle> retiredHandles;

    // Vehicles
    private ObjectArrayList<VehicleInstance> vehicles;
    private boolean held;

    // Clock
    private float timeStep;
    private float accumulatedTime;

    // Scratch
    private Vector3 offsetScratch;
    private Vector3 modelScratch;
    private Quaternion orientationScratch;
    private WorldPositionStruct spawnScratch;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.vehicleName2VehicleHandle = new Object2ObjectOpenHashMap<>();
        this.vehicleHandles = new ObjectArrayList<>();
        this.retiredHandles = new ObjectArrayList<>();

        // Branches
        create(VehicleGeometryBranch.class);
        create(VehicleHullFormBranch.class);
        this.vehicleAssemblyBranch = create(VehicleAssemblyBranch.class);
        this.vehicleMotionBranch = create(VehicleMotionBranch.class);
        this.vehicleHullBranch = create(VehicleHullBranch.class);
        this.vehicleRigBranch = create(VehicleRigBranch.class);
        this.vehicleGroundBranch = create(VehicleGroundBranch.class);
        this.vehicleMooringBranch = create(VehicleMooringBranch.class);
        this.vehicleDoorBranch = create(VehicleDoorBranch.class);
        this.vehicleSoundBranch = create(VehicleSoundBranch.class);

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
        this.spawnScratch = new WorldPositionStruct();
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
        this.clockManager = get(ClockManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        boolean moving = !held && !clockManager.isPaused();

        for (int i = 0; i < vehicles.size(); i++)
            vehicleSoundBranch.update(vehicles.get(i), moving);

        if (vehicles.isEmpty() || !moving) {
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

    // The one way a vehicle type enters the palette: assembled from the design drawn for it, replacing any type of
    // its name
    public VehicleHandle registerVehicle(VehicleDesignStruct design) {

        VehicleHandle vehicleHandle = vehicleAssemblyBranch.assemble(design);
        VehicleHandle replaced = vehicleName2VehicleHandle.put(vehicleHandle.getVehicleName(), vehicleHandle);

        if (replaced == null) {
            vehicleHandles.add(vehicleHandle);
            return vehicleHandle;
        }

        vehicleHandles.set(vehicleHandles.indexOf(replaced), vehicleHandle);
        retire(replaced);

        return vehicleHandle;
    }

    // Takes a type out of the palette; vehicles of it already afloat keep sailing until they leave the world
    public void removeVehicle(String vehicleName) {

        VehicleHandle removed = vehicleName2VehicleHandle.remove(vehicleName);

        if (removed == null)
            return;

        vehicleHandles.remove(removed);
        retire(removed);
    }

    private void retire(VehicleHandle vehicleHandle) {
        retiredHandles.add(vehicleHandle);
        releaseRetired();
    }

    // Frees every retired type no vehicle in the world is still made of
    private void releaseRetired() {

        for (int i = retiredHandles.size() - 1; i >= 0; i--) {

            VehicleHandle retired = retiredHandles.get(i);

            if (isAfloat(retired))
                continue;

            vehicleAssemblyBranch.release(retired);
            retiredHandles.remove(i);
        }
    }

    private boolean isAfloat(VehicleHandle vehicleHandle) {

        for (int i = 0; i < vehicles.size(); i++)
            if (vehicles.get(i).getVehicleHandle() == vehicleHandle)
                return true;

        return false;
    }

    // Survey \\

    // The hull a set of parts would float as at a draft, worked out without entering anything in the palette
    public VehicleHullStruct surveyHull(
            String vehicleName,
            ObjectArrayList<VehiclePartStruct> parts,
            float draft,
            float centerOfMassHeight) {
        return vehicleAssemblyBranch.surveyHull(vehicleName, parts, draft, centerOfMassHeight);
    }

    // The tonnes of sea a set of parts displaces floating level at a draft in model blocks
    public float resolveDisplacement(String vehicleName, ObjectArrayList<VehiclePartStruct> parts, float draft) {
        return vehicleAssemblyBranch.resolveDisplacement(vehicleName, parts, draft);
    }

    // The draft a set of parts floats at carrying a mass in tonnes, VEHICLE_DRAFT_SINKS when it cannot float it
    public float solveDraft(String vehicleName, ObjectArrayList<VehiclePartStruct> parts, float mass) {
        return vehicleAssemblyBranch.solveDraft(vehicleName, parts, mass);
    }

    // How far a hull heeled by an angle in radians is pushed back upright, in blocks; negative rolls it over
    public float resolveRightingArm(VehicleHullStruct hull, float heel) {
        return vehicleAssemblyBranch.resolveRightingArm(hull, heel);
    }

    // The triangles a set of parts draws once meshed, counted through the vehicle mesher without building a mesh
    public int countTriangles(ObjectArrayList<VehiclePartStruct> parts) {
        return vehicleAssemblyBranch.countTriangles(parts);
    }

    // The sub-voxels riders and cargo would collide with, laid exactly as an assembled vehicle lays them
    public SubVoxelGridStruct buildSolidGrid(ObjectArrayList<VehiclePartStruct> parts) {
        return vehicleAssemblyBranch.buildSolidGrid(parts);
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
        vehicleSoundBranch.attach(vehicle);
        vehicleMooringBranch.dropAnchor(vehicle);
        vehicleCargoSystem.stow(vehicle);

        return vehicle;
    }

    // Launches a vehicle broadside in front of an entity along the direction given, clear of it by its half-beam
    // and the spawn clearance, at anchor: its waterline on the water where there is water, its keel level with
    // the entity's feet where there is not
    public VehicleInstance spawnBeside(VehicleHandle vehicleHandle, EntityInstance entity, Vector3 direction) {

        float directionLength = (float) Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float forwardX = directionLength > 0f ? direction.x / directionLength : 0f;
        float forwardZ = directionLength > 0f ? direction.z / directionLength : 1f;

        Vector3 centerOfMass = vehicleHandle.getHull().getCenterOfMass();
        float halfBeam = (vehicleHandle.getMaxZ() - vehicleHandle.getMinZ()) * 0.5f
                / EngineSetting.SUB_VOXEL_RESOLUTION;
        float distance = halfBeam + EngineSetting.VEHICLE_SPAWN_CLEARANCE;
        Vector3 feet = entity.getWorldPositionStruct().getPosition();
        Vector3 size = entity.getSize();

        spawnScratch.setChunkCoordinate(entity.getWorldPositionStruct().getChunkCoordinate());
        spawnScratch.getPosition().set(
                feet.x + size.x * 0.5f + forwardX * distance,
                feet.y + centerOfMass.y,
                feet.z + size.z * 0.5f + forwardZ * distance);
        WorldPositionUtility.settleChunk(entity.getWorldHandle(), spawnScratch);

        return spawn(
                vehicleHandle,
                entity.getWorldHandle(),
                spawnScratch.getChunkCoordinate(),
                spawnScratch.getPosition(),
                (float) Math.atan2(forwardX, forwardZ) + (float) Math.PI * 0.5f);
    }

    // The one way a vehicle leaves the world — its helmsman, riders and cargo go with it
    public void despawn(VehicleInstance vehicle) {

        if (!vehicles.remove(vehicle))
            return;

        vehicleSoundBranch.detach(vehicle);
        vehicle.setHelmsman(null);
        vehicleRiderSystem.releaseVehicle(vehicle);
        releaseRetired();
    }

    // A vehicle drawn only, never part of the world, so nothing steps, carries or despawns it
    public VehicleInstance createPuppet(
            VehicleHandle vehicleHandle,
            WorldHandle worldHandle,
            long chunkCoordinate,
            Vector3 position,
            Quaternion orientation) {

        VehicleInstance puppet = create(VehicleInstance.class);
        puppet.constructor(vehicleHandle, worldHandle, position, chunkCoordinate, orientation);

        return puppet;
    }

    // Hold \\

    public void setHeld(boolean held) {
        this.held = held;
    }

    public boolean isHeld() {
        return held;
    }

    // Helm \\

    public void takeHelm(VehicleInstance vehicle, EntityInstance entity) {

        releaseHelm(entity);
        vehicle.setHelmsman(entity);
        vehicleSoundBranch.helm(vehicle);
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

        vehicleSoundBranch.hoist(vehicle, !anySet);
    }

    // Doors \\

    // Opens a door that is shut or shutting, or shuts one that is open unless cargo stands where it closes
    public void toggleDoor(VehicleInstance vehicle, int doorIndex) {

        boolean open = !vehicle.isDoorOpen(doorIndex);

        if (!open && !vehicleCargoSystem.isPartClear(vehicle, vehicle.getVehicleHandle().getDoor(doorIndex)))
            return;

        vehicle.setDoorOpen(doorIndex, open);
        vehicleSoundBranch.door(vehicle);
    }

    // Anchor \\

    public void toggleAnchor(VehicleInstance vehicle) {

        if (vehicle.isAnchored())
            vehicleMooringBranch.weighAnchor(vehicle);
        else
            vehicleMooringBranch.dropAnchor(vehicle);

        vehicleSoundBranch.anchor(vehicle);
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

        if (handle == null)
            throwException("[VehicleManager] No vehicle is registered as \"" + vehicleName + "\"");

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
