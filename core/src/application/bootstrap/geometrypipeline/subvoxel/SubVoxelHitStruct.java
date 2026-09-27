package application.bootstrap.geometrypipeline.subvoxel;

import engine.root.StructPackage;

public class SubVoxelHitStruct extends StructPackage {

    /*
     * Result of one ray cast into a sub-voxel model: the cube or wall struck,
     * the empty cell a new cube would go in, and the wall a new wall would
     * become. A struck wall keeps its axis, and its position's coordinate on
     * that axis is its plane. Reused in place by its owner.
     */

    // Target
    private boolean hasTarget;
    private boolean targetWall;
    private int targetAxis;
    private int targetX;
    private int targetY;
    private int targetZ;

    // Placement
    private boolean hasPlacement;
    private int placeX;
    private int placeY;
    private int placeZ;

    // Wall Placement
    private boolean hasWallPlacement;
    private int wallPlaceAxis;
    private int wallPlaceX;
    private int wallPlaceY;
    private int wallPlaceZ;

    // Management \\

    public void clear() {

        this.hasTarget = false;
        this.targetWall = false;
        this.hasPlacement = false;
        this.hasWallPlacement = false;
    }

    public void setTarget(int x, int y, int z) {

        this.hasTarget = true;
        this.targetWall = false;
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    public void setWallTarget(int axis, int x, int y, int z) {

        this.hasTarget = true;
        this.targetWall = true;
        this.targetAxis = axis;
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

    public void setWallPlacement(int axis, int x, int y, int z) {

        this.hasWallPlacement = true;
        this.wallPlaceAxis = axis;
        this.wallPlaceX = x;
        this.wallPlaceY = y;
        this.wallPlaceZ = z;
    }

    // Accessible \\

    public boolean hasHit() {
        return hasTarget || hasPlacement;
    }

    public boolean hasTarget() {
        return hasTarget;
    }

    public boolean isTargetWall() {
        return targetWall;
    }

    public int getTargetAxis() {
        return targetAxis;
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

    public boolean hasWallPlacement() {
        return hasWallPlacement;
    }

    public int getWallPlaceAxis() {
        return wallPlaceAxis;
    }

    public int getWallPlaceX() {
        return wallPlaceX;
    }

    public int getWallPlaceY() {
        return wallPlaceY;
    }

    public int getWallPlaceZ() {
        return wallPlaceZ;
    }
}
