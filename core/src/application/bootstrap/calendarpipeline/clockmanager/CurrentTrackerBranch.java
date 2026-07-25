package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class CurrentTrackerBranch extends BranchPackage {

    /*
     * Advances the sub-day clock every frame from the real system clock.
     * totalDaysElapsed (whole calendar days since the world's epoch) and
     * dayProgress (fraction of the current calendar day) are both derived
     * from the same elapsed-since-epoch value and the same
     * millisPerGameDay divisor, so the calendar date always rolls over at
     * exactly the instant dayProgress wraps to midnight. computeVisualTimeOfDay()
     * then localizes that shared raw time per grid, bending it by season
     * and by latitude so day length varies correctly with time of year and
     * with distance from the equator.
     */

    // Internal
    private long MILLIS_PER_REAL_DAY;
    private double LATITUDE_CURVE_POWER;

    // Seasonal Bending — safety bounds only; the shift amount itself comes
    // from SeasonBlendBranch's data-driven day length.
    private double SUNRISE_MIN;
    private double SUNRISE_MAX;
    private double SUNSET_MIN;
    private double SUNSET_MAX;
    private double NOON;
    private double QUARTER;
    private double THREE_QUARTERS;

    // Calendar
    private CalendarHandle calendarHandle;
    private SeasonBlendBranch seasonBlendBranch;

    // Per-world
    private int daysPerDay;
    private float axialTilt;
    private double axialTiltStrength;
    private ClockHandle clockHandle;

    // Tracking
    private long lastDay;

    // Internal \\

    @Override
    protected void create() {

        this.MILLIS_PER_REAL_DAY = EngineSetting.MILLIS_PER_REAL_DAY;
        this.LATITUDE_CURVE_POWER = EngineSetting.LATITUDE_DAYLENGTH_CURVE_POWER;

        this.SUNRISE_MIN = EngineSetting.CLOCK_SUNRISE_MIN;
        this.SUNRISE_MAX = EngineSetting.CLOCK_SUNRISE_MAX;
        this.SUNSET_MIN = EngineSetting.CLOCK_SUNSET_MIN;
        this.SUNSET_MAX = EngineSetting.CLOCK_SUNSET_MAX;
        this.NOON = EngineSetting.CLOCK_NOON;
        this.QUARTER = EngineSetting.CLOCK_QUARTER;
        this.THREE_QUARTERS = EngineSetting.CLOCK_THREE_QUARTERS;

        this.lastDay = -1;
    }

    // Assignment \\

    void assignData(
            CalendarHandle calendarHandle,
            ClockHandle clockHandle,
            float axialTilt,
            SeasonBlendBranch seasonBlendBranch) {
        this.clockHandle = clockHandle;
        this.seasonBlendBranch = seasonBlendBranch;
        setCalendarHandle(calendarHandle);
        setAxialTilt(axialTilt);
    }

    void setCalendarHandle(CalendarHandle calendarHandle) {
        this.calendarHandle = calendarHandle;
        this.daysPerDay = calendarHandle.getDaysPerDay();
    }

    void setAxialTilt(float axialTilt) {
        this.axialTilt = axialTilt;
        this.axialTiltStrength = Math.max(
                0.0,
                axialTilt / EngineSetting.LATITUDE_DAYLENGTH_REFERENCE_TILT_DEGREES);
    }

    // Global Time \\

    boolean advanceGlobalTime() {

        long now = internal.getTime();
        long millisPerGameDay = MILLIS_PER_REAL_DAY / daysPerDay;

        long elapsedSinceEpoch = now - clockHandle.getWorldEpochStart();
        long totalDaysElapsed = Math.floorDiv(elapsedSinceEpoch, millisPerGameDay);
        long millisIntoCurrentGameDay = Math.floorMod(elapsedSinceEpoch, millisPerGameDay);

        double dayProgress = millisIntoCurrentGameDay / (double) millisPerGameDay;

        double rawTimeOfDay = calculateRawTimeOfDay(dayProgress);
        int currentMinute = calculateMinute(rawTimeOfDay);
        int currentHour = calculateHour(rawTimeOfDay);

        clockHandle.setTotalDaysElapsed(totalDaysElapsed);
        clockHandle.setDayProgress(dayProgress);
        clockHandle.setRawTimeOfDay(rawTimeOfDay);
        clockHandle.setCurrentMinute(currentMinute);
        clockHandle.setCurrentHour(currentHour);

        boolean dayChanged = lastDay != totalDaysElapsed;
        lastDay = totalDaysElapsed;

        return dayChanged;
    }

    // Location Time \\

    double computeVisualTimeOfDay(double locationOffset, double latitudeFactor) {

        double rawTimeOfDay = clockHandle.getRawTimeOfDay();
        double yearProgress = clockHandle.getYearProgress();

        double localRawTimeOfDay = wrapFraction(rawTimeOfDay + locationOffset);

        return calculateVisualTimeOfDay(localRawTimeOfDay, yearProgress, latitudeFactor);
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

    // latitudeFactor is signed (-1 at one pole, 0 at either equator
    // crossing, +1 at the other pole), so the deviation from an even 0.5
    // day/night split fades to nothing at the equator and reaches full
    // strength at the poles, flipping direction between hemispheres.
    // axialTiltStrength normalizes the world's own axial tilt against an
    // Earth-like reference — an upright world (tilt 0) collapses this to
    // a flat 0.5 everywhere.
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