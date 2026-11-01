package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.EngineSetting;

public enum ShipWall {

    /*
     * What the walls tool paints on a deck at the drafting table: a full
     * wall up to the deck above, or a deckhouse high in the open; a half wall
     * waist high; or a railing, rail high, on open floor or in place of the
     * ship's own side or bulwark at the deck's edge, above the waterline, so
     * a porch or a gallery is a deck railed round instead of walled. Each is
     * kept in the blueprint list of its own kind, a place holding at most one
     * of them.
     */

    FULL(EngineSetting.SHIPWRIGHT_TEXT_FULL_WALL),
    HALF(EngineSetting.SHIPWRIGHT_TEXT_HALF_WALL),
    RAILING(EngineSetting.SHIPWRIGHT_TEXT_RAILING);

    // Values
    public static final ShipWall[] VALUES = values();

    // Identity
    private final String title;

    // Constructor \\

    ShipWall(String title) {

        // Identity
        this.title = title;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }
}
