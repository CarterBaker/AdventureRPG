package application.bootstrap.entitypipeline.pathfinding;

public enum PathLayer {

    /*
     * What one sub-block layer of a block column holds, as a path search
     * reads it. OPEN lets a body through, SOLID holds one up and stops it —
     * terrain, solid items and tree wood alike — and LIQUID neither holds a
     * body up nor lets a path through, so no way is ever found into water.
     */

    OPEN,
    SOLID,
    LIQUID;

    // Values
    public static final PathLayer[] VALUES = values();
}
