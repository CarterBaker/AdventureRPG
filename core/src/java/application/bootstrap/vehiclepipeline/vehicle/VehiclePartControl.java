package application.bootstrap.vehiclepipeline.vehicle;

public enum VehiclePartControl {

    /*
     * What the activate binding does to a vehicle part an entity faces within
     * reach. STEER takes or leaves the helm, HOIST sets or takes in the sails
     * of a yard, MOOR drops or weighs the anchor, and OPEN opens or shuts a
     * door, hatch, lid or portcullis. NONE parts are only walked on, climbed
     * or looked at.
     */

    NONE,
    STEER,
    HOIST,
    MOOR,
    OPEN;

    // Values
    public static final VehiclePartControl[] VALUES = values();
}
