package application.bootstrap.worldpipeline.layout;

import application.bootstrap.worldpipeline.road.RoadPathStruct;
import engine.root.StructPackage;

public class LayoutPlanStruct extends StructPackage {

    /*
     * A planned layout, immutable once sealed: the roads it lays, the walls
     * it raises and the structures it stands on its lots, a settlement, the
     * roads between settlements, or any other pathed arrangement. Every
     * position is in the layout's own unwrapped blocks, measured around its
     * reference column, so the whole plan stays continuous across the
     * world's wrap; a world position is brought into it by its wrapped
     * distance from the reference. The bounds take in everything it lays.
     */

    // Reference — wrapped world blocks
    private final double referenceX;
    private final double referenceZ;

    // Contents
    private final RoadPathStruct[] paths;
    private final LayoutWallStruct[] walls;
    private final LayoutLotStruct[] lots;

    // Bounds — the layout's unwrapped blocks
    private final double minX;
    private final double maxX;
    private final double minZ;
    private final double maxZ;

    // Constructor \\

    public LayoutPlanStruct(
            double referenceX,
            double referenceZ,
            RoadPathStruct[] paths,
            LayoutWallStruct[] walls,
            LayoutLotStruct[] lots,
            double minX,
            double maxX,
            double minZ,
            double maxZ) {

        // Reference
        this.referenceX = referenceX;
        this.referenceZ = referenceZ;

        // Contents
        this.paths = paths;
        this.walls = walls;
        this.lots = lots;

        // Bounds
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    // Accessible \\

    public double getReferenceX() {
        return referenceX;
    }

    public double getReferenceZ() {
        return referenceZ;
    }

    public RoadPathStruct[] getPaths() {
        return paths;
    }

    public LayoutWallStruct[] getWalls() {
        return walls;
    }

    public LayoutLotStruct[] getLots() {
        return lots;
    }

    public double getMinX() {
        return minX;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMinZ() {
        return minZ;
    }

    public double getMaxZ() {
        return maxZ;
    }

    // True when a rectangle of the layout's blocks overlaps its bounds
    public boolean overlaps(double rectMinX, double rectMinZ, double rectMaxX, double rectMaxZ) {
        return rectMaxX >= minX && rectMinX <= maxX && rectMaxZ >= minZ && rectMinZ <= maxZ;
    }
}
