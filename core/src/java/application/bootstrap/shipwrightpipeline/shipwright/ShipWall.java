package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.EngineSetting;

public enum ShipWall {

    /*
     * What the walls tool paints on a deck at the drafting table: a full
     * wall up to the deck above, or a deckhouse high in the open; a half wall
     * waist high; a railing, rail high, on open floor or in place of the
     * ship's own side or bulwark at the deck's edge, above the waterline, so
     * a porch or a gallery is a deck railed round instead of walled; or a lip,
     * a low sill stepped over, in place of the side the same way, so the side
     * of a lower deck opens as a way in. Each is kept in the blueprint list of
     * its own kind, a place holding at most one of them.
     */

    FULL(EngineSetting.SHIPWRIGHT_TEXT_FULL_WALL, ShipPiece.WALLS, EngineSetting.SHIP_DECK_HEIGHT - 1),
    HALF(EngineSetting.SHIPWRIGHT_TEXT_HALF_WALL, ShipPiece.WALLS, EngineSetting.SHIP_HALF_WALL_HEIGHT),
    RAILING(EngineSetting.SHIPWRIGHT_TEXT_RAILING, ShipPiece.RAILS, EngineSetting.SHIP_RAILING_HEIGHT),
    LIP(EngineSetting.SHIPWRIGHT_TEXT_LIP, ShipPiece.LIPS, 1);

    // Values
    public static final ShipWall[] VALUES = values();

    // Identity
    private final String title;

    // Shape — the piece it is laid as and the octants it rises under cover
    private final ShipPiece piece;
    private final int height;

    // Constructor \\

    ShipWall(String title, ShipPiece piece, int height) {

        // Identity
        this.title = title;

        // Shape
        this.piece = piece;
        this.height = height;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }

    public ShipPiece getPiece() {
        return piece;
    }

    public int getHeight() {
        return height;
    }

    // True for a wall laid in place of the deck's edge, under the railing's rule: above the waterline, on open floor
    // or in place of the side or a bulwark
    public boolean isEdge() {
        return this == RAILING || this == LIP;
    }
}
