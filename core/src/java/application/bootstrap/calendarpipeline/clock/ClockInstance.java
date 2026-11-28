package application.bootstrap.calendarpipeline.clock;

import application.bootstrap.calendarpipeline.util.CelestialUtility;
import engine.root.InstancePackage;

public class ClockInstance extends InstancePackage {

    /*
     * Time-of-day state for one place on the world's Y axis rather than the
     * world as a whole — two locations far enough apart on that axis can be
     * in day and night simultaneously. Recomputed every frame per grid by
     * ClockManager, from the shared global clock plus that grid's own
     * active chunk coordinate. One instance per grid, created once and
     * held for that grid's lifetime — never shared, never rebuilt. Solar
     * elevation is the vertical component of the sun's direction for that
     * same visual time, from -1 (nadir) through 0 (horizon) to 1 (zenith).
     * The north heading is the angle from the grid's +Z toward its +X at
     * which true north lies there, which turns the sky onto the grid.
     */

    // State
    private double visualTimeOfDay;
    private double locationOffset;
    private double latitudeFactor;
    private double northHeading;
    private double solarElevation;

    // Update \\

    public void update(
            double visualTimeOfDay,
            double locationOffset,
            double latitudeFactor,
            double northHeading) {

        this.visualTimeOfDay = visualTimeOfDay;
        this.locationOffset = locationOffset;
        this.latitudeFactor = latitudeFactor;
        this.northHeading = northHeading;
        this.solarElevation = CelestialUtility.resolveSolarElevation(visualTimeOfDay);
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

    public double getNorthHeading() {
        return northHeading;
    }

    public double getSolarElevation() {
        return solarElevation;
    }
}