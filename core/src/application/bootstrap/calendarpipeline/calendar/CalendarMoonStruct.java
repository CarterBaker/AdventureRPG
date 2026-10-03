package application.bootstrap.calendarpipeline.calendar;

import engine.root.StructPackage;

public class CalendarMoonStruct extends StructPackage {

    /*
     * The orbit of the moon this calendar's world keeps, beside the lunar
     * cycle that sets its phases: the phase it shows on the calendar's very
     * first day, how far its path tilts from the sun's, how many days it takes
     * to swing above that path and back, how many days lie between one close
     * pass and the next, and how much larger it looks at its closest. Every
     * value follows the shape of a real moon's orbit in the calendar's own
     * units of time.
     */

    // Internal
    private final float phaseOffset;
    private final float inclination;
    private final float nodalCycleDays;
    private final float distanceCycleDays;
    private final float sizeVariation;

    // Calculated
    private final float inclinationRadians;

    // Constructor \\

    public CalendarMoonStruct(
            float phaseOffset,
            float inclination,
            float nodalCycleDays,
            float distanceCycleDays,
            float sizeVariation) {

        // Internal
        this.phaseOffset = phaseOffset;
        this.inclination = inclination;
        this.nodalCycleDays = nodalCycleDays;
        this.distanceCycleDays = distanceCycleDays;
        this.sizeVariation = sizeVariation;

        // Calculated
        this.inclinationRadians = (float) Math.toRadians(inclination);
    }

    // Accessible \\

    public float getPhaseOffset() {
        return phaseOffset;
    }

    public float getInclination() {
        return inclination;
    }

    public float getInclinationRadians() {
        return inclinationRadians;
    }

    public float getNodalCycleDays() {
        return nodalCycleDays;
    }

    public float getDistanceCycleDays() {
        return distanceCycleDays;
    }

    public float getSizeVariation() {
        return sizeVariation;
    }
}
