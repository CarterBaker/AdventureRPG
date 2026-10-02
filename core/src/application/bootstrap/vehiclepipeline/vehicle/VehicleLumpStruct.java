package application.bootstrap.vehiclepipeline.vehicle;

import engine.root.StructPackage;

public class VehicleLumpStruct extends StructPackage {

    /*
     * One lump of a hull's dry volume, VEHICLE_LUMP_SUB_VOXELS on a side: the
     * centroid of its dry cells in model blocks, the volume of dry hull it
     * holds, and the column whose sampled sea it floats in. The sea lifts a
     * hull lump by lump, each by the share of it under the surface, so the
     * lift moves across the hull exactly as it heels, however far it goes.
     */

    // Position
    private final float x;
    private final float y;
    private final float z;

    // Volume
    private final float volume;

    // Sea
    private final int columnIndex;

    // Constructor \\

    public VehicleLumpStruct(float x, float y, float z, float volume, int columnIndex) {

        // Position
        this.x = x;
        this.y = y;
        this.z = z;

        // Volume
        this.volume = volume;

        // Sea
        this.columnIndex = columnIndex;
    }

    // Accessible \\

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getVolume() {
        return volume;
    }

    public int getColumnIndex() {
        return columnIndex;
    }
}
