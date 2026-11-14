package application.bootstrap.worldpipeline.layout;

import application.bootstrap.worldpipeline.structure.StructureHandle;
import engine.root.StructPackage;

public class LayoutLotStruct extends StructPackage {

    /*
     * One structure a layout stands up: the structure and its clockwise
     * quarter turns, its anchor as a wrapped world block column and height
     * for stamping, its anchor in the layout's own unwrapped blocks, and the
     * box its turned footprint covers there, minimum inclusive and maximum
     * exclusive, for fitting, sampling and claims.
     */

    // Structure
    private final StructureHandle structureHandle;
    private final int quarterTurns;

    // Anchor — wrapped world blocks
    private final long anchorX;
    private final long anchorZ;
    private final int anchorY;

    // Plan — the layout's unwrapped blocks
    private final long planX;
    private final long planZ;
    private final double minX;
    private final double maxX;
    private final double minZ;
    private final double maxZ;

    // Constructor \\

    public LayoutLotStruct(
            StructureHandle structureHandle,
            int quarterTurns,
            long anchorX,
            long anchorZ,
            int anchorY,
            long planX,
            long planZ,
            double minX,
            double maxX,
            double minZ,
            double maxZ) {

        // Structure
        this.structureHandle = structureHandle;
        this.quarterTurns = quarterTurns;

        // Anchor
        this.anchorX = anchorX;
        this.anchorZ = anchorZ;
        this.anchorY = anchorY;

        // Plan
        this.planX = planX;
        this.planZ = planZ;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    // Accessible \\

    public StructureHandle getStructureHandle() {
        return structureHandle;
    }

    public int getQuarterTurns() {
        return quarterTurns;
    }

    public long getAnchorX() {
        return anchorX;
    }

    public long getAnchorZ() {
        return anchorZ;
    }

    public int getAnchorY() {
        return anchorY;
    }

    public long getPlanX() {
        return planX;
    }

    public long getPlanZ() {
        return planZ;
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

    // True when a point of the layout lies within a margin of the lot's box
    public boolean covers(double x, double z, double margin) {
        return x >= minX - margin && x < maxX + margin && z >= minZ - margin && z < maxZ + margin;
    }
}
