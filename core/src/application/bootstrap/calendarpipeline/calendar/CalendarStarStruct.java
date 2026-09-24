package application.bootstrap.calendarpipeline.calendar;

import engine.root.StructPackage;

public class CalendarStarStruct extends StructPackage {

    /*
     * The star this calendar's world orbits: its distance in astronomical
     * units and its luminosity relative to our sun. The calendar's year is
     * that orbit, so the star lives beside it. Received light falls off with
     * the square of distance and a planet's temperature follows the fourth
     * root of received light, so temperatureScale — 1.0 for an Earth-like
     * orbit — is the factor every absolute temperature is multiplied by.
     */

    // Internal
    private final float distance;
    private final float luminosity;

    // Calculated
    private final float temperatureScale;

    // Constructor \\

    public CalendarStarStruct(float distance, float luminosity) {

        // Internal
        this.distance = distance;
        this.luminosity = luminosity;

        // Calculated
        this.temperatureScale = (float) Math.pow(luminosity / ((double) distance * distance), 0.25);
    }

    // Accessible \\

    public float getDistance() {
        return distance;
    }

    public float getLuminosity() {
        return luminosity;
    }

    public float getTemperatureScale() {
        return temperatureScale;
    }
}
