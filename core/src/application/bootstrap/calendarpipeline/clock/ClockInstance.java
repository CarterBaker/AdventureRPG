package application.bootstrap.calendarpipeline.clock;

import engine.root.InstancePackage;

public class ClockInstance extends InstancePackage {

    /*
     * Time-of-day state for one place on the world's Y axis rather than the
     * world as a whole — two locations far enough apart on that axis can be
     * in day and night simultaneously. Recomputed every frame per grid by
     * ClockManager, from the shared global clock plus that grid's own
     * active chunk coordinate. One instance per grid, created once and
     * held for that grid's lifetime — never shared, never rebuilt.
     */

    // State
    private double visualTimeOfDay;
    private double locationOffset;
    private double latitudeFactor;

    // Update \\

    public void update(double visualTimeOfDay, double locationOffset, double latitudeFactor) {
        this.visualTimeOfDay = visualTimeOfDay;
        this.locationOffset = locationOffset;
        this.latitudeFactor = latitudeFactor;
    }

    // Accessible \\

    public double getVisualTimeOfDay() {
        return visualTimeOfDay;
    }

    public double getLocationOffset() {
        return locationOffset;
    }

    public double getLatitudeFactor() {
        return latitudeFactor;
    }
}