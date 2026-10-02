package application.bootstrap.combatpipeline.projectile;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.InstancePackage;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;

public class ProjectileInstance extends InstancePackage {

    /*
     * One thrown item in flight: the item, who threw it, where its centre is
     * and how it moves — velocity, orientation and the spin it tumbles with —
     * and how long it has flown. Once it comes to rest it remembers the height
     * of the ground under it until it can settle back into the world, or, on
     * a vehicle's deck, that vehicle and the point it rests on in the
     * vehicle's model blocks. Launched by ProjectileManager and owned by it
     * until it lands.
     */

    // Internal
    private EntityInstance thrower;
    private ItemInstance itemInstance;
    private WorldHandle worldHandle;

    // Position — the centre of the item's shape
    private WorldPositionStruct worldPositionStruct;

    // Motion
    private Vector3 velocity;
    private Vector3 spinAxis;
    private float spinRate;
    private Quaternion orientation;

    // Flight
    private float flightTime;
    private boolean struck;

    // Rest
    private boolean resting;
    private float groundY;
    private VehicleInstance restVehicle;
    private Vector3 restModelPoint;

    // Internal \\

    @Override
    protected void create() {

        // Position
        this.worldPositionStruct = new WorldPositionStruct();

        // Rest
        this.restModelPoint = new Vector3();

        // Motion
        this.velocity = new Vector3();
        this.spinAxis = new Vector3();
        this.orientation = new Quaternion();
    }

    // Constructor \\

    public void constructor(
            EntityInstance thrower,
            ItemInstance itemInstance,
            WorldHandle worldHandle,
            long chunkCoordinate,
            Vector3 position,
            Vector3 velocity,
            Vector3 spinAxis,
            float spinRate,
            Quaternion orientation) {

        // Internal
        this.thrower = thrower;
        this.itemInstance = itemInstance;
        this.worldHandle = worldHandle;

        // Position
        this.worldPositionStruct.setPosition(position);
        this.worldPositionStruct.setChunkCoordinate(chunkCoordinate);

        // Motion
        this.velocity.set(velocity);
        this.spinAxis.set(spinAxis);
        this.spinRate = spinRate;
        this.orientation.set(orientation);
    }

    // Flight \\

    public void advanceFlightTime(float deltaTime) {
        flightTime += deltaTime;
    }

    public void markStruck() {
        struck = true;
    }

    public void rest(float groundY) {

        this.resting = true;
        this.groundY = groundY;
        this.restVehicle = null;

        velocity.set(0f, 0f, 0f);
        spinRate = 0f;
    }

    // Rests on a vehicle's deck, at a point given in the vehicle's model blocks
    public void restOn(VehicleInstance vehicle, float modelX, float modelY, float modelZ) {

        rest(worldPositionStruct.getPosition().y);

        this.restVehicle = vehicle;
        this.restModelPoint.set(modelX, modelY, modelZ);
    }

    // Takes flight again from where it rests, its flight time begun anew, when it could not settle on a vehicle
    public void resume() {

        this.resting = false;
        this.restVehicle = null;
        this.flightTime = 0f;
    }

    // Accessible \\

    public EntityInstance getThrower() {
        return thrower;
    }

    public ItemInstance getItemInstance() {
        return itemInstance;
    }

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public WorldPositionStruct getWorldPositionStruct() {
        return worldPositionStruct;
    }

    public Vector3 getVelocity() {
        return velocity;
    }

    public Vector3 getSpinAxis() {
        return spinAxis;
    }

    public float getSpinRate() {
        return spinRate;
    }

    public void setSpinRate(float spinRate) {
        this.spinRate = spinRate;
    }

    public Quaternion getOrientation() {
        return orientation;
    }

    public float getFlightTime() {
        return flightTime;
    }

    public boolean hasStruck() {
        return struck;
    }

    public boolean isResting() {
        return resting;
    }

    public float getGroundY() {
        return groundY;
    }

    public boolean isRestingOnVehicle() {
        return restVehicle != null;
    }

    public VehicleInstance getRestVehicle() {
        return restVehicle;
    }

    public Vector3 getRestModelPoint() {
        return restModelPoint;
    }
}
