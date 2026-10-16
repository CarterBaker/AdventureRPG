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
     * it. Its speed is where a wind's push on every sail set square, as
     * VehicleRigBranch pushes a sail, comes level with the water's drag along
     * the length on everything under the waterline, as VehicleHullBranch
     * drags it — so a finer bow below the waterline is a faster ship here and
     * at sea alike — worked out for a fair breeze and for a gale. Its reserve
     * is how much more sea its hull displaces sunk to its sill than it weighs.
     * Its stability is how far it can be heeled, a few degrees at a time, and
     * still be pushed back upright by the very lumps the sea lifts it by, so a
     * ship too tall or too narrow for what it carries capsizes here as surely
     * as it will once launched.
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
        float length = resolveLength(blueprint);
        float beam = resolveBeam(blueprint);

        if (!layout.isAfloat()) {
            survey.set(triangles, 0f, 0f, layout.getMass(), layout.getBallastMass(), length, beam, 0f, 0f, 0f, 0f,
                    layout.getSailCount(), 0f);
            return;
        }

        String name = blueprint.getDisplayName();
        float mass = layout.getMass();
        VehicleHullStruct hull = vehicleManager.surveyHull(name, parts, layout.getDraft(),
                layout.getCenterOfMassHeight());
        float sailArea = resolveSailArea(parts);
        float drag = hull.getSurgeDrag() * mass / EngineSetting.VEHICLE_WATER_DENSITY;
        float capacity = vehicleManager.resolveDisplacement(name, parts, layout.getSillHeight());

        survey.set(
                triangles,
                resolveSpeed(sailArea, drag, EngineSetting.SHIPWRIGHT_BREEZE_WIND),
                resolveSpeed(sailArea, drag, EngineSetting.SHIPWRIGHT_GALE_WIND),
                mass,
                layout.getBallastMass(),
                length,
                beam,
                layout.getDraft(),
                layout.getSillHeight() - layout.getDraft(),
                Math.max(0f, capacity - mass) / Math.max(mass, EngineSetting.DIVISION_EPSILON),
                resolveStableDegrees(hull),
                layout.getSailCount(),
                (float) Math.toDegrees(hull.getEntryAngle()));
    }

    // Stability \\

    // The last heel, a step at a time, the hull is still pushed back upright from, 0 when it rolls over at once
    private float resolveStableDegrees(VehicleHullStruct hull) {

        float step = EngineSetting.SHIPWRIGHT_STABILITY_STEP_DEGREES;
        float stable = 0f;

        for (float heel = step; heel <= EngineSetting.SHIPWRIGHT_STABILITY_MAX_DEGREES; heel += step) {

            if (vehicleManager.resolveRightingArm(hull, (float) Math.toRadians(heel)) <= 0f)
                return stable;

            stable = heel;
        }

        return stable;
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

    // The speed at which a wind's push on the canvas, by the square of the wind it still feels, equals the water's
    // drag on the hull, quadratic with a linear tail at low speed
    private float resolveSpeed(float sailArea, float drag, float wind) {

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

        int[] bounds = new int[2];

        if (!blueprint.resolveExtent(bounds))
            return 0f;

        return (bounds[1] - bounds[0] + 1) * EngineSetting.SUB_BLOCK_SIZE;
    }

    private float resolveBeam(ShipBlueprintStruct blueprint) {

        int widest = 0;

        for (int deck = 0; deck < blueprint.getDeckCount(); deck++)
            for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++)
                widest = Math.max(widest, blueprint.getOutline(deck, x));

        return widest * 2 * EngineSetting.SUB_BLOCK_SIZE;
    }
}
