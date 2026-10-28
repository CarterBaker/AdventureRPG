package application.bootstrap.worldpipeline.world;

public enum WorldPlacementKind {

    /*
     * What a hand-placed entry in a world's companion file stands up: a whole
     * settlement laid out around it, one structure on its own, or a camera
     * the main menu looks out from, which stands nothing up, so generation
     * never reads it and a change to it regenerates nothing.
     */

    SETTLEMENT(true), // A settlement of the named type, its layout grown from the placement
    STRUCTURE(true), // One structure, anchored on the ground at the placement
    CAMERA(false); // A view of the world for the main menu, raised above the ground at the placement

    // Values
    public static final WorldPlacementKind[] VALUES = values();

    // Internal
    private final boolean generated;

    // Constructor \\

    WorldPlacementKind(boolean generated) {
        this.generated = generated;
    }

    // Accessible \\

    // True for a placement world generation stands up, whose every change reaches the terrain around it
    public boolean isGenerated() {
        return generated;
    }
}
