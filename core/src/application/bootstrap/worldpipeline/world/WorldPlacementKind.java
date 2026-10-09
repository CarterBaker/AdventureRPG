package application.bootstrap.worldpipeline.world;

public enum WorldPlacementKind {

    /*
     * What a hand-placed entry in a world's companion file stands up: a whole
     * settlement laid out around it, or one structure on its own.
     */

    SETTLEMENT, // A settlement of the named type, its layout grown from the placement
    STRUCTURE // One structure, anchored on the ground at the placement
}
