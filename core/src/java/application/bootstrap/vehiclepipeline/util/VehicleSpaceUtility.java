package application.bootstrap.vehiclepipeline.util;

import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartMotion;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleSailStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;

public class VehicleSpaceUtility extends EngineUtility {

    /*
     * The one place a vehicle's frame meets the world's. A world point is a
     * chunk and a position local to it, and its offset from a vehicle is
     * measured across the world's wrap from the vehicle's chunk to its centre
     * of mass, in world axes. Model space is the vehicle's model grid in
     * blocks: an offset turns into it through the inverse of the vehicle's
     * rotation and shifts by its centre of mass, and comes back the other way.
     * The rotation matrix stores its columns first, so a column is a model
     * axis seen in world axes. Moving parts are posed in model blocks here
     * too, so a part is drawn and struck exactly where it stands, and here a
     * door that is not shut stops being solid to whatever moves through it.
     */

    // Offset \\

    // A chunk-local point's offset from the vehicle's centre of mass, in world axes
    public static Vector3 toOffset(
            VehicleInstance vehicle,
            long chunkCoordinate,
            float x,
            float y,
            float z,
            Vector3 out) {

        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                vehicle.getWorldHandle(),
                vehicle.getWorldPositionStruct().getChunkCoordinate(),
                chunkCoordinate);
        Vector3 position = vehicle.getWorldPositionStruct().getPosition();

        return out.set(
                Coordinate2Long.unpackX(delta) * EngineSetting.CHUNK_SIZE + x - position.x,
                y - position.y,
                Coordinate2Long.unpackY(delta) * EngineSetting.CHUNK_SIZE + z - position.z);
    }

    // Chunk \\

    // The chunk a point given local to the vehicle's own chunk lies in, wrapped around the world
    public static long toChunkCoordinate(VehicleInstance vehicle, float x, float z) {

        long chunkCoordinate = Coordinate2Long.add(
                vehicle.getWorldPositionStruct().getChunkCoordinate(),
                Math.floorDiv((int) Math.floor(x), EngineSetting.CHUNK_SIZE),
                Math.floorDiv((int) Math.floor(z), EngineSetting.CHUNK_SIZE));

        return WorldWrapUtility.wrapAroundWorld(vehicle.getWorldHandle(), chunkCoordinate);
    }

    // One coordinate of that point, local to the chunk it lies in
    public static float toChunkLocal(float coordinate) {
        return coordinate - Math.floorDiv((int) Math.floor(coordinate), EngineSetting.CHUNK_SIZE)
                * EngineSetting.CHUNK_SIZE;
    }

    // Model \\

    // A chunk-local point in the vehicle's model blocks
    public static Vector3 toModel(
            VehicleInstance vehicle,
            long chunkCoordinate,
            float x,
            float y,
            float z,
            Vector3 out) {

        toOffset(vehicle, chunkCoordinate, x, y, z, out);

        return offsetToModel(vehicle, out, out);
    }

    public static Vector3 offsetToModel(VehicleInstance vehicle, Vector3 offset, Vector3 out) {

        Vector3 centerOfMass = vehicle.getVehicleHandle().getHull().getCenterOfMass();
        float[] m = vehicle.getRotation().val;
        float x = offset.x;
        float y = offset.y;
        float z = offset.z;

        return out.set(
                m[0] * x + m[1] * y + m[2] * z + centerOfMass.x,
                m[4] * x + m[5] * y + m[6] * z + centerOfMass.y,
                m[8] * x + m[9] * y + m[10] * z + centerOfMass.z);
    }

    // A world direction in the vehicle's model axes
    public static Vector3 toModelDirection(VehicleInstance vehicle, float x, float y, float z, Vector3 out) {

        float[] m = vehicle.getRotation().val;

        return out.set(
                m[0] * x + m[1] * y + m[2] * z,
                m[4] * x + m[5] * y + m[6] * z,
                m[8] * x + m[9] * y + m[10] * z);
    }

    // World \\

    // A point in model blocks as an offset from the centre of mass, in world axes
    public static Vector3 modelToOffset(VehicleInstance vehicle, float x, float y, float z, Vector3 out) {

        Vector3 centerOfMass = vehicle.getVehicleHandle().getHull().getCenterOfMass();

        return toWorldDirection(vehicle, x - centerOfMass.x, y - centerOfMass.y, z - centerOfMass.z, out);
    }

    // A direction in model axes turned into world axes
    public static Vector3 toWorldDirection(VehicleInstance vehicle, float x, float y, float z, Vector3 out) {

        float[] m = vehicle.getRotation().val;

        return out.set(
                m[0] * x + m[4] * y + m[8] * z,
                m[1] * x + m[5] * y + m[9] * z,
                m[2] * x + m[6] * y + m[10] * z);
    }

    // Heading \\

    // The yaw the vehicle's bow points along, measured the way a camera measures its own
    public static float resolveHeading(VehicleInstance vehicle) {

        float[] m = vehicle.getRotation().val;

        return (float) Math.atan2(m[0], m[2]);
    }

    // How far the vehicle has rolled or pitched off upright — 1 standing straight up, -1 turned over
    public static float resolveUprightness(VehicleInstance vehicle) {
        return vehicle.getRotation().val[5];
    }

    // Parts \\

    // Where a moving part stands in model blocks: a yard braced about its mast, a sail braced with it and gathered
    // up toward its head as it is taken in, a wheel spun about its axle, a rudder swung about its hinge, a door
    // swung about its hinge as far as it stands open and a portcullis lifted by as much of its height
    public static Matrix4 composePartMatrix(
            VehicleInstance vehicle,
            VehiclePartStruct part,
            Quaternion turnScratch,
            Matrix4 turnMatrix,
            Matrix4 out) {

        VehicleHandle vehicleHandle = vehicle.getVehicleHandle();

        switch (part.getRole().getMotion()) {

            case BRACE -> composeBrace(vehicle, part.getMastIndex(), turnScratch, turnMatrix, out);

            case FURL -> {

                VehicleSailStruct sail = vehicleHandle.getSail(part.getSailIndex());
                float hoist = vehicle.getSailHoist(part.getSailIndex());
                float gather = EngineSetting.VEHICLE_SAIL_FURLED_SCALE
                        + (1f - EngineSetting.VEHICLE_SAIL_FURLED_SCALE) * hoist;
                float head = sail.getHeadY();

                composeBrace(vehicle, part.getMastIndex(), turnScratch, turnMatrix, out).multiply(
                        1, 0, 0, 0,
                        0, gather, 0, head - head * gather,
                        0, 0, 1, 0,
                        0, 0, 0, 1);
            }

            case WHEEL -> composeTurn(
                    part.getPivot(),
                    part.getAxis(),
                    vehicle.getRudderAngle() * EngineSetting.VEHICLE_WHEEL_TURNS_PER_RUDDER,
                    turnScratch,
                    turnMatrix,
                    out);

            case RUDDER -> composeTurn(
                    part.getPivot(),
                    EngineSetting.AXIS_Y,
                    vehicle.getRudderAngle(),
                    turnScratch,
                    turnMatrix,
                    out);

            case SWING -> composeTurn(
                    part.getPivot(),
                    part.getAxis(),
                    part.getOpenAngle() * vehicle.getDoorOpening(part.getDoorIndex()),
                    turnScratch,
                    turnMatrix,
                    out);

            case LIFT -> out.set(
                    1, 0, 0, 0,
                    0, 1, 0, (part.getMaxY() - part.getMinY()) / (float) EngineSetting.SUB_VOXEL_RESOLUTION
                            * vehicle.getDoorOpening(part.getDoorIndex()),
                    0, 0, 1, 0,
                    0, 0, 0, 1);

            case DROP -> out.set(
                    1, 0, 0, 0,
                    0, 1, 0, vehicle.isAnchored() ? -vehicleHandle.getHandling().getAnchorDrop() : 0f,
                    0, 0, 1, 0,
                    0, 0, 0, 1);

            default -> out.set(1f);
        }

        return out;
    }

    private static Matrix4 composeBrace(
            VehicleInstance vehicle,
            int mastIndex,
            Quaternion turnScratch,
            Matrix4 turnMatrix,
            Matrix4 out) {

        return composeTurn(
                vehicle.getVehicleHandle().getMast(mastIndex).getPivot(),
                EngineSetting.AXIS_Y,
                vehicle.getBraceAngle(mastIndex),
                turnScratch,
                turnMatrix,
                out);
    }

    // T(pivot) * R(axis, angle) * T(-pivot)
    private static Matrix4 composeTurn(
            Vector3 pivot,
            int axis,
            float angle,
            Quaternion turnScratch,
            Matrix4 turnMatrix,
            Matrix4 out) {

        turnScratch.setFromAxisAngle(
                axis == EngineSetting.AXIS_X ? 1f : 0f,
                axis == EngineSetting.AXIS_Y ? 1f : 0f,
                axis == EngineSetting.AXIS_Z ? 1f : 0f,
                angle);

        return out.set(
                1, 0, 0, pivot.x,
                0, 1, 0, pivot.y,
                0, 0, 1, pivot.z,
                0, 0, 0, 1)
                .multiply(turnScratch.toMatrix(turnMatrix))
                .multiply(
                        1, 0, 0, -pivot.x,
                        0, 1, 0, -pivot.y,
                        0, 0, 1, -pivot.z,
                        0, 0, 0, 1);
    }

    // Whether a part is drawn now — an anchor cable only while the anchor is down
    public static boolean isPartShown(VehicleInstance vehicle, VehiclePartStruct part) {
        return isPartShownUnderWay(part) || vehicle.isAnchored();
    }

    // Whether a part is drawn on a vehicle under way, its anchor aboard — every part but an anchor cable
    public static boolean isPartShownUnderWay(VehiclePartStruct part) {
        return part.getRole().getMotion() != VehiclePartMotion.PAYOUT;
    }

    // Solid \\

    // Whether a model sub-voxel stops riders and cargo now — a door stops nothing once it starts to open
    public static boolean isSolid(VehicleInstance vehicle, int x, int y, int z) {

        int partIndex = vehicle.getVehicleHandle().getSolidGrid().getPart(x, y, z);

        return partIndex != EngineSetting.INDEX_NOT_FOUND
                && !isPartOpen(vehicle, vehicle.getVehicleHandle().getPart(partIndex));
    }

    // Whether a part is a door that is not shut
    public static boolean isPartOpen(VehicleInstance vehicle, VehiclePartStruct part) {
        return part.isDoor() && vehicle.getDoorOpening(part.getDoorIndex()) > 0f;
    }

    // Matrix \\

    // T(chunk offset + centre of mass) * R * T(-centre of mass) — false beyond the radius around the viewer's chunk
    public static boolean composeModelMatrix(
            VehicleInstance vehicle,
            long viewerChunkCoordinate,
            int chunkRadius,
            Matrix4 out) {

        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                vehicle.getWorldHandle(),
                viewerChunkCoordinate,
                vehicle.getWorldPositionStruct().getChunkCoordinate());
        int deltaX = Coordinate2Long.unpackX(delta);
        int deltaZ = Coordinate2Long.unpackY(delta);

        if (Math.abs(deltaX) > chunkRadius || Math.abs(deltaZ) > chunkRadius)
            return false;

        Vector3 position = vehicle.getWorldPositionStruct().getPosition();
        Vector3 centerOfMass = vehicle.getVehicleHandle().getHull().getCenterOfMass();

        out.set(
                1, 0, 0, deltaX * EngineSetting.CHUNK_SIZE + position.x,
                0, 1, 0, position.y,
                0, 0, 1, deltaZ * EngineSetting.CHUNK_SIZE + position.z,
                0, 0, 0, 1)
                .multiply(vehicle.getRotation())
                .multiply(
                        1, 0, 0, -centerOfMass.x,
                        0, 1, 0, -centerOfMass.y,
                        0, 0, 1, -centerOfMass.z,
                        0, 0, 0, 1);

        return true;
    }
}
