package application.bootstrap.lightingpipeline.naturallightmanager;

import application.bootstrap.calendarpipeline.clock.ClockInstance;
import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.calendarpipeline.util.CelestialUtility;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;

class StarSphereSystem extends SystemPackage {

    /*
     * Turns the star sphere for one place each frame. The stars are fixed on
     * the sphere; the sphere turns with the grid's own time of day and the
     * year's progress, and tilts its pole to the grid's latitude, so the sky
     * any window sees is the true sky for its date, hour and place. The
     * resulting world-to-sphere rotation is read by NaturalLightManager and
     * pushed to that grid's CelestialData UBO.
     */

    // Output
    private final Matrix4 rotation = new Matrix4();

    // Internal
    private ClockManager clockManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.clockManager = get(ClockManager.class);
    }

    // Update \\

    void update(ClockInstance clockInstance) {

        double siderealTime = CelestialUtility.resolveSiderealTime(
                clockInstance.getVisualTimeOfDay(),
                clockManager.getClockHandle().getVisualYearProgress());
        double latitude = CelestialUtility.resolveObserverLatitude(clockInstance.getLatitudeFactor());

        CelestialUtility.resolveStarRotation(siderealTime, latitude, rotation);
    }

    // Accessible \\

    Matrix4 getRotation() {
        return rotation;
    }
}
