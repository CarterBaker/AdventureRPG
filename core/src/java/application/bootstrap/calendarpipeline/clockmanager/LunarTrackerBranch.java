package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.calendarpipeline.util.CelestialUtility;
import engine.root.BranchPackage;

class LunarTrackerBranch extends BranchPackage {

    /*
     * Places the moon in its orbit every frame from the calendar date. The
     * days counted from the calendar's first day, carried to the current
     * fraction of a day, drive three cycles at once: the lunar cycle sets the
     * phase, new at 0 and full at one half, and how much of the face is lit;
     * the nodal cycle swings the moon above and below the sun's path by the
     * orbit's inclination; and the distance cycle grows and shrinks how large
     * it looks. A full moon landing on a close pass looks larger, and a new or
     * full moon landing on the sun's path eclipses, as a real moon does. A
     * calendar with no lunar cycle has no moon.
     */

    private static final double TWO_PI = Math.PI * 2.0;

    // Internal
    private CalendarHandle calendarHandle;
    private ClockHandle clockHandle;

    // Assignment \\

    void assignData(CalendarHandle calendarHandle, ClockHandle clockHandle) {

        // Internal
        this.calendarHandle = calendarHandle;
        this.clockHandle = clockHandle;
    }

    // Lunar Tracker \\

    void advanceOrbit() {

        if (!calendarHandle.hasMoon()) {
            clearOrbit();
            return;
        }

        double days = clockHandle.getTotalDaysWithOffset() + clockHandle.getDayProgress();
        double phase = CelestialUtility.wrapFraction(
                days / calendarHandle.getLunarCycleDays() + calendarHandle.getMoonPhaseOffset());
        double nodalAngle = days / calendarHandle.getMoonNodalCycleDays() * TWO_PI;
        double distanceAngle = days / calendarHandle.getMoonDistanceCycleDays() * TWO_PI;

        clockHandle.setLunarPhase(phase);
        clockHandle.setLunarIllumination((float) ((1.0 - Math.cos(phase * TWO_PI)) * 0.5));
        clockHandle.setLunarLatitude((float) (calendarHandle.getMoonInclinationRadians() * Math.sin(nodalAngle)));
        clockHandle.setLunarSizeScale((float) (1.0 + calendarHandle.getMoonSizeVariation() * Math.cos(distanceAngle)));
    }

    private void clearOrbit() {

        clockHandle.setLunarPhase(0.0);
        clockHandle.setLunarIllumination(0f);
        clockHandle.setLunarLatitude(0f);
        clockHandle.setLunarSizeScale(0f);
    }
}
