package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.EngineSetting;

public enum ShipOpening {

    /*
     * What the openings tool cuts at the drafting table. A door or a window
     * is set on the wall, bulwark or planking it is cut through and turns to
     * face across it; a hatch lies in a deck's floor, closing the opening a
     * stair or ladder climbs through when it is set on one, or cutting one of
     * its own. Each is kept in the blueprint list of its own kind.
     */

    DOOR(EngineSetting.SHIPWRIGHT_TEXT_DOOR),
    WINDOW(EngineSetting.SHIPWRIGHT_TEXT_WINDOW),
    HATCH(EngineSetting.SHIPWRIGHT_TEXT_HATCH),
    GRATE(EngineSetting.SHIPWRIGHT_TEXT_GRATE);

    // Values
    public static final ShipOpening[] VALUES = values();

    // Identity
    private final String title;

    // Constructor \\

    ShipOpening(String title) {

        // Identity
        this.title = title;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }

    // True for an opening that lies in a deck's floor and turns freely, not set on a wall
    public boolean isFloor() {
        return this == HATCH || this == GRATE;
    }
}
