package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.vehiclepipeline.util.VehicleSpaceUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandlingStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleSailStruct;
import application.bootstrap.weatherpipeline.wind.WindHandle;
import application.bootstrap.weatherpipeline.wind.WindInstance;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class VehicleRigBranch extends BranchPackage {

    /*
     * A vehicle's sails and rudder. Once a step the wheel swings the rudder
     * while the helm is held over and leaves it where it lies when let go,
     * every sail is set or taken in at its hoist rate, and every mast's yards
     * brace toward the angle that draws the most drive from the wind across
     * the deck, within their limit. Every sub-step each sail is pushed along
     * its face by the wind it meets, the wind over the grid less the sail's
     * own motion, at the pressure that wind has across the face, scaled by how
     * far the sail is set, through its centre high above the hull, so a press
     * of canvas heels the hull over and canvas taken aback drives it astern;
     * and the rudder under the stern pushes across the hull by the water
     * streaming past it, which only bites while the hull makes way.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private WindManager windManager;

    // Scratch
    private Vector3 windScratch;
    private Vector3 pointScratch;
    private Vector3 normalScratch;
    private Vector3 bodyScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.windScratch = new Vector3();
        this.pointScratch = new Vector3();
        this.normalScratch = new Vector3();
        this.bodyScratch = new Vector3();
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
        this.windManager = get(WindManager.class);
    }

    // Trim \\

    void trim(VehicleInstance vehicle, float timeStep) {

        VehicleHandlingStruct handling = vehicle.getVehicleHandle().getHandling();

        vehicle.setRudderAngle(clamp(
                vehicle.getRudderAngle() + vehicle.getWheelInput() * handling.getRudderRate() * timeStep,
                -handling.getRudderLimit(),
                handling.getRudderLimit()));

        hoistSails(vehicle, handling.getHoistRate() * timeStep);
        braceYards(vehicle, handling, timeStep);
    }

    private void hoistSails(VehicleInstance vehicle, float hoistStep) {

        for (int sailIndex = 0; sailIndex < vehicle.getVehicleHandle().getSailCount(); sailIndex++) {

            float target = vehicle.isSailSet(sailIndex) ? 1f : 0f;
            float hoist = vehicle.getSailHoist(sailIndex);

            vehicle.setSailHoist(sailIndex, hoist + clamp(target - hoist, -hoistStep, hoistStep));
        }
    }

    // Each mast's yards swing toward the brace that turns the most of the wind across the deck into drive
    private void braceYards(VehicleInstance vehicle, VehicleHandlingStruct handling, float timeStep) {

        resolveWind(vehicle, windScratch);

        Vector3 velocity = vehicle.getVelocity();
        VehicleSpaceUtility.toModelDirection(
                vehicle,
                windScratch.x - velocity.x,
                0f,
                windScratch.z - velocity.z,
                bodyScratch);

        float target = resolveBestBrace(bodyScratch.x, bodyScratch.z, handling.getBraceLimit());
        float braceStep = handling.getBraceRate() * timeStep;

        for (int mastIndex = 0; mastIndex < vehicle.getVehicleHandle().getMastCount(); mastIndex++) {

            float brace = vehicle.getBraceAngle(mastIndex);

            vehicle.setBraceAngle(mastIndex, brace + clamp(target - brace, -braceStep, braceStep));
        }
    }

    // The brace whose face draws the most forward drive from a wind given in model axes
    private float resolveBestBrace(float windX, float windZ, float braceLimit) {

        int samples = EngineSetting.VEHICLE_BRACE_SAMPLES;
        float best = 0f;
        float bestDrive = -Float.MAX_VALUE;

        for (int sample = 0; sample < samples; sample++) {

            float brace = -braceLimit + 2f * braceLimit * sample / (samples - 1);
            float normalX = (float) Math.cos(brace);
            float normalZ = (float) -Math.sin(brace);
            float across = windX * normalX + windZ * normalZ;
            float drive = across * Math.abs(across) * normalX;

            if (drive <= bestDrive)
                continue;

            best = brace;
            bestDrive = drive;
        }

        return best;
    }

    // Forces \\

    void applyForces(VehicleInstance vehicle) {

        resolveWind(vehicle, windScratch);

        VehicleHandle vehicleHandle = vehicle.getVehicleHandle();

        for (int sailIndex = 0; sailIndex < vehicleHandle.getSailCount(); sailIndex++)
            applySail(vehicle, vehicleHandle.getSail(sailIndex), vehicle.getSailHoist(sailIndex));

        if (vehicleHandle.hasRudder())
            applyRudder(vehicle, vehicleHandle.getRudder());
    }

    private void applySail(VehicleInstance vehicle, VehicleSailStruct sail, float hoist) {

        if (hoist <= 0f)
            return;

        Vector3 center = sail.getCenter();
        Vector3 velocity = vehicle.getVelocity();
        Vector3 spin = vehicle.getAngularVelocity();
        float brace = vehicle.getBraceAngle(sail.getMastIndex());

        VehicleSpaceUtility.modelToOffset(vehicle, center.x, center.y, center.z, pointScratch);
        VehicleSpaceUtility.toWorldDirection(vehicle, (float) Math.cos(brace), 0f, (float) -Math.sin(brace),
                normalScratch);

        float apparentX = windScratch.x - (velocity.x + spin.y * pointScratch.z - spin.z * pointScratch.y);
        float apparentY = windScratch.y - (velocity.y + spin.z * pointScratch.x - spin.x * pointScratch.z);
        float apparentZ = windScratch.z - (velocity.z + spin.x * pointScratch.y - spin.y * pointScratch.x);
        float across = apparentX * normalScratch.x + apparentY * normalScratch.y + apparentZ * normalScratch.z;
        float push = 0.5f * EngineSetting.VEHICLE_AIR_DENSITY * across * Math.abs(across) * sail.getArea() * hoist
                * vehicle.getVehicleHandle().getHandling().getSailForce();

        vehicle.applyForce(
                normalScratch.x * push, normalScratch.y * push, normalScratch.z * push,
                pointScratch.x, pointScratch.y, pointScratch.z);
    }

    // Lift across the blade from the water streaming past it, by the speed the hull makes along its length
    private void applyRudder(VehicleInstance vehicle, VehiclePartStruct rudder) {

        if (VehicleSpaceUtility.resolveUprightness(vehicle) <= 0f)
            return;

        Vector3 velocity = vehicle.getVelocity();
        Vector3 pivot = rudder.getPivot();
        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;
        float area = (rudder.getMaxX() - rudder.getMinX()) * (rudder.getMaxY() - rudder.getMinY()) * scale * scale;

        VehicleSpaceUtility.toModelDirection(vehicle, velocity.x, velocity.y, velocity.z, bodyScratch);

        float flow = bodyScratch.x;
        float lift = -0.5f * EngineSetting.VEHICLE_WATER_DENSITY * area * flow * Math.abs(flow)
                * (float) Math.sin(vehicle.getRudderAngle())
                * vehicle.getVehicleHandle().getHandling().getRudderForce();

        VehicleSpaceUtility.modelToOffset(vehicle, pivot.x, pivot.y, pivot.z, pointScratch);
        VehicleSpaceUtility.toWorldDirection(vehicle, 0f, 0f, lift, normalScratch);
        vehicle.applyForce(
                normalScratch.x, normalScratch.y, normalScratch.z,
                pointScratch.x, pointScratch.y, pointScratch.z);
    }

    // Wind \\

    // The wind over the grid streaming the vehicle, in blocks per second, or the prevailing wind where none does
    private void resolveWind(VehicleInstance vehicle, Vector3 out) {

        GridInstance grid = worldStreamManager.getGridForChunk(vehicle.getWorldPositionStruct().getChunkCoordinate());

        if (grid != null) {

            WindInstance wind = grid.getWindInstance();
            Vector3 direction = wind.getLocalWindDirection();

            out.set(direction).multiply(wind.getLocalWindSpeed());
            return;
        }

        WindHandle wind = windManager.getWindHandle();

        out.set(wind.getGlobalWindDirection()).multiply(wind.getGlobalWindSpeed());
    }

    // Utility \\

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
