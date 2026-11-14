package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.oceanpipeline.tidemanager.TideManager;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHullStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.util.LiquidColumnUtility;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;

class VehicleMooringBranch extends BranchPackage {

    /*
     * Anchoring, and the pose a vehicle enters the world in. While the anchor
     * is down it drags every sub-step against the hull's drift and swing, so
     * a hull at anchor stays where it lies yet still floats, rolls and heaves
     * with the sea, and a hull under sail at anchor only creeps as it drags
     * it. A vehicle enters the world level, its bow on the heading given and
     * its design waterline on the still water under it, or where it was put
     * when there is no water there.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private TideManager tideManager;

    // Scratch
    private Vector3 offsetScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.offsetScratch = new Vector3();
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
        this.tideManager = get(TideManager.class);
    }

    // Anchor \\

    void dropAnchor(VehicleInstance vehicle) {
        vehicle.setAnchored(true);
    }

    void weighAnchor(VehicleInstance vehicle) {
        vehicle.setAnchored(false);
    }

    // The anchor drags against the hull's drift and swing, harder the faster it moves
    void applyForces(VehicleInstance vehicle) {

        if (!vehicle.isAnchored())
            return;

        VehicleHullStruct hull = vehicle.getVehicleHandle().getHull();
        float hold = vehicle.getVehicleHandle().getHandling().getAnchorHold();
        Vector3 velocity = vehicle.getVelocity();
        Vector3 spin = vehicle.getAngularVelocity();
        float drift = hold * hull.getMass();

        vehicle.applyForce(-velocity.x * drift, 0f, -velocity.z * drift, 0f, 0f, 0f);
        vehicle.getTorque().add(0f, -spin.y * hold * hull.getInertia().y, 0f);
    }

    // Pose \\

    // Level, bow on the heading given — a turn about the world's up axis taking the model's x onto the heading
    Quaternion resolveLevelOrientation(float heading, Quaternion out) {
        return out.setFromAxisAngle(0f, 1f, 0f, heading - (float) Math.PI * 0.5f);
    }

    // The centre-of-mass height that puts a level hull's design waterline on the still water under its centre,
    // or leaves it where it stands when there is no water there
    float resolveRestHeight(VehicleInstance vehicle) {

        VehicleHullStruct hull = vehicle.getVehicleHandle().getHull();
        float surface = sampleStillSurface(vehicle.getWorldPositionStruct());

        if (Float.isNaN(surface))
            return vehicle.getWorldPositionStruct().getPosition().y;

        vehicle.getOrientation().transform(offsetScratch.set(hull.getCenterOfMass()));

        return surface - hull.getDraft() + offsetScratch.y;
    }

    // The resting surface of the water under a point: the still tide over the sea, the fill level of any other
    private float sampleStillSurface(WorldPositionStruct point) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(point.getChunkCoordinate());

        if (chunk == null)
            return EngineSetting.LIQUID_NO_SURFACE;

        Vector3 position = point.getPosition();
        int blockX = Math.floorMod((int) Math.floor(position.x), EngineSetting.CHUNK_SIZE);
        int blockZ = Math.floorMod((int) Math.floor(position.z), EngineSetting.CHUNK_SIZE);
        float tide = tideManager.getSurfaceHeightBlocks();
        int waterY = LiquidColumnUtility.findWaterY(chunk, blockManager, blockX, (int) Math.floor(tide), blockZ, tide);

        if (waterY == LiquidColumnUtility.NO_WATER)
            return EngineSetting.LIQUID_NO_SURFACE;

        if (LiquidColumnUtility.isTidal(chunk, blockX, waterY, blockZ))
            return tide;

        return LiquidColumnUtility.findSurfaceHeight(chunk, blockManager, blockX, waterY, blockZ);
    }
}
