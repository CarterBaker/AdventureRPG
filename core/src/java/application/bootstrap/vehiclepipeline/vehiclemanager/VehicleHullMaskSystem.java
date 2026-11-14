package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.vehiclepipeline.vehicle.VehicleHullStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VehicleHullMaskSystem extends SystemPackage {

    /*
     * Keeps the sea surface out of hulls. For each grid it writes one mask per
     * vehicle the grid streams, up to OCEAN_HULL_MAX_ENTRIES, into the vectors
     * WaveBufferSystem mirrors into that grid's OceanData: three rows that take
     * a position relative to the grid's reference chunk into the hull's mask
     * space, where x runs along the hull from its first mask station, y is the
     * model's height and z is the offset from the hull's centre line; then the
     * hull's length, floor and top; then its half-beam at every station. A
     * hull taking on water past VEHICLE_DRY_FLOOD_LIMIT lets the sea in.
     */

    // Internal
    private VehicleManager vehicleManager;

    // Base \\

    @Override
    protected void get() {
        this.vehicleManager = get(VehicleManager.class);
    }

    // Write \\

    // Writes every mask the grid needs, returning how many it wrote
    public int writeHullMasks(GridInstance grid, Vector4[] out) {

        ObjectArrayList<VehicleInstance> vehicles = vehicleManager.getVehicles();
        int count = 0;

        for (int i = 0; i < vehicles.size() && count < EngineSetting.OCEAN_HULL_MAX_ENTRIES; i++) {

            VehicleInstance vehicle = vehicles.get(i);

            if (vehicle.getWorldHandle() != grid.getWorldHandle()
                    || vehicle.getFlood() >= EngineSetting.VEHICLE_DRY_FLOOD_LIMIT
                    || grid.getGridSlotForChunk(vehicle.getWorldPositionStruct().getChunkCoordinate()) == null)
                continue;

            writeHullMask(grid, vehicle, out, count * EngineSetting.OCEAN_HULL_VECTORS_PER_ENTRY);
            count++;
        }

        return count;
    }

    // mask = R^T (position - centre of mass) + centre of mass - (first station, 0, centre line)
    private void writeHullMask(GridInstance grid, VehicleInstance vehicle, Vector4[] out, int base) {

        VehicleHullStruct hull = vehicle.getVehicleHandle().getHull();
        Vector3 centerOfMass = hull.getCenterOfMass();
        Vector3 position = vehicle.getWorldPositionStruct().getPosition();
        float[] m = vehicle.getRotation().val;
        float[] halfBeams = hull.getMaskHalfBeams();

        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                grid.getWorldHandle(),
                grid.getActiveChunkCoordinate(),
                vehicle.getWorldPositionStruct().getChunkCoordinate());
        float gridX = Coordinate2Long.unpackX(delta) * EngineSetting.CHUNK_SIZE + position.x;
        float gridY = position.y;
        float gridZ = Coordinate2Long.unpackY(delta) * EngineSetting.CHUNK_SIZE + position.z;

        out[base].set(m[0], m[1], m[2],
                centerOfMass.x - hull.getMaskMinX() - (m[0] * gridX + m[1] * gridY + m[2] * gridZ));
        out[base + 1].set(m[4], m[5], m[6],
                centerOfMass.y - (m[4] * gridX + m[5] * gridY + m[6] * gridZ));
        out[base + 2].set(m[8], m[9], m[10],
                centerOfMass.z - hull.getMaskCenterZ() - (m[8] * gridX + m[9] * gridY + m[10] * gridZ));
        out[base + 3].set(hull.getMaskLength(), hull.getMaskBottom(), hull.getMaskTop(), 0f);

        int stationBase = base + EngineSetting.OCEAN_HULL_HEADER_VECTORS;

        for (int station = 0; station < halfBeams.length; station++)
            setComponent(
                    out[stationBase + station / EngineSetting.VECTOR4_COMPONENT_COUNT],
                    station % EngineSetting.VECTOR4_COMPONENT_COUNT,
                    halfBeams[station]);
    }

    private void setComponent(Vector4 vector, int component, float value) {

        switch (component) {
            case 0 -> vector.x = value;
            case 1 -> vector.y = value;
            case 2 -> vector.z = value;
            default -> vector.w = value;
        }
    }
}
