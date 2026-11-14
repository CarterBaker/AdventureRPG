package application.bootstrap.vehiclepipeline.vehicle;

import java.util.Arrays;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VehicleInstance extends InstancePackage {

    /*
     * One vehicle in the world. Its pose is the world position of its centre
     * of mass and the turn from its model axes to the world's, the rotation
     * matrix of that turn kept current with it; it moves with a linear and an
     * angular velocity, and every step gathers the forces and torques on it
     * about its centre of mass before VehicleMotionBranch integrates them.
     * It carries the sea it last sampled under each hull column, who stands
     * at its helm and where its rudder lies, how far each mast's yards are
     * braced and each sail is set, how far each door stands open and whether
     * it is opening or shutting, whether it rides at anchor, how much water it
     * has taken on, and the cargo set down aboard. Spawned and owned by
     * VehicleManager.
     */

    // Internal
    private VehicleHandle vehicleHandle;
    private WorldHandle worldHandle;

    // Pose
    private WorldPositionStruct worldPositionStruct;
    private Quaternion orientation;
    private Matrix4 rotation;

    // Motion
    private Vector3 velocity;
    private Vector3 angularVelocity;

    // Forces — world axes, torque about the centre of mass
    private Vector3 force;
    private Vector3 torque;

    // Sea — the surface over each column, and the water's motion there, three floats each
    private float[] columnSurfaces;
    private float[] columnMotion;

    // Helm
    private EntityInstance helmsman;
    private float wheelInput;
    private float rudderAngle;

    // Rig
    private float[] braceAngles;
    private float[] sailHoists;
    private boolean[] sailsSet;

    // Doors
    private float[] doorOpenings;
    private boolean[] doorsOpen;

    // Mooring
    private boolean anchored;

    // Flooding
    private float flood;

    // Cargo
    private ObjectArrayList<VehicleCargoInstance> cargo;

    // Internal \\

    @Override
    protected void create() {

        // Pose
        this.worldPositionStruct = new WorldPositionStruct();
        this.orientation = new Quaternion(1f, 0f, 0f, 0f);
        this.rotation = new Matrix4();

        // Motion
        this.velocity = new Vector3();
        this.angularVelocity = new Vector3();

        // Forces
        this.force = new Vector3();
        this.torque = new Vector3();

        // Cargo
        this.cargo = new ObjectArrayList<>();
    }

    // Constructor \\

    public void constructor(
            VehicleHandle vehicleHandle,
            WorldHandle worldHandle,
            Vector3 position,
            long chunkCoordinate,
            Quaternion orientation) {

        // Internal
        this.vehicleHandle = vehicleHandle;
        this.worldHandle = worldHandle;

        // Pose
        this.worldPositionStruct.setPosition(position);
        this.worldPositionStruct.setChunkCoordinate(chunkCoordinate);
        this.orientation.set(orientation).normalize();
        refreshRotation();

        // Sea
        int columnCount = vehicleHandle.getHull().getColumns().size();
        this.columnSurfaces = new float[columnCount];
        this.columnMotion = new float[columnCount * EngineSetting.AXIS_COUNT];
        Arrays.fill(columnSurfaces, EngineSetting.LIQUID_NO_SURFACE);

        // Rig
        this.braceAngles = new float[vehicleHandle.getMastCount()];
        this.sailHoists = new float[vehicleHandle.getSailCount()];
        this.sailsSet = new boolean[vehicleHandle.getSailCount()];

        // Doors
        this.doorOpenings = new float[vehicleHandle.getDoorCount()];
        this.doorsOpen = new boolean[vehicleHandle.getDoorCount()];
    }

    // Pose \\

    // The rotation matrix always follows the orientation — the one place it is rebuilt
    public void refreshRotation() {
        orientation.toMatrix(rotation);
    }

    // Forces \\

    public void clearForces() {
        force.set(0f, 0f, 0f);
        torque.set(0f, 0f, 0f);
    }

    // A force in world axes acting at an offset in blocks from the centre of mass
    public void applyForce(
            float forceX,
            float forceY,
            float forceZ,
            float offsetX,
            float offsetY,
            float offsetZ) {

        force.add(forceX, forceY, forceZ);
        torque.add(
                offsetY * forceZ - offsetZ * forceY,
                offsetZ * forceX - offsetX * forceZ,
                offsetX * forceY - offsetY * forceX);
    }

    // Helm \\

    public void setHelmsman(EntityInstance helmsman) {
        this.helmsman = helmsman;
        this.wheelInput = 0f;
    }

    public void setWheelInput(float wheelInput) {
        this.wheelInput = wheelInput;
    }

    public void setRudderAngle(float rudderAngle) {
        this.rudderAngle = rudderAngle;
    }

    // Rig \\

    public void setBraceAngle(int mastIndex, float braceAngle) {
        braceAngles[mastIndex] = braceAngle;
    }

    public void setSailHoist(int sailIndex, float hoist) {
        sailHoists[sailIndex] = hoist;
    }

    public void setSailSet(int sailIndex, boolean set) {
        sailsSet[sailIndex] = set;
    }

    // Doors \\

    public void setDoorOpening(int doorIndex, float opening) {
        doorOpenings[doorIndex] = opening;
    }

    public void setDoorOpen(int doorIndex, boolean open) {
        doorsOpen[doorIndex] = open;
    }

    // Mooring \\

    public void setAnchored(boolean anchored) {
        this.anchored = anchored;
    }

    // Flooding \\

    public void setFlood(float flood) {
        this.flood = flood;
    }

    // Accessible \\

    public VehicleHandle getVehicleHandle() {
        return vehicleHandle;
    }

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public WorldPositionStruct getWorldPositionStruct() {
        return worldPositionStruct;
    }

    public Quaternion getOrientation() {
        return orientation;
    }

    public Matrix4 getRotation() {
        return rotation;
    }

    public Vector3 getVelocity() {
        return velocity;
    }

    public Vector3 getAngularVelocity() {
        return angularVelocity;
    }

    public Vector3 getForce() {
        return force;
    }

    public Vector3 getTorque() {
        return torque;
    }

    public float[] getColumnSurfaces() {
        return columnSurfaces;
    }

    public float[] getColumnMotion() {
        return columnMotion;
    }

    public EntityInstance getHelmsman() {
        return helmsman;
    }

    public boolean hasHelmsman() {
        return helmsman != null;
    }

    public float getWheelInput() {
        return wheelInput;
    }

    public float getRudderAngle() {
        return rudderAngle;
    }

    public float getBraceAngle(int mastIndex) {
        return braceAngles[mastIndex];
    }

    public float getSailHoist(int sailIndex) {
        return sailHoists[sailIndex];
    }

    public boolean isSailSet(int sailIndex) {
        return sailsSet[sailIndex];
    }

    // How far a door stands open, from 0 shut to 1 fully open
    public float getDoorOpening(int doorIndex) {
        return doorOpenings[doorIndex];
    }

    // Whether a door is opening or stands open, rather than shutting or shut
    public boolean isDoorOpen(int doorIndex) {
        return doorsOpen[doorIndex];
    }

    public boolean isAnchored() {
        return anchored;
    }

    public float getFlood() {
        return flood;
    }

    public ObjectArrayList<VehicleCargoInstance> getCargo() {
        return cargo;
    }
}
