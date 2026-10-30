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
    HATCH(EngineSetting.SHIPWRIGHT_TEXT_HATCH);

    // Values
    public static final ShipOpening[] VALUES = values();

    // Identity
    private final String title;

    // Constructor \\

    ShipOpening(String title) {

        // Identity
        this.title = title;
    }

    // The opening a step along the list lies at, round either end
    public ShipOpening step(int step) {
        return VALUES[Math.floorMod(ordinal() + step, VALUES.length)];
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }
}
