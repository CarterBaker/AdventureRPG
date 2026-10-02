package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.physicspipeline.util.SubBlockSampleUtility;
import application.bootstrap.vehiclepipeline.util.VehicleSpaceUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHullStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

class VehicleGroundBranch extends BranchPackage {

    /*
     * Keeps a hull out of the land. Every contact point the hull carries is
     * tested against the world's solid sub-blocks each sub-step, and one
     * buried in the ground is pushed back out along the shortest way to open
     * air, up onto a shore or back off a bank, by a stiff spring that grows
     * with how deep it has sunk, damped while it presses in, and held by
     * friction along the ground. A hull runs aground, settles on a beach or
     * comes to rest on the sea bed instead of passing through.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;

    // Settings
    private float subBlockSize;

    // Scratch
    private Vector3 pointScratch;
    private Vector3 normalScratch;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.subBlockSize = SubBlockUtility.SIZE;

        // Scratch
        this.pointScratch = new Vector3();
        this.normalScratch = new Vector3();
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Contact \\

    void applyForces(VehicleInstance vehicle) {

        VehicleHullStruct hull = vehicle.getVehicleHandle().getHull();
        FloatArrayList points = hull.getContactPoints();
        int pointCount = hull.getContactPointCount();
        float pointMass = hull.getMass() / Math.max(1, pointCount);

        for (int i = 0; i < pointCount; i++) {

            int base = i * EngineSetting.AXIS_COUNT;

            VehicleSpaceUtility.modelToOffset(
                    vehicle,
                    points.getFloat(base),
                    points.getFloat(base + 1),
                    points.getFloat(base + 2),
                    pointScratch);

            float depth = resolvePenetration(vehicle, pointScratch);

            if (depth > 0f)
                applyContact(vehicle, depth, pointMass);
        }
    }

    // How deep a point lies in solid ground, writing the way out — zero when it stands in the open
    private float resolvePenetration(VehicleInstance vehicle, Vector3 offset) {

        long chunkCoordinate = vehicle.getWorldPositionStruct().getChunkCoordinate();
        Vector3 position = vehicle.getWorldPositionStruct().getPosition();
        float x = position.x + offset.x;
        float y = position.y + offset.y;
        float z = position.z + offset.z;
        int subX = SubBlockSampleUtility.toSub(x);
        int subY = SubBlockSampleUtility.toSub(y);
        int subZ = SubBlockSampleUtility.toSub(z);

        if (!isSolid(chunkCoordinate, subX, subY, subZ))
            return 0f;

        float best = Float.MAX_VALUE;
        normalScratch.set(0f, 1f, 0f);

        for (int step = 1; step <= EngineSetting.VEHICLE_GROUND_ESCAPE_STEPS && best == Float.MAX_VALUE; step++) {

            if (!isSolid(chunkCoordinate, subX, subY + step, subZ))
                best = escape((subY + step) * subBlockSize - y, best, 0f, 1f, 0f);

            if (!isSolid(chunkCoordinate, subX + step, subY, subZ))
                best = escape((subX + step) * subBlockSize - x, best, 1f, 0f, 0f);

            if (!isSolid(chunkCoordinate, subX - step, subY, subZ))
                best = escape(x - (subX - step + 1) * subBlockSize, best, -1f, 0f, 0f);

            if (!isSolid(chunkCoordinate, subX, subY, subZ + step))
                best = escape((subZ + step) * subBlockSize - z, best, 0f, 0f, 1f);

            if (!isSolid(chunkCoordinate, subX, subY, subZ - step))
                best = escape(z - (subZ - step + 1) * subBlockSize, best, 0f, 0f, -1f);
        }

        return best == Float.MAX_VALUE ? EngineSetting.VEHICLE_GROUND_ESCAPE_STEPS * subBlockSize : best;
    }

    private float escape(float depth, float best, float normalX, float normalY, float normalZ) {

        if (depth >= best)
            return best;

        normalScratch.set(normalX, normalY, normalZ);

        return depth;
    }

    // Spring and damping along the way out, friction across it, at the buried point
    private void applyContact(VehicleInstance vehicle, float depth, float pointMass) {

        Vector3 velocity = vehicle.getVelocity();
        Vector3 spin = vehicle.getAngularVelocity();

        float pointX = velocity.x + spin.y * pointScratch.z - spin.z * pointScratch.y;
        float pointY = velocity.y + spin.z * pointScratch.x - spin.x * pointScratch.z;
        float pointZ = velocity.z + spin.x * pointScratch.y - spin.y * pointScratch.x;
        float normalSpeed = pointX * normalScratch.x + pointY * normalScratch.y + pointZ * normalScratch.z;

        float press = pointMass * (EngineSetting.VEHICLE_GROUND_STIFFNESS * depth
                - EngineSetting.VEHICLE_GROUND_DAMPING * Math.min(normalSpeed, 0f));

        if (press <= 0f)
            return;

        float slideX = pointX - normalSpeed * normalScratch.x;
        float slideY = pointY - normalSpeed * normalScratch.y;
        float slideZ = pointZ - normalSpeed * normalScratch.z;
        float slide = (float) Math.sqrt(slideX * slideX + slideY * slideY + slideZ * slideZ);
        float grip = EngineSetting.VEHICLE_GROUND_FRICTION * press
                / Math.max(slide, EngineSetting.VEHICLE_GROUND_FRICTION_SPEED);

        vehicle.applyForce(
                normalScratch.x * press - slideX * grip,
                normalScratch.y * press - slideY * grip,
                normalScratch.z * press - slideZ * grip,
                pointScratch.x, pointScratch.y, pointScratch.z);
    }

    // Utility \\

    private boolean isSolid(long chunkCoordinate, int subX, int subY, int subZ) {
        return SubBlockSampleUtility.isSolid(worldStreamManager, blockManager, chunkCoordinate, subX, subY, subZ);
    }
}
