package application.bootstrap.vehiclepipeline.vehicle;

import engine.root.StructPackage;

public class VehicleHandlingStruct extends StructPackage {

    /*
     * How one vehicle type handles, as its data tunes it. The hull's drag
     * through the water along, across and up its length, per block of hull
     * under water; how fast water floods in once the deck edge goes under and
     * how fast the crew pumps it out again; how hard its sails pull, how far
     * and how fast its yards brace and how fast a sail is set or taken in; how
     * far and how fast its rudder swings and how hard it bites; and how firmly
     * its anchor holds and how far below its cathead it hangs once dropped,
     * in blocks. Angles are radians.
     */

    // Hull
    private final float surgeDrag;
    private final float swayDrag;
    private final float heaveDrag;
    private final float floodRate;
    private final float pumpRate;

    // Rig
    private final float sailForce;
    private final float braceLimit;
    private final float braceRate;
    private final float hoistRate;

    // Steering
    private final float rudderLimit;
    private final float rudderRate;
    private final float rudderForce;

    // Anchor
    private final float anchorHold;
    private final float anchorDrop;

    // Constructor \\

    public VehicleHandlingStruct(
            float surgeDrag,
            float swayDrag,
            float heaveDrag,
            float floodRate,
            float pumpRate,
            float sailForce,
            float braceLimit,
            float braceRate,
            float hoistRate,
            float rudderLimit,
            float rudderRate,
            float rudderForce,
            float anchorHold,
            float anchorDrop) {

        // Hull
        this.surgeDrag = surgeDrag;
        this.swayDrag = swayDrag;
        this.heaveDrag = heaveDrag;
        this.floodRate = floodRate;
        this.pumpRate = pumpRate;

        // Rig
        this.sailForce = sailForce;
        this.braceLimit = braceLimit;
        this.braceRate = braceRate;
        this.hoistRate = hoistRate;

        // Steering
        this.rudderLimit = rudderLimit;
        this.rudderRate = rudderRate;
        this.rudderForce = rudderForce;

        // Anchor
        this.anchorHold = anchorHold;
        this.anchorDrop = anchorDrop;
    }

    // Accessible \\

    public float getSurgeDrag() {
        return surgeDrag;
    }

    public float getSwayDrag() {
        return swayDrag;
    }

    public float getHeaveDrag() {
        return heaveDrag;
    }

    public float getFloodRate() {
        return floodRate;
    }

    public float getPumpRate() {
        return pumpRate;
    }

    public float getSailForce() {
        return sailForce;
    }

    public float getBraceLimit() {
        return braceLimit;
    }

    public float getBraceRate() {
        return braceRate;
    }

    public float getHoistRate() {
        return hoistRate;
    }

    public float getRudderLimit() {
        return rudderLimit;
    }

    public float getRudderRate() {
        return rudderRate;
    }

    public float getRudderForce() {
        return rudderForce;
    }

    public float getAnchorHold() {
        return anchorHold;
    }

    public float getAnchorDrop() {
        return anchorDrop;
    }
}
