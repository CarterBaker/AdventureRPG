package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.vehiclepipeline.util.VehicleSpaceUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCargoInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehicleCastStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleGridStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartControl;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class VehicleCastSystem extends SystemPackage {

    /*
     * Casts a ray against every vehicle it passes near, each in its own model
     * grid. The ray walks the solid sub-voxels cell by cell from the one it
     * starts in, which is never a hit, and stops at the first filled one,
     * keeping the face it entered and the cell just outside it; it is tested
     * against the rough box of every cargo item the same way, and against the
     * bounds of every yard and sail where they stand now, so canvas can be
     * reached without being solid, unless the caller asks only for what is
     * solid. The nearest hit within reach wins.
     */

    // Internal
    private VehicleManager vehicleManager;

    // Settings
    private float resolution;

    // Scratch — model space
    private Vector3 offsetScratch;
    private Vector3 originScratch;
    private Vector3 directionScratch;
    private Vector3 partOriginScratch;
    private Vector3 partDirectionScratch;

    // Scratch — part poses
    private Matrix4 partMatrix;
    private Matrix4 turnMatrix;
    private Quaternion turnScratch;

    // Scratch — the walk, indexed by axis
    private float[] rayOrigin;
    private float[] rayDirection;
    private float[] boundsMin;
    private float[] boundsMax;
    private int[] cell;
    private int[] step;
    private float[] tMax;
    private float[] tDelta;

    // Box — the distance the last box test met its box at
    private float boxDistance;

    // Base \\

    @Override
    protected void create() {

        // Settings
        this.resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        // Scratch
        this.offsetScratch = new Vector3();
        this.originScratch = new Vector3();
        this.directionScratch = new Vector3();
        this.partOriginScratch = new Vector3();
        this.partDirectionScratch = new Vector3();
        this.partMatrix = new Matrix4();
        this.turnMatrix = new Matrix4();
        this.turnScratch = new Quaternion();
        this.rayOrigin = new float[EngineSetting.AXIS_COUNT];
        this.rayDirection = new float[EngineSetting.AXIS_COUNT];
        this.boundsMin = new float[EngineSetting.AXIS_COUNT];
        this.boundsMax = new float[EngineSetting.AXIS_COUNT];
        this.cell = new int[EngineSetting.AXIS_COUNT];
        this.step = new int[EngineSetting.AXIS_COUNT];
        this.tMax = new float[EngineSetting.AXIS_COUNT];
        this.tDelta = new float[EngineSetting.AXIS_COUNT];
    }

    @Override
    protected void get() {
        this.vehicleManager = get(VehicleManager.class);
    }

    // Cast \\

    void cast(
            WorldHandle worldHandle,
            long chunkCoordinate,
            Vector3 origin,
            Vector3 direction,
            float maxDistance,
            boolean includeCanvas,
            VehicleCastStruct out) {

        out.clear(maxDistance);

        ObjectArrayList<VehicleInstance> vehicles = vehicleManager.getVehicles();

        for (int i = 0; i < vehicles.size(); i++) {

            VehicleInstance vehicle = vehicles.get(i);

            if (vehicle.getWorldHandle() != worldHandle)
                continue;

            VehicleSpaceUtility.toOffset(vehicle, chunkCoordinate, origin.x, origin.y, origin.z, offsetScratch);

            if (!passesNear(offsetScratch, direction, maxDistance, vehicle.getVehicleHandle().getBoundingRadius()))
                continue;

            VehicleSpaceUtility.offsetToModel(vehicle, offsetScratch, originScratch);
            VehicleSpaceUtility.toModelDirection(vehicle, direction.x, direction.y, direction.z, directionScratch);

            castGrid(vehicle, out);
            castCargo(vehicle, out);

            if (includeCanvas)
                castCanvas(vehicle, out);
        }
    }

    // Whether the ray comes within the radius of the centre of mass anywhere along its reach
    private boolean passesNear(Vector3 offset, Vector3 direction, float maxDistance, float radius) {

        float along = Math.max(0f, Math.min(maxDistance,
                -(offset.x * direction.x + offset.y * direction.y + offset.z * direction.z)));
        float closestX = offset.x + direction.x * along;
        float closestY = offset.y + direction.y * along;
        float closestZ = offset.z + direction.z * along;

        return closestX * closestX + closestY * closestY + closestZ * closestZ <= radius * radius;
    }

    // Grid \\

    private void castGrid(VehicleInstance vehicle, VehicleCastStruct out) {

        VehicleGridStruct grid = vehicle.getVehicleHandle().getSolidGrid();
        beginWalk(originScratch, directionScratch);

        while (true) {

            int axis = tMax[EngineSetting.AXIS_X] < tMax[EngineSetting.AXIS_Y]
                    ? (tMax[EngineSetting.AXIS_X] < tMax[EngineSetting.AXIS_Z] ? EngineSetting.AXIS_X
                            : EngineSetting.AXIS_Z)
                    : (tMax[EngineSetting.AXIS_Y] < tMax[EngineSetting.AXIS_Z] ? EngineSetting.AXIS_Y
                            : EngineSetting.AXIS_Z);
            float t = tMax[axis];

            if (t >= out.getDistance())
                return;

            cell[axis] += step[axis];
            tMax[axis] += tDelta[axis];

            int part = grid.getPart(cell[EngineSetting.AXIS_X], cell[EngineSetting.AXIS_Y], cell[EngineSetting.AXIS_Z]);

            if (part == EngineSetting.INDEX_NOT_FOUND)
                continue;

            out.setPart(vehicle, t, part);
            writeFace(axis, out);
            return;
        }
    }

    private void beginWalk(Vector3 origin, Vector3 direction) {

        rayOrigin[EngineSetting.AXIS_X] = origin.x * resolution;
        rayOrigin[EngineSetting.AXIS_Y] = origin.y * resolution;
        rayOrigin[EngineSetting.AXIS_Z] = origin.z * resolution;
        rayDirection[EngineSetting.AXIS_X] = direction.x * resolution;
        rayDirection[EngineSetting.AXIS_Y] = direction.y * resolution;
        rayDirection[EngineSetting.AXIS_Z] = direction.z * resolution;

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {

            cell[axis] = (int) Math.floor(rayOrigin[axis]);
            step[axis] = rayDirection[axis] > 0f ? 1 : (rayDirection[axis] < 0f ? -1 : 0);
            tDelta[axis] = step[axis] != 0 ? Math.abs(1f / rayDirection[axis]) : Float.MAX_VALUE;

            if (step[axis] > 0)
                tMax[axis] = (cell[axis] + 1 - rayOrigin[axis]) / rayDirection[axis];
            else if (step[axis] < 0)
                tMax[axis] = (cell[axis] - rayOrigin[axis]) / rayDirection[axis];
            else
                tMax[axis] = Float.MAX_VALUE;
        }
    }

    // The face entered along the axis just stepped, and the cell outside it
    private void writeFace(int axis, VehicleCastStruct out) {

        int normal = -step[axis];
        Direction3Vector face = axis == EngineSetting.AXIS_X
                ? Direction3Vector.getDirectionX(normal)
                : axis == EngineSetting.AXIS_Y
                        ? Direction3Vector.getDirectionY(normal)
                        : Direction3Vector.getDirectionZ(normal);

        out.setFace(face, cell[EngineSetting.AXIS_X] + face.x, cell[EngineSetting.AXIS_Y] + face.y,
                cell[EngineSetting.AXIS_Z] + face.z);
    }

    // Cargo \\

    private void castCargo(VehicleInstance vehicle, VehicleCastStruct out) {

        ObjectArrayList<VehicleCargoInstance> cargo = vehicle.getCargo();

        for (int i = 0; i < cargo.size(); i++) {

            VehicleCargoInstance item = cargo.get(i);

            boundsMin[EngineSetting.AXIS_X] = item.getMinX() / resolution;
            boundsMin[EngineSetting.AXIS_Y] = item.getMinY() / resolution;
            boundsMin[EngineSetting.AXIS_Z] = item.getMinZ() / resolution;
            boundsMax[EngineSetting.AXIS_X] = item.getMaxX() / resolution;
            boundsMax[EngineSetting.AXIS_Y] = item.getMaxY() / resolution;
            boundsMax[EngineSetting.AXIS_Z] = item.getMaxZ() / resolution;

            int faceAxis = castBox(originScratch, directionScratch, out.getDistance());

            if (faceAxis == EngineSetting.INDEX_NOT_FOUND)
                continue;

            out.setCargo(vehicle, boxDistance, item);
            writeBoxFace(faceAxis, boxDistance, item, out);
        }
    }

    // The face of a cargo box the ray entered, and the sub-voxel outside it over the point it struck
    private void writeBoxFace(int faceAxis, float distance, VehicleCargoInstance item, VehicleCastStruct out) {

        float along = faceAxis == EngineSetting.AXIS_X ? directionScratch.x
                : faceAxis == EngineSetting.AXIS_Y ? directionScratch.y : directionScratch.z;
        int normal = along > 0f ? -1 : 1;
        Direction3Vector face = faceAxis == EngineSetting.AXIS_X
                ? Direction3Vector.getDirectionX(normal)
                : faceAxis == EngineSetting.AXIS_Y
                        ? Direction3Vector.getDirectionY(normal)
                        : Direction3Vector.getDirectionZ(normal);

        int hitX = clampCell((originScratch.x + directionScratch.x * distance) * resolution,
                item.getMinX(), item.getMaxX());
        int hitY = clampCell((originScratch.y + directionScratch.y * distance) * resolution,
                item.getMinY(), item.getMaxY());
        int hitZ = clampCell((originScratch.z + directionScratch.z * distance) * resolution,
                item.getMinZ(), item.getMaxZ());

        out.setFace(face, hitX + face.x, hitY + face.y, hitZ + face.z);
    }

    private int clampCell(float coordinate, int min, int max) {
        return Math.max(min, Math.min(max - 1, (int) Math.floor(coordinate)));
    }

    // Canvas \\

    // Every yard and sail tested where it stands now, its pose undone so its bounds stay a box
    private void castCanvas(VehicleInstance vehicle, VehicleCastStruct out) {

        VehicleHandle vehicleHandle = vehicle.getVehicleHandle();

        for (int partIndex = 0; partIndex < vehicleHandle.getPartCount(); partIndex++) {

            VehiclePartStruct part = vehicleHandle.getPart(partIndex);

            if (part.getRole().getControl() != VehiclePartControl.HOIST)
                continue;

            VehicleSpaceUtility.composePartMatrix(vehicle, part, turnScratch, turnMatrix, partMatrix).inverse();
            float[] m = partMatrix.val;

            partOriginScratch.set(
                    m[0] * originScratch.x + m[4] * originScratch.y + m[8] * originScratch.z + m[12],
                    m[1] * originScratch.x + m[5] * originScratch.y + m[9] * originScratch.z + m[13],
                    m[2] * originScratch.x + m[6] * originScratch.y + m[10] * originScratch.z + m[14]);
            partDirectionScratch.set(
                    m[0] * directionScratch.x + m[4] * directionScratch.y + m[8] * directionScratch.z,
                    m[1] * directionScratch.x + m[5] * directionScratch.y + m[9] * directionScratch.z,
                    m[2] * directionScratch.x + m[6] * directionScratch.y + m[10] * directionScratch.z);

            boundsMin[EngineSetting.AXIS_X] = part.getMinX() / resolution;
            boundsMin[EngineSetting.AXIS_Y] = part.getMinY() / resolution;
            boundsMin[EngineSetting.AXIS_Z] = part.getMinZ() / resolution;
            boundsMax[EngineSetting.AXIS_X] = part.getMaxX() / resolution;
            boundsMax[EngineSetting.AXIS_Y] = part.getMaxY() / resolution;
            boundsMax[EngineSetting.AXIS_Z] = part.getMaxZ() / resolution;

            if (castBox(partOriginScratch, partDirectionScratch, out.getDistance()) != EngineSetting.INDEX_NOT_FOUND)
                out.setPart(vehicle, boxDistance, partIndex);
        }
    }

    // Box \\

    // The slab test against the current bounds — the axis of the face entered, its distance kept in boxDistance,
    // or INDEX_NOT_FOUND when the ray misses, starts inside, or meets the box no nearer than the reach given
    private int castBox(Vector3 origin, Vector3 direction, float reach) {

        float near = 0f;
        float far = reach;
        int faceAxis = EngineSetting.INDEX_NOT_FOUND;

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++) {

            float start = axis == EngineSetting.AXIS_X ? origin.x : axis == EngineSetting.AXIS_Y ? origin.y : origin.z;
            float along = axis == EngineSetting.AXIS_X ? direction.x
                    : axis == EngineSetting.AXIS_Y ? direction.y : direction.z;

            if (along == 0f) {

                if (start < boundsMin[axis] || start >= boundsMax[axis])
                    return EngineSetting.INDEX_NOT_FOUND;

                continue;
            }

            float first = (boundsMin[axis] - start) / along;
            float second = (boundsMax[axis] - start) / along;
            float enter = Math.min(first, second);

            if (enter > near) {
                near = enter;
                faceAxis = axis;
            }

            far = Math.min(far, Math.max(first, second));
        }

        if (near > far || near >= reach || faceAxis == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        boxDistance = near;

        return faceAxis;
    }
}
