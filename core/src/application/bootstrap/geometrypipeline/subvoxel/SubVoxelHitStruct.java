package application.bootstrap.geometrypipeline.subvoxel;

import engine.root.StructPackage;

public class SubVoxelHitStruct extends StructPackage {

    /*
     * Result of one ray cast into a sub-voxel model: the filled cell struck and
     * the empty cell a new cube would go in. Reused in place by its owner.
     */

    // Target
    private boolean hasTarget;
    private int targetX;
    private int targetY;
    private int targetZ;

    // Placement
    private boolean hasPlacement;
    private int placeX;
    private int placeY;
    private int placeZ;

    // Management \\

    public void clear() {

        this.hasTarget = false;
        this.hasPlacement = false;
    }

    public void setTarget(int x, int y, int z) {

        this.hasTarget = true;
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    public void setPlacement(int x, int y, int z) {

        this.hasPlacement = true;
        this.placeX = x;
        this.placeY = y;
        this.placeZ = z;
    }

    // Accessible \\

    public boolean hasHit() {
        return hasTarget || hasPlacement;
    }

    public boolean hasTarget() {
        return hasTarget;
    }

    public int getTargetX() {
        return targetX;
    }

    public int getTargetY() {
        return targetY;
    }

    public int getTargetZ() {
        return targetZ;
    }

    public boolean hasPlacement() {
        return hasPlacement;
    }

    public int getPlaceX() {
        return placeX;
    }

    public int getPlaceY() {
        return placeY;
    }

    public int getPlaceZ() {
        return placeZ;
    }
}
