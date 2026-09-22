package application.bootstrap.worldpipeline.structure;

import engine.root.StructPackage;

public class StructurePlacementStruct extends StructPackage {

    /*
     * One resolved placement of a structure definition in the world — the
     * outcome of a spawn cell's roll surviving every constraint, or of a
     * hand-authored location. x/z are the placement point in world blocks,
     * wrapped into the world's canonical range; y is the floor level the
     * structure's anchor sits on; depthBlocks is how far below the surface
     * an UNDERGROUND placement was buried (0 otherwise). Identity
     * is placementID, which is a pure function of (definition, spawn cell)
     * or (definition, location index), so every thread and every chunk that
     * resolves the same placement agrees on it.
     */

    private final long placementID;
    private final StructureHandle structureHandle;

    private final double x;
    private final double z;
    private final int y;
    private final int rotation;
    private final boolean handcrafted;
    private final int depthBlocks;

    public StructurePlacementStruct(
            long placementID,
            StructureHandle structureHandle,
            double x, double z, int y,
            int rotation,
            boolean handcrafted,
            int depthBlocks) {

        this.placementID = placementID;
        this.structureHandle = structureHandle;
        this.x = x;
        this.z = z;
        this.y = y;
        this.rotation = rotation;
        this.handcrafted = handcrafted;
        this.depthBlocks = depthBlocks;
    }

    /*
     * Total order used to settle overlaps: hand-authored locations first,
     * then type priority, then the larger footprint, then ID so the order
     * is always strict.
     */
    public boolean outranks(StructurePlacementStruct other) {

        if (handcrafted != other.handcrafted)
            return handcrafted;

        int priority = structureHandle.getStructureType().placementPriority;
        int otherPriority = other.structureHandle.getStructureType().placementPriority;

        if (priority != otherPriority)
            return priority > otherPriority;

        int radius = getRadiusBlocks();
        int otherRadius = other.getRadiusBlocks();

        if (radius != otherRadius)
            return radius > otherRadius;

        return placementID < other.placementID;
    }

    public long getPlacementID() {
        return placementID;
    }

    public StructureHandle getStructureHandle() {
        return structureHandle;
    }

    public double getX() {
        return x;
    }

    public double getZ() {
        return z;
    }

    public int getY() {
        return y;
    }

    public int getRotation() {
        return rotation;
    }

    public boolean isHandcrafted() {
        return handcrafted;
    }

    public int getDepthBlocks() {
        return depthBlocks;
    }

    public int getRadiusBlocks() {
        return structureHandle.getBoundingRadiusBlocks();
    }
}
