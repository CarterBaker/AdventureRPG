package application.bootstrap.vehiclepipeline.vehicle;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class VehicleSailStruct extends StructPackage {

    /*
     * One sail of a vehicle as its rig sees it: the part it is drawn and
     * clicked as, the yard it hangs from and the mast that yard braces about,
     * the area of canvas it spreads when fully set, the centre that canvas
     * pulls through, and the height of its head, which it gathers up toward
     * as it is taken in. Positions are model blocks.
     */

    // Parts
    private final int partIndex;
    private final int yardIndex;
    private final int mastIndex;

    // Canvas
    private final float area;
    private final Vector3 center;
    private final float headY;

    // Constructor \\

    public VehicleSailStruct(
            int partIndex,
            int yardIndex,
            int mastIndex,
            float area,
            Vector3 center,
            float headY) {

        // Parts
        this.partIndex = partIndex;
        this.yardIndex = yardIndex;
        this.mastIndex = mastIndex;

        // Canvas
        this.area = area;
        this.center = center;
        this.headY = headY;
    }

    // Accessible \\

    public int getPartIndex() {
        return partIndex;
    }

    public int getYardIndex() {
        return yardIndex;
    }

    public int getMastIndex() {
        return mastIndex;
    }

    public float getArea() {
        return area;
    }

    public Vector3 getCenter() {
        return center;
    }

    public float getHeadY() {
        return headY;
    }
}
