package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.EngineSetting;

public enum ShipClimb {

    /*
     * What the stairs tool sets down at the drafting table. A staircase is set
     * at its foot and always climbs one deck the way it faces; a ladder stands
     * on two by two octants of floor and climbs to the floor over it; a
     * boarding ladder hangs down the outside of the hull from a deck's edge,
     * rope and rungs to climb aboard from the water by. Each is kept in the
     * blueprint list of its own kind.
     */

    STAIR(EngineSetting.SHIPWRIGHT_TEXT_STAIR),
    LADDER(EngineSetting.SHIPWRIGHT_TEXT_LADDER),
    BOARDING(EngineSetting.SHIPWRIGHT_TEXT_BOARDING);

    // Values
    public static final ShipClimb[] VALUES = values();

    // Identity
    private final String title;

    // Constructor \\

    ShipClimb(String title) {

        // Identity
        this.title = title;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }
}
