package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class CurrentTrackerBranch extends BranchPackage {

    /*
     * Advances the sub-day clock every frame from the real system clock.
     * The world's epoch is the real instant its calendar's start date and
     * start time were reached, so the start time is folded into the elapsed
     * value once and totalDaysElapsed, dayProgress, hour, and minute all fall
     * out of that single number. The date rolls over at exactly the instant
     * dayProgress wraps to midnight, and any gap since the last session
     * resolves in one step. computeVisualTimeOfDay() then localizes the
     * shared solar time per grid, bending it by season and latitude.
     */

    // Internal
    private long MILLIS_PER_REAL_DAY;
    private double LATITUDE_CURVE_POWER;

    // Seasonal Bending
    private double SUNRISE_MIN;
    private double SUNRISE_MAX;
    private double SUNSET_MIN;
    private double SUNSET_MAX;
    private double NOON;
    private double QUARTER;
    private double THREE_QUARTERS;

    // Calendar
    private CalendarHandle calendarHandle;
    private long millisPerGameDay;
    private long startMillisIntoDay;
    private int totalDaysInYear;

    // Per-world
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
            float axialTilt) {

        this.clockHandle = clockHandle;
        this.calendarHandle = calendarHandle;
        this.millisPerGameDay = MILLIS_PER_REAL_DAY / calendarHandle.getDaysPerDay();
        this.startMillisIntoDay = calculateStartMillisIntoDay();
        this.totalDaysInYear = calendarHandle.getTotalDaysInYear();
        this.axialTiltStrength = Math.max(
                0.0,
                axialTilt / EngineSetting.LATITUDE_DAYLENGTH_REFERENCE_TILT_DEGREES);
        this.lastDay = -1;
    }

    private long calculateStartMillisIntoDay() {

        int minutesPerDay = calendarHandle.getHoursPerDay() * calendarHandle.getMinutesPerHour();
        int startMinuteOfDay = calendarHandle.getStartHour() * calendarHandle.getMinutesPerHour()
                + calendarHandle.getStartMinute();

        return Math.round(startMinuteOfDay / (double) minutesPerDay * millisPerGameDay);
    }

    // Global Time \\

    boolean advanceGlobalTime() {

        long elapsedSinceEpoch = internal.getTime() - clockHandle.getWorldEpochStart();
        long elapsedSinceStartDay = elapsedSinceEpoch + startMillisIntoDay;

        long totalDaysElapsed = Math.floorDiv(elapsedSinceStartDay, millisPerGameDay);
        long millisIntoCurrentGameDay = Math.floorMod(elapsedSinceStartDay, millisPerGameDay);

        double dayProgress = millisIntoCurrentGameDay / (double) millisPerGameDay;
        int minuteOfDay = calculateMinuteOfDay(dayProgress);

        clockHandle.setWorldSecondsElapsed(elapsedSinceEpoch / EngineSetting.MILLIS_PER_SECOND);
        clockHandle.setTotalDaysElapsed(totalDaysElapsed);
        clockHandle.setDayProgress(dayProgress);
        clockHandle.setRawTimeOfDay(calculateRawTimeOfDay(dayProgress));
        clockHandle.setCurrentHour(minuteOfDay / calendarHandle.getMinutesPerHour());
        clockHandle.setCurrentMinute(minuteOfDay % calendarHandle.getMinutesPerHour());

        boolean dayChanged = lastDay != totalDaysElapsed;
        lastDay = totalDaysElapsed;

        return dayChanged;
    }

    // Visual Year \\

    /*
     * Year progress is only recomputed per whole day, so the fraction of
     * the current day is added on top here to give a continuous point in
     * the year for anything blending across seasons.
     */
    void advanceVisualYear() {

        double visualYearProgress = clockHandle.getYearProgress()
                + clockHandle.getDayProgress() / totalDaysInYear;

        clockHandle.setVisualYearProgress(wrapFraction(visualYearProgress));
    }

    // Location Time \\

    double computeVisualTimeOfDay(double locationOffset, double latitudeFactor) {

        double rawTimeOfDay = clockHandle.getRawTimeOfDay();
        double yearProgress = clockHandle.getVisualYearProgress();

        double localRawTimeOfDay = wrapFraction(rawTimeOfDay + locationOffset);

        return calculateVisualTimeOfDay(localRawTimeOfDay, yearProgress, latitudeFactor);
    }

    // Calculations \\

    /*
     * Solar time for the calendar's reference location: the calendar's
     * middayOffset is the fraction of its day at which the sun peaks, so it
     * is shifted onto NOON here. Clock-face hours and minutes are read from
     * dayProgress instead and are never shifted.
     */
    double calculateRawTimeOfDay(double dayProgress) {
        return wrapFraction(dayProgress - calendarHandle.getMiddayOffset() + NOON);
    }

    int calculateMinuteOfDay(double dayProgress) {

        int minutesPerDay = calendarHandle.getHoursPerDay() * calendarHandle.getMinutesPerHour();
        int minuteOfDay = (int) (dayProgress * minutesPerDay);

        return Math.min(minuteOfDay, minutesPerDay - 1);
    }

    double calculateVisualTimeOfDay(double rawTimeOfDay, double yearProgress, double latitudeFactor) {

        float seasonDayLength = calendarHandle.getDayLengthForYearProgress(yearProgress);
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
