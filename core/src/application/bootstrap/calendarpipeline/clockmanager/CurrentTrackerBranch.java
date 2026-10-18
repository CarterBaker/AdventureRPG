package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.weatherpipeline.util.SkyColorUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class CurrentTrackerBranch extends BranchPackage {

    /*
     * Advances the sub-day clock every frame from the real system clock.
     * The world's epoch is the real instant its calendar's start date and
     * start time were reached, so the start time is folded into the elapsed
     * value once and totalDaysElapsed, dayProgress, hour, and minute all fall
     * out of that single number, alongside the elapsed real seconds and the
     * same span in game seconds, which run daysPerDay times faster. The date
     * rolls over at exactly the instant dayProgress wraps to midnight, and
     * any gap since the epoch resolves in one step. Moving the clock to a day
     * and minute shifts the epoch, so time keeps running from there; the
     * minute is resolved in whole milliseconds so a move lands on exactly the
     * minute asked for.
     * computeVisualTimeOfDay() then localizes the shared solar time per grid,
     * bending it by season and latitude, the bend deepening toward the poles
     * so high latitudes see long winter nights and long summer days.
     */

    // Internal
    private long MILLIS_PER_REAL_DAY;
    private double LATITUDE_CURVE_POWER;
    private float LATITUDE_POLAR_START;
    private double LATITUDE_POLAR_GAIN;

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
        this.LATITUDE_POLAR_START = EngineSetting.LATITUDE_DAYLENGTH_POLAR_START;
        this.LATITUDE_POLAR_GAIN = EngineSetting.LATITUDE_DAYLENGTH_POLAR_GAIN;

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

        resetTracking();
    }

    void resetTracking() {
        this.lastDay = -1;
    }

    private long calculateStartMillisIntoDay() {
        return resolveMillisIntoDay(calendarHandle.getStartHour() * calendarHandle.getMinutesPerHour()
                + calendarHandle.getStartMinute());
    }

    // Global Time \\

    boolean advanceGlobalTime() {

        long elapsedSinceEpoch = internal.getTime() - clockHandle.getWorldEpochStart();
        long elapsedSinceStartDay = elapsedSinceEpoch + startMillisIntoDay;

        long totalDaysElapsed = Math.floorDiv(elapsedSinceStartDay, millisPerGameDay);
        long millisIntoCurrentGameDay = Math.floorMod(elapsedSinceStartDay, millisPerGameDay);

        double dayProgress = millisIntoCurrentGameDay / (double) millisPerGameDay;
        int minuteOfDay = calculateMinuteOfDay(millisIntoCurrentGameDay);

        clockHandle.setWorldSecondsElapsed(elapsedSinceEpoch / EngineSetting.MILLIS_PER_SECOND);
        clockHandle.setGameSecondsElapsed(
                elapsedSinceEpoch * (double) calendarHandle.getDaysPerDay() / EngineSetting.MILLIS_PER_SECOND);
        clockHandle.setTotalDaysElapsed(totalDaysElapsed);
        clockHandle.setDayProgress(dayProgress);
        clockHandle.setRawTimeOfDay(calculateRawTimeOfDay(dayProgress));
        clockHandle.setCurrentHour(minuteOfDay / calendarHandle.getMinutesPerHour());
        clockHandle.setCurrentMinute(minuteOfDay % calendarHandle.getMinutesPerHour());

        boolean dayChanged = lastDay != totalDaysElapsed;
        lastDay = totalDaysElapsed;

        return dayChanged;
    }

    // Move \\

    // Rebases the epoch so the clock reads the start of this minute on this day right now
    void moveTo(long totalDaysElapsed, int minuteOfDay) {

        long elapsedSinceStartDay = totalDaysElapsed * millisPerGameDay + resolveMillisIntoDay(minuteOfDay);

        clockHandle.setWorldEpochStart(internal.getTime() - (elapsedSinceStartDay - startMillisIntoDay));
    }

    // Visual Year \\

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

    double calculateRawTimeOfDay(double dayProgress) {
        return wrapFraction(dayProgress - calendarHandle.getMiddayOffset() + NOON);
    }

    // Whole-millisecond math, so a minute's first millisecond never reads as the minute before
    int calculateMinuteOfDay(long millisIntoDay) {
        return (int) (millisIntoDay * resolveMinutesPerDay() / millisPerGameDay);
    }

    // The first whole millisecond of a minute
    private long resolveMillisIntoDay(int minuteOfDay) {
        return Math.ceilDiv(minuteOfDay * millisPerGameDay, resolveMinutesPerDay());
    }

    private int resolveMinutesPerDay() {
        return calendarHandle.getHoursPerDay() * calendarHandle.getMinutesPerHour();
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

    // Bends the day length away from an even split toward the poles, flipped per hemisphere and scaled by tilt
    double applyLatitudeBend(float seasonDayLength, double latitudeFactor) {

        double curvedLatitude = Math.signum(latitudeFactor)
                * Math.pow(Math.abs(latitudeFactor), LATITUDE_CURVE_POWER);

        double delta = (seasonDayLength - 0.5) * curvedLatitude * axialTiltStrength
                * resolvePolarGain(latitudeFactor);

        return Math.max(0.0, Math.min(1.0, 0.5 + delta));
    }

    // Ramps the bend up past the polar start, so the extremes gather at high latitudes instead of spreading evenly
    private double resolvePolarGain(double latitudeFactor) {

        float polarWeight = SkyColorUtility.smoothstep(
                LATITUDE_POLAR_START,
                1f,
                (float) Math.abs(latitudeFactor));

        return 1.0 + (LATITUDE_POLAR_GAIN - 1.0) * polarWeight;
    }

    double wrapFraction(double value) {

        double wrapped = value % 1.0;

        if (wrapped < 0)
            wrapped += 1.0;

        return wrapped;
    }
}
