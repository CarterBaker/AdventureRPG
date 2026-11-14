package application.bootstrap.vehiclepipeline.vehicle;

public enum VehiclePartRole {

    /*
     * What one part of a vehicle is for, named in lower case by vehicle data.
     * A role fixes everything about how the part behaves: whether riders and
     * cargo collide with it, whether it keeps the sea out of the hull, whether
     * it can be climbed, how it moves when it is drawn, and what the activate
     * binding does to it. A mast needs a pivot, the vertical axis its yards
     * brace about; a yard names its mast and a sail its yard; a helm needs a
     * pivot and an axle, and a rudder a pivot, the hinge it swings about. An
     * anchor is drawn lowered while it is down, its cable hanging only then. A
     * door, a hatch or a port lid needs a pivot and an axis, the hinge it
     * swings open about, and a portcullis, such as a row of port lids, lifts
     * clear by its own height and seals the hull like its planking does; both
     * stop riders and cargo only while they stand shut.
     */

    HULL(true, true, false, VehiclePartMotion.STATIC, VehiclePartControl.NONE),
    DECK(true, false, false, VehiclePartMotion.STATIC, VehiclePartControl.NONE),
    STRUCTURE(true, false, false, VehiclePartMotion.STATIC, VehiclePartControl.NONE),
    MAST(true, false, false, VehiclePartMotion.STATIC, VehiclePartControl.NONE),
    YARD(false, false, false, VehiclePartMotion.BRACE, VehiclePartControl.HOIST),
    SAIL(false, false, false, VehiclePartMotion.FURL, VehiclePartControl.HOIST),
    RIGGING(false, false, false, VehiclePartMotion.STATIC, VehiclePartControl.NONE),
    LADDER(false, false, true, VehiclePartMotion.STATIC, VehiclePartControl.NONE),
    HELM(true, false, false, VehiclePartMotion.WHEEL, VehiclePartControl.STEER),
    RUDDER(false, false, false, VehiclePartMotion.RUDDER, VehiclePartControl.NONE),
    ANCHOR(false, false, false, VehiclePartMotion.DROP, VehiclePartControl.NONE),
    CABLE(false, false, false, VehiclePartMotion.PAYOUT, VehiclePartControl.NONE),
    CAPSTAN(true, false, false, VehiclePartMotion.STATIC, VehiclePartControl.MOOR),
    DOOR(true, false, false, VehiclePartMotion.SWING, VehiclePartControl.OPEN),
    PORTCULLIS(true, true, false, VehiclePartMotion.LIFT, VehiclePartControl.OPEN);

    // Values
    public static final VehiclePartRole[] VALUES = values();

    // Behaviour
    private final boolean solid;
    private final boolean watertight;
    private final boolean climbable;
    private final VehiclePartMotion motion;
    private final VehiclePartControl control;

    // Constructor \\

    VehiclePartRole(
            boolean solid,
            boolean watertight,
            boolean climbable,
            VehiclePartMotion motion,
            VehiclePartControl control) {

        // Behaviour
        this.solid = solid;
        this.watertight = watertight;
        this.climbable = climbable;
        this.motion = motion;
        this.control = control;
    }

    // Accessible \\

    public boolean isSolid() {
        return solid;
    }

    public boolean isWatertight() {
        return watertight;
    }

    public boolean isClimbable() {
        return climbable;
    }

    public VehiclePartMotion getMotion() {
        return motion;
    }

    public VehiclePartControl getControl() {
        return control;
    }

    public boolean isStatic() {
        return motion == VehiclePartMotion.STATIC;
    }
}
