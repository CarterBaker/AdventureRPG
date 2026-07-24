// CurrentTrackerBranch.java
package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class CurrentTrackerBranch extends BranchPackage {

    /*
     * Advances the sub-day clock every frame from the real system clock,
     * applying the calendar's middayOffset so real-world noon lines up with
     * in-game noon. visualTimeOfDay folds in a location phase (see
     * WorldWrapUtility.wrappedPlanetaryOffset, supplied by ClockManager)
     * before the day-length bend, so different points along the world's Y
     * axis experience day and night at different moments, wrapping
     * seamlessly at the world edges. The day-length bend itself starts from
     * SeasonBlendBranch's data-driven curve and is then reshaped by
     * latitude (see WorldWrapUtility.wrappedLatitudeFactor) — flat toward
     * the equator, exaggerated toward the poles, scaled by the world's own
     * axial tilt.
     */

    // Internal
    private long MILLIS_PER_REAL_DAY;
    private double LATITUDE_CURVE_POWER;

    // Seasonal Bending — safety bounds only; the shift amount itself now
    // comes from SeasonBlendBranch's data-driven day length.
    private double SUNRISE_MIN;
    private double SUNRISE_MAX;
    private double SUNSET_MIN;
    private double SUNSET_MAX;
    private double MIDNIGHT;
    private double NOON;
    private double QUARTER;
    private double THREE_QUARTERS;

    // Calendar
    private CalendarHandle calendarHandle;
    private SeasonBlendBranch seasonBlendBranch;

    // Per-world
    private float daysPerDay;
    private float axialTilt;
    private double axialTiltStrength;
    private ClockHandle clockHandle;

    // Tracking
    private long lastDay;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.MILLIS_PER_REAL_DAY = EngineSetting.MILLIS_PER_REAL_DAY;
        this.LATITUDE_CURVE_POWER = EngineSetting.LATITUDE_DAYLENGTH_CURVE_POWER;

        // Seasonal Bending
        this.SUNRISE_MIN = EngineSetting.CLOCK_SUNRISE_MIN;
        this.SUNRISE_MAX = EngineSetting.CLOCK_SUNRISE_MAX;
        this.SUNSET_MIN = EngineSetting.CLOCK_SUNSET_MIN;
        this.SUNSET_MAX = EngineSetting.CLOCK_SUNSET_MAX;
        this.MIDNIGHT = EngineSetting.CLOCK_MIDNIGHT;
        this.NOON = EngineSetting.CLOCK_NOON;
        this.QUARTER = EngineSetting.CLOCK_QUARTER;
        this.THREE_QUARTERS = EngineSetting.CLOCK_THREE_QUARTERS;

        // Tracking
        this.lastDay = -1;
    }

    // Assignment \\

    void assignData(
            CalendarHandle calendarHandle,
            ClockHandle clockHandle,
            float daysPerDay,
            float axialTilt,
            SeasonBlendBranch seasonBlendBranch) {
        this.calendarHandle = calendarHandle;
        this.clockHandle = clockHandle;
        this.daysPerDay = daysPerDay;
        this.seasonBlendBranch = seasonBlendBranch;
        setAxialTilt(axialTilt);
    }

    void setCalendarHandle(CalendarHandle calendarHandle) {
        this.calendarHandle = calendarHandle;
    }

    void setDaysPerDay(float daysPerDay) {
        this.daysPerDay = daysPerDay;
    }

    void setAxialTilt(float axialTilt) {
        this.axialTilt = axialTilt;
        this.axialTiltStrength = Math.max(
                0.0,
                axialTilt / EngineSetting.LATITUDE_DAYLENGTH_REFERENCE_TILT_DEGREES);
    }

    // Current Tracker \\

    boolean advanceTime(double locationOffset, double latitudeFactor) {

        long now = internal.getTime();
        long millisPerGameDay = (long) (MILLIS_PER_REAL_DAY / daysPerDay);

        long totalDaysElapsed = (now - clockHandle.getWorldEpochStart()) / millisPerGameDay;
        double dayProgress = ((double) (now % MILLIS_PER_REAL_DAY) / MILLIS_PER_REAL_DAY * daysPerDay) % 1.0;

        double rawTimeOfDay = calculateRawTimeOfDay(dayProgress);
        int currentMinute = calculateMinute(rawTimeOfDay);
        int currentHour = calculateHour(rawTimeOfDay);
        double yearProgress = clockHandle.getYearProgress();

        double localRawTimeOfDay = wrapFraction(rawTimeOfDay + locationOffset);
        double visualTimeOfDay = calculateVisualTimeOfDay(localRawTimeOfDay, yearProgress, latitudeFactor);

        clockHandle.setTotalDaysElapsed(totalDaysElapsed);
        clockHandle.setDayProgress(dayProgress);
        clockHandle.setVisualTimeOfDay(visualTimeOfDay);
        clockHandle.setCurrentMinute(currentMinute);
        clockHandle.setCurrentHour(currentHour);

        boolean dayChanged = lastDay != totalDaysElapsed;
        lastDay = totalDaysElapsed;

        return dayChanged;
    }

    // Calculations \\

    double calculateRawTimeOfDay(double dayProgress) {

        double raw = (dayProgress + calendarHandle.getMiddayOffset()) % 1.0;

        if (raw < 0)
            raw += 1.0;

        return raw;
    }

    int calculateMinute(double rawTimeOfDay) {

        int hoursPerDay = calendarHandle.getHoursPerDay();
        int minutesPerHour = calendarHandle.getMinutesPerHour();

        return (int) ((rawTimeOfDay * hoursPerDay * minutesPerHour) % minutesPerHour);
    }

    int calculateHour(double rawTimeOfDay) {
        return (int) (rawTimeOfDay * calendarHandle.getHoursPerDay());
    }

    /*
     * Bends raw time of day using the current daylight fraction, so day
     * length actually shrinks and grows through the year and across
     * latitude rather than following a fixed sine curve. 0.0 and 1.0 are
     * always midnight. 0.5 is always visual noon. The bend only affects the
     * rate at which time moves between those anchors.
     */
    double calculateVisualTimeOfDay(double rawTimeOfDay, double yearProgress, double latitudeFactor) {

        float seasonDayLength = seasonBlendBranch.getDayLengthForYearProgress(yearProgress);
        double dayLength = applyLatitudeBend(seasonDayLength, latitudeFactor);
        double shift = (dayLength - 0.5) * 0.5;

        double actualSunrise = Math.max(SUNRISE_MIN, Math.min(SUNRISE_MAX, QUARTER - shift));
        double actualSunset = Math.max(SUNSET_MIN, Math.min(SUNSET_MAX, THREE_QUARTERS + shift));

        if (rawTimeOfDay < actualSunrise)
            return (rawTimeOfDay / actualSunrise) * QUARTER;

        if (rawTimeOfDay < NOON)
            return QUARTER + ((rawTimeOfDay - actualSunrise) / (NOON - actualSunrise)) * QUARTER;

        if (rawTimeOfDay < actualSunset)
            return NOON + ((rawTimeOfDay - NOON) / (actualSunset - NOON)) * QUARTER;

        return THREE_QUARTERS + ((rawTimeOfDay - actualSunset) / (1.0 - actualSunset)) * QUARTER;
    }

    /*
     * Reshapes the calendar's authored seasonal day length by latitude.
     * latitudeFactor is signed (-1 at one pole, 0 at either equator
     * crossing, +1 at the other pole — see
     * WorldWrapUtility.wrappedLatitudeFactor), so the deviation from an
     * even 0.5 day/night split fades smoothly to nothing at the equator and
     * reaches full strength at the poles, flipping direction between the
     * two hemispheres exactly as real seasons do. axialTiltStrength is the
     * world's own axial tilt normalized against an Earth-like reference —
     * an upright world (tilt 0) collapses this to a flat 0.5 everywhere.
     */
    double applyLatitudeBend(float seasonDayLength, double latitudeFactor) {

        double curvedLatitude = Math.signum(latitudeFactor)
                * Math.pow(Math.abs(latitudeFactor), LATITUDE_CURVE_POWER);

        double delta = (seasonDayLength - 0.5) * curvedLatitude * axialTiltStrength;

        return Math.max(0.0, Math.min(1.0, 0.5 + delta));
    }

    double wrapFraction(double value) {

        double wrapped = value % 1.0;

        if (wrapped < 0)
            wrapped += 1.0;

        return wrapped;
    }
}