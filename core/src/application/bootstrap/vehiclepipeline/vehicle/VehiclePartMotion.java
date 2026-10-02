package application.bootstrap.vehiclepipeline.vehicle;

public enum VehiclePartMotion {

    /*
     * How a vehicle part moves when it is drawn. STATIC parts are merged into
     * the vehicle's shared hull meshes; every other part keeps meshes of its
     * own and is drawn through its own transform: BRACE turns about its mast
     * with the yards, FURL turns with them and gathers up toward its yard as
     * it is taken in, WHEEL spins about its axle with the rudder, RUDDER swings
     * about its hinge, and STOWED is only drawn while the anchor is aboard.
     */

    STATIC,
    BRACE,
    FURL,
    WHEEL,
    RUDDER,
    STOWED;

    // Values
    public static final VehiclePartMotion[] VALUES = values();
}
