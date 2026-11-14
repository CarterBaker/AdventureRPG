package application.bootstrap.shipwrightpipeline.shipwright;

public enum ShipPieceShape {

    /*
     * How the octants of a ship piece are built into its part: merged whole
     * into the fewest boxes, as the hull, decks and walls are; as a rail, a
     * post up each octant under a handrail run out to the rails beside it; or
     * as a pane, a sheet through the middle of each octant reaching out to
     * every wall or pane beside it, so glass runs straight along a wall and
     * bends round its corners.
     */

    BLOCKS,
    RAILING,
    PANE;

    // Values
    public static final ShipPieceShape[] VALUES = values();
}
