package application.bootstrap.vehiclepipeline.vehicle;

import engine.root.StructPackage;

public class VehicleColumnStruct extends StructPackage {

    /*
     * One upright column of a hull's dry volume, the unit the sea lifts it by.
     * Its centre in model blocks, the heights of its floor and its top, the
     * volume of dry hull it holds between them, and whether it lies on the
     * hull's edge, where a deck edge that goes under lets the sea in.
     */

    // Position
    private final float x;
    private final float z;

    // Extent
    private final float bottom;
    private final float top;
    private final float volume;

    // Flooding
    private final boolean edge;

    // Constructor \\

    public VehicleColumnStruct(float x, float z, float bottom, float top, float volume, boolean edge) {

        // Position
        this.x = x;
        this.z = z;

        // Extent
        this.bottom = bottom;
        this.top = top;
        this.volume = volume;

        // Flooding
        this.edge = edge;
    }

    // Accessible \\

    public float getX() {
        return x;
    }

    public float getZ() {
        return z;
    }

    public float getBottom() {
        return bottom;
    }

    public float getTop() {
        return top;
    }

    public float getVolume() {
        return volume;
    }

    public boolean isEdge() {
        return edge;
    }
}
