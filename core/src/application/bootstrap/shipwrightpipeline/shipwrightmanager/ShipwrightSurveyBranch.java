package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipSurveyStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHullStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartRole;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ShipwrightSurveyBranch extends BranchPackage {

    /*
     * Measures a compiled ship the way the game will sail it. Its triangles
     * are counted through the vehicle mesher itself, so the count is what it
     * will draw. Its hull is worked out exactly as VehicleManager will float
     * it, and its speed is where the reference wind's push on every sail set
     * square, as VehicleRigBranch pushes a sail, comes level with the water's
     * drag along the length on everything under the waterline, as
     * VehicleHullBranch drags it — so a finer bow below the waterline is a
     * faster ship here and at sea alike.
     */

    // Internal
    private VehicleManager vehicleManager;

    // Base \\

    @Override
    protected void get() {
        this.vehicleManager = get(VehicleManager.class);
    }

    // Survey \\

    void survey(ShipBlueprintStruct blueprint, ShipLayoutStruct layout, ShipSurveyStruct survey) {

        ObjectArrayList<VehiclePartStruct> parts = layout.getParts();
        int triangles = parts.isEmpty() ? 0 : vehicleManager.countTriangles(parts);

        if (layout.hasProblem() || layout.getDraft() <= 0f) {
            survey.set(triangles, 0f, 0f, 0f, 0f, 0f, layout.getSailCount(), 0f, 0f);
            return;
        }

        VehicleHullStruct hull = vehicleManager.surveyHull(
                blueprint.getDisplayName(), parts, layout.getDraft(), layout.getCenterOfMassHeight());
        float displaced = hull.getMass() / EngineSetting.VEHICLE_WATER_DENSITY;

        survey.set(
                triangles,
                resolveSpeed(resolveSailArea(parts), hull.getSurgeDrag() * displaced),
                hull.getMass(),
                resolveLength(blueprint),
                resolveBeam(blueprint),
                resolveDraft(blueprint),
                layout.getSailCount(),
                (float) Math.toDegrees(hull.getEntryAngle()),
                hull.getEntranceCoefficient());
    }

    // Speed \\

    // Every sail's canvas in square blocks
    private float resolveSailArea(ObjectArrayList<VehiclePartStruct> parts) {

        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;
        float area = 0f;

        for (int i = 0; i < parts.size(); i++) {

            VehiclePartStruct part = parts.get(i);

            if (part.getRole() == VehiclePartRole.SAIL)
                area += (part.getMaxY() - part.getMinY()) * scale * (part.getMaxZ() - part.getMinZ()) * scale;
        }

        return area;
    }

    // The speed at which the wind's push on the canvas, by the square of the wind it still feels, equals the
    // water's drag on the hull, quadratic with a linear tail at low speed
    private float resolveSpeed(float sailArea, float drag) {

        float wind = EngineSetting.SHIPWRIGHT_REFERENCE_WIND;
        float push = 0.5f * EngineSetting.VEHICLE_AIR_DENSITY * sailArea * EngineSetting.DEFAULT_VEHICLE_SAIL_FORCE;
        float linear = EngineSetting.VEHICLE_DRAG_LINEAR_SPEED;

        if (push <= 0f)
            return 0f;

        if (drag <= 0f)
            return wind;

        float a = push - drag;
        float b = -(2f * push * wind + drag * linear);
        float c = push * wind * wind;

        if (Math.abs(a) < EngineSetting.DIVISION_EPSILON)
            return Math.max(0f, Math.min(wind, -c / b));

        float root = (float) Math.sqrt(Math.max(0f, b * b - 4f * a * c));
        float low = (-b - root) / (2f * a);
        float high = (-b + root) / (2f * a);

        return Math.max(0f, Math.min(wind, low >= 0f && low <= wind ? low : high));
    }

    // Dimensions \\

    private float resolveLength(ShipBlueprintStruct blueprint) {

        int first = Integer.MAX_VALUE;
        int last = Integer.MIN_VALUE;

        for (int x = 0; x < blueprint.getLength(); x++)
            for (int y = 0; y < blueprint.getHeight(); y++)
                for (int halfZ = 0; halfZ < blueprint.getHalfBreadth(); halfZ++)
                    if (blueprint.isHull(x, y, halfZ)) {
                        first = Math.min(first, x);
                        last = Math.max(last, x);
                    }

        return first > last ? 0f : (last - first + 1) * EngineSetting.SUB_BLOCK_SIZE;
    }

    private float resolveBeam(ShipBlueprintStruct blueprint) {

        int reach = 0;

        for (int y = 0; y < blueprint.getHeight(); y++)
            for (int halfZ = reach; halfZ < blueprint.getHalfBreadth(); halfZ++)
                for (int x = 0; x < blueprint.getLength(); x++)
                    if (blueprint.isHull(x, y, halfZ)) {
                        reach = halfZ + 1;
                        break;
                    }

        return reach * 2 * EngineSetting.SUB_BLOCK_SIZE;
    }

    // From the bottom of the keel to the top of the waterline layer
    private float resolveDraft(ShipBlueprintStruct blueprint) {

        for (int y = 0; y < blueprint.getHeight(); y++)
            for (int halfZ = 0; halfZ < blueprint.getHalfBreadth(); halfZ++)
                for (int x = 0; x < blueprint.getLength(); x++)
                    if (blueprint.isHull(x, y, halfZ))
                        return Math.max(0, blueprint.getWaterline() + 1 - y) * EngineSetting.SUB_BLOCK_SIZE;

        return 0f;
    }
}
