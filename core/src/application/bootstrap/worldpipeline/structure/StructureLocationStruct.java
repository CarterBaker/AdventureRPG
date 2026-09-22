package application.bootstrap.worldpipeline.structure;

import engine.root.StructPackage;

public class StructureLocationStruct extends StructPackage {

    /*
     * One hand-authored spawn location. x/z are world block coordinates
     * (authored directly, or converted from a world-map pixel at load time).
     * A location always spawns — it ignores frequency and biome — and it
     * outranks every procedural candidate it overlaps. y is only used when
     * hasY is set; otherwise the structure's elevation rule resolves it.
     * rotation is a quarter-turn count 0-3, or ROTATION_RANDOM to roll one.
     */

    public static final int ROTATION_RANDOM = -1;

    private final double x;
    private final double z;
    private final boolean hasY;
    private final int y;
    private final int rotation;

    public StructureLocationStruct(double x, double z, boolean hasY, int y, int rotation) {
        this.x = x;
        this.z = z;
        this.hasY = hasY;
        this.y = y;
        this.rotation = rotation;
    }

    public double getX() {
        return x;
    }

    public double getZ() {
        return z;
    }

    public boolean hasY() {
        return hasY;
    }

    public int getY() {
        return y;
    }

    public int getRotation() {
        return rotation;
    }
}
