package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.oceanpipeline.tidemanager.TideManager;
import application.bootstrap.oceanpipeline.wavemanager.WaveManager;
import application.bootstrap.vehiclepipeline.util.VehicleSpaceUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleColumnStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandlingStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHullStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehicleLumpStruct;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.util.LiquidColumnUtility;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class VehicleHullBranch extends BranchPackage {

    /*
     * The sea's hold on a hull. Once a step it samples the surface over every
     * column, the live sea over open water and the still surface of any other
     * liquid, and how the water itself moves there. Every sub-step each lump
     * of dry hull is then lifted by the sea it displaces in its column, the
     * share of its turned height under the surface times its dry volume,
     * through its centre, and dragged through the water by how fast that point
     * moves against it, lightly along the hull and hard across and up it.
     * Spray over the rail drains away, but a hull heeled far over, or swamped
     * with half its deck edge under, floods through every edge column that is
     * under. The water aboard fills the lowest lumps and weighs them down, so
     * it runs to the low side and heels the hull further, until it capsizes;
     * it is pumped out while no edge is under and the hull stands upright, and
     * a hull full of water sinks.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private WaveManager waveManager;
    private TideManager tideManager;

    // Scratch
    private Vector3 lowScratch;
    private Vector3 highScratch;
    private Vector3 pointScratch;
    private Vector3 motionScratch;

    // Flooding — every lump's height in world axes this sub-step, and the lumps from lowest to highest
    private float[] lumpHeights;
    private int[] lumpOrder;
    private IntComparator heightOrder;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.lowScratch = new Vector3();
        this.highScratch = new Vector3();
        this.pointScratch = new Vector3();
        this.motionScratch = new Vector3();

        // Flooding
        this.lumpHeights = new float[0];
        this.lumpOrder = new int[0];
        this.heightOrder = (first, second) -> Float.compare(lumpHeights[first], lumpHeights[second]);
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
        this.waveManager = get(WaveManager.class);
        this.tideManager = get(TideManager.class);
    }

    // Sea \\

    void sampleSea(VehicleInstance vehicle) {

        ObjectArrayList<VehicleColumnStruct> columns = vehicle.getVehicleHandle().getHull().getColumns();
        float[] surfaces = vehicle.getColumnSurfaces();
        float[] motion = vehicle.getColumnMotion();

        for (int i = 0; i < columns.size(); i++) {

            surfaces[i] = sampleColumn(vehicle, columns.get(i), motionScratch);
            motion[i * EngineSetting.AXIS_COUNT] = motionScratch.x;
            motion[i * EngineSetting.AXIS_COUNT + 1] = motionScratch.y;
            motion[i * EngineSetting.AXIS_COUNT + 2] = motionScratch.z;
        }
    }

    // The water surface over a column and how the water moves there, LIQUID_NO_SURFACE when it stands in none
    private float sampleColumn(VehicleInstance vehicle, VehicleColumnStruct column, Vector3 outMotion) {

        outMotion.set(0f, 0f, 0f);
        resolveEnds(vehicle, column);

        Vector3 position = vehicle.getWorldPositionStruct().getPosition();
        float worldX = position.x + (lowScratch.x + highScratch.x) * 0.5f;
        float worldZ = position.z + (lowScratch.z + highScratch.z) * 0.5f;
        float lowY = position.y + lowScratch.y;
        long chunkCoordinate = VehicleSpaceUtility.toChunkCoordinate(vehicle, worldX, worldZ);
        ChunkInstance chunk = worldStreamManager.getChunkInstance(chunkCoordinate);

        if (chunk == null)
            return EngineSetting.LIQUID_NO_SURFACE;

        float localX = VehicleSpaceUtility.toChunkLocal(worldX);
        float localZ = VehicleSpaceUtility.toChunkLocal(worldZ);
        int blockX = (int) Math.floor(localX);
        int blockZ = (int) Math.floor(localZ);
        float tide = tideManager.getSurfaceHeightBlocks();
        int waterY = LiquidColumnUtility.findWaterY(chunk, blockManager, blockX, (int) Math.floor(lowY), blockZ, tide);

        if (waterY == LiquidColumnUtility.NO_WATER)
            waterY = LiquidColumnUtility.findWaterY(
                    chunk, blockManager, blockX, (int) Math.floor(tide), blockZ, tide);

        if (waterY == LiquidColumnUtility.NO_WATER)
            return EngineSetting.LIQUID_NO_SURFACE;

        float surface = waveManager.sampleWaterSurface(chunk, chunkCoordinate, blockX, waterY, blockZ, localX, localZ);

        if (LiquidColumnUtility.isTidal(chunk, blockX, waterY, blockZ))
            waveManager.sampleSurfaceMotion(chunkCoordinate, localX, localZ, Math.max(0f, surface - lowY), outMotion);

        return surface;
    }

    // Lift & Drag \\

    void applyForces(VehicleInstance vehicle) {

        VehicleHullStruct hull = vehicle.getVehicleHandle().getHull();
        ObjectArrayList<VehicleLumpStruct> lumps = hull.getLumps();
        float[] surfaces = vehicle.getColumnSurfaces();
        float[] m = vehicle.getRotation().val;
        float gravity = EngineSetting.GRAVITY_FORCE * vehicle.getWorldHandle().getGravityMultiplier();
        float positionY = vehicle.getWorldPositionStruct().getPosition().y;
        float reach = (float) EngineSetting.VEHICLE_COLUMN_SUB_VOXELS / EngineSetting.SUB_VOXEL_RESOLUTION * 0.5f
                * (Math.abs(m[1]) + Math.abs(m[5]) + Math.abs(m[9]));

        ensureLumpScratch(lumps.size());

        for (int i = 0; i < lumps.size(); i++) {

            VehicleLumpStruct lump = lumps.get(i);
            float surface = surfaces[lump.getColumnIndex()];

            VehicleSpaceUtility.modelToOffset(vehicle, lump.getX(), lump.getY(), lump.getZ(), pointScratch);
            lumpHeights[i] = pointScratch.y;

            if (Float.isNaN(surface))
                continue;

            float submerged = Math.max(0f, Math.min(1f, (surface - (positionY + pointScratch.y - reach))
                    / (2f * reach)));

            if (submerged <= 0f)
                continue;

            float volume = lump.getVolume() * submerged;

            vehicle.applyForce(
                    0f, EngineSetting.VEHICLE_WATER_DENSITY * volume * gravity, 0f,
                    pointScratch.x, pointScratch.y, pointScratch.z);

            applyDrag(vehicle, lump.getColumnIndex(), volume);
        }

        applyFloodWater(vehicle, hull, gravity);
    }

    // The water aboard fills the lowest of the hull's lumps first and weighs them down, so it runs to whichever
    // side lies lowest and a hull heeled far enough to take it on heels further still
    private void applyFloodWater(VehicleInstance vehicle, VehicleHullStruct hull, float gravity) {

        float water = vehicle.getFlood() * hull.getDryVolume();

        if (water <= 0f)
            return;

        ObjectArrayList<VehicleLumpStruct> lumps = hull.getLumps();

        for (int i = 0; i < lumps.size(); i++)
            lumpOrder[i] = i;

        IntArrays.quickSort(lumpOrder, 0, lumps.size(), heightOrder);

        for (int i = 0; i < lumps.size() && water > 0f; i++) {

            VehicleLumpStruct lump = lumps.get(lumpOrder[i]);
            float volume = Math.min(lump.getVolume(), water);

            water -= volume;
            VehicleSpaceUtility.modelToOffset(vehicle, lump.getX(), lump.getY(), lump.getZ(), pointScratch);
            vehicle.applyForce(
                    0f, -EngineSetting.VEHICLE_WATER_DENSITY * volume * gravity, 0f,
                    pointScratch.x, pointScratch.y, pointScratch.z);
        }
    }

    private void ensureLumpScratch(int lumpCount) {

        if (lumpHeights.length >= lumpCount)
            return;

        lumpHeights = new float[lumpCount];
        lumpOrder = new int[lumpCount];
    }

    // The water's drag on a submerged point, quadratic in how fast it moves against the water, per model axis
    private void applyDrag(VehicleInstance vehicle, int columnIndex, float volume) {

        VehicleHandlingStruct handling = vehicle.getVehicleHandle().getHandling();
        Vector3 velocity = vehicle.getVelocity();
        Vector3 spin = vehicle.getAngularVelocity();
        float[] motion = vehicle.getColumnMotion();
        int motionBase = columnIndex * EngineSetting.AXIS_COUNT;

        float relativeX = velocity.x + spin.y * pointScratch.z - spin.z * pointScratch.y - motion[motionBase];
        float relativeY = velocity.y + spin.z * pointScratch.x - spin.x * pointScratch.z - motion[motionBase + 1];
        float relativeZ = velocity.z + spin.x * pointScratch.y - spin.y * pointScratch.x - motion[motionBase + 2];

        VehicleSpaceUtility.toModelDirection(vehicle, relativeX, relativeY, relativeZ, motionScratch);

        float linear = EngineSetting.VEHICLE_DRAG_LINEAR_SPEED;
        float surge = -handling.getSurgeDrag() * volume * (Math.abs(motionScratch.x) + linear) * motionScratch.x;
        float heave = -handling.getHeaveDrag() * volume * (Math.abs(motionScratch.y) + linear) * motionScratch.y;
        float sway = -handling.getSwayDrag() * volume * (Math.abs(motionScratch.z) + linear) * motionScratch.z;

        VehicleSpaceUtility.toWorldDirection(vehicle, surge, heave, sway, motionScratch);
        vehicle.applyForce(
                motionScratch.x, motionScratch.y, motionScratch.z,
                pointScratch.x, pointScratch.y, pointScratch.z);
    }

    // Flooding \\

    void flood(VehicleInstance vehicle, float timeStep) {

        VehicleHullStruct hull = vehicle.getVehicleHandle().getHull();
        VehicleHandlingStruct handling = vehicle.getVehicleHandle().getHandling();
        ObjectArrayList<VehicleColumnStruct> columns = hull.getColumns();
        float[] surfaces = vehicle.getColumnSurfaces();
        float positionY = vehicle.getWorldPositionStruct().getPosition().y;
        int awash = 0;

        for (int i = 0; i < columns.size(); i++) {

            VehicleColumnStruct column = columns.get(i);

            if (!column.isEdge() || Float.isNaN(surfaces[i]))
                continue;

            VehicleSpaceUtility.modelToOffset(
                    vehicle, column.getX(), column.getTop(), column.getZ(), pointScratch);

            if (positionY + pointScratch.y < surfaces[i])
                awash++;
        }

        float flood = vehicle.getFlood();
        float uprightness = VehicleSpaceUtility.resolveUprightness(vehicle);
        float awashShare = (float) awash / Math.max(1, hull.getEdgeColumnCount());
        boolean swamped = awashShare >= EngineSetting.VEHICLE_SWAMP_SHARE;
        boolean heeled = uprightness < EngineSetting.VEHICLE_DOWNFLOOD_UPRIGHTNESS;

        if (awash > 0 && (swamped || heeled))
            flood += handling.getFloodRate() * awashShare * timeStep;
        else if (uprightness > EngineSetting.VEHICLE_PUMP_UPRIGHTNESS)
            flood -= handling.getPumpRate() * timeStep;

        vehicle.setFlood(Math.max(0f, Math.min(1f, flood)));
    }

    // Utility \\

    // A column's floor and top as offsets from the centre of mass, the lower of the two written first
    private void resolveEnds(VehicleInstance vehicle, VehicleColumnStruct column) {

        VehicleSpaceUtility.modelToOffset(vehicle, column.getX(), column.getBottom(), column.getZ(), lowScratch);
        VehicleSpaceUtility.modelToOffset(vehicle, column.getX(), column.getTop(), column.getZ(), highScratch);

        if (lowScratch.y <= highScratch.y)
            return;

        pointScratch.set(lowScratch);
        lowScratch.set(highScratch);
        highScratch.set(pointScratch);
    }
}
