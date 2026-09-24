package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

class DayTrackerBranch extends BranchPackage {

    /*
     * Advances the day-level clock when the day rolls over. Builds lookup
     * tables from the calendar definition for fast day-of-year to month and
     * day-of-month resolution. Every value is derived directly from the
     * elapsed day count rather than incremented, so any gap since the last
     * session lands on the correct date in one step, and advanceTime()
     * reports every recomputed day so month and year trackers can run their
     * own change checks. The daily noise is hashed from the world's epoch
     * and the absolute day, so the same day always reads the same value,
     * and blendDailyNoise() eases it into the next day's value across the
     * day so nothing driven by it ever pops at rollover.
     */

    // Internal
    private long NOISE_MASK;
    private double NOISE_DIVISOR;
    private long NOISE_MULTIPLIER;
    private double NOISE_MIN;
    private CalendarHandle calendarHandle;
    private ClockHandle clockHandle;

    // Conversion Tables
    private Int2ObjectOpenHashMap<Int2IntOpenHashMap> monthToDayOfMonthToDayOfYear;
    private Int2IntOpenHashMap dayOfYearToDayOfMonth;
    private Int2IntOpenHashMap dayOfYearToMonth;

    // Calendar Offset
    private long startDayOffset;

    // Daily Noise
    private float currentDayNoise;
    private float nextDayNoise;

    // Tracking
    private long lastDayElapsed;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.NOISE_MASK = EngineSetting.CLOCK_NOISE_MASK;
        this.NOISE_DIVISOR = EngineSetting.CLOCK_NOISE_DIVISOR;
        this.NOISE_MULTIPLIER = EngineSetting.CLOCK_NOISE_MULTIPLIER;
        this.NOISE_MIN = EngineSetting.CLOCK_NOISE_MIN;

        // Conversion Tables
        this.monthToDayOfMonthToDayOfYear = new Int2ObjectOpenHashMap<>();
        this.dayOfYearToDayOfMonth = new Int2IntOpenHashMap();
        this.dayOfYearToMonth = new Int2IntOpenHashMap();

        // Tracking
        this.lastDayElapsed = -1;
    }

    // Assignment \\

    void assignData(CalendarHandle calendarHandle, ClockHandle clockHandle) {

        // Internal
        this.calendarHandle = calendarHandle;
        this.clockHandle = clockHandle;

        buildDayConversionTables();

        // Calendar Offset
        this.startDayOffset = calculateStartDayOffset();

        // Tracking
        this.lastDayElapsed = -1;
    }

    private void buildDayConversionTables() {

        monthToDayOfMonthToDayOfYear.clear();
        dayOfYearToDayOfMonth.clear();
        dayOfYearToMonth.clear();

        int runningDayOfYear = 1;
        int monthCount = calendarHandle.getMonthCount();

        for (int monthIndex = 0; monthIndex < monthCount; monthIndex++) {

            int daysInMonth = calendarHandle.getMonthDays(monthIndex);
            Int2IntOpenHashMap dayToYear = new Int2IntOpenHashMap(daysInMonth);

            for (int dayOfMonth = 1; dayOfMonth <= daysInMonth; dayOfMonth++) {

                dayToYear.put(dayOfMonth, runningDayOfYear);
                dayOfYearToMonth.put(runningDayOfYear, monthIndex);
                dayOfYearToDayOfMonth.put(runningDayOfYear, dayOfMonth);

                runningDayOfYear++;
            }

            monthToDayOfMonthToDayOfYear.put(monthIndex, dayToYear);
        }
    }

    // Day Tracker \\

    boolean advanceTime() {

        long totalDaysElapsed = clockHandle.getTotalDaysElapsed();

        if (lastDayElapsed == totalDaysElapsed)
            return false;

        lastDayElapsed = totalDaysElapsed;

        long totalDaysWithOffset = totalDaysElapsed + startDayOffset;
        int dayOfYear = (int) Math.floorMod(totalDaysWithOffset, (long) calendarHandle.getTotalDaysInYear()) + 1;

        this.currentDayNoise = calculateRandomNoise(totalDaysWithOffset);
        this.nextDayNoise = calculateRandomNoise(totalDaysWithOffset + 1);

        clockHandle.setTotalDaysWithOffset(totalDaysWithOffset);
        clockHandle.setYearProgress(calculateYearProgress(dayOfYear));
        clockHandle.setCurrentDayOfWeek(calculateDayOfWeek(totalDaysWithOffset));
        clockHandle.setCurrentDayOfMonth(getDayOfMonthFromDayOfYear(dayOfYear));
        clockHandle.setCurrentMonth(getMonthFromDayOfYear(dayOfYear));

        return true;
    }

    // Daily Noise \\

    void blendDailyNoise() {

        double t = clockHandle.getDayProgress();
        double eased = t * t * (3.0 - 2.0 * t);

        clockHandle.setRandomNoiseFromDay((float) (currentDayNoise + (nextDayNoise - currentDayNoise) * eased));
    }

    // Calculations \\

    long calculateStartDayOffset() {

        int startDayOfYear = getDayOfYearFromDayAndMonth(
                calendarHandle.getStartDayOfMonth(),
                calendarHandle.getStartMonth());

        return (long) calendarHandle.getStartYear() * calendarHandle.getTotalDaysInYear() + startDayOfYear - 1;
    }

    float calculateRandomNoise(long totalDaysWithOffset) {

        long mixed = totalDaysWithOffset ^ clockHandle.getWorldEpochStart();

        mixed ^= mixed >>> 33;
        mixed *= NOISE_MULTIPLIER;
        mixed ^= mixed >>> 33;

        double normalized = (double) (mixed & NOISE_MASK) / NOISE_DIVISOR;

        return (float) Math.max(NOISE_MIN, normalized);
    }

    double calculateYearProgress(int dayOfYear) {
        return (dayOfYear - 1) / (double) calendarHandle.getTotalDaysInYear();
    }

    int calculateDayOfWeek(long totalDaysWithOffset) {
        return (int) Math.floorMod(totalDaysWithOffset, (long) calendarHandle.getDaysPerWeek()) + 1;
    }

    // Conversion Utilities \\

    int getDayOfYearFromDayAndMonth(int dayOfMonth, int month) {

        Int2IntOpenHashMap dayOfMonthToDayOfYear = monthToDayOfMonthToDayOfYear.get(month);

        if (dayOfMonthToDayOfYear == null)
            throwException("Invalid month index: " + month);

        if (!dayOfMonthToDayOfYear.containsKey(dayOfMonth))
            throwException("Invalid dayOfMonth: " + dayOfMonth);

        return dayOfMonthToDayOfYear.get(dayOfMonth);
    }

    int getDayOfMonthFromDayOfYear(int dayOfYear) {
        return dayOfYearToDayOfMonth.get(dayOfYear);
    }

    int getMonthFromDayOfYear(int dayOfYear) {
        return dayOfYearToMonth.get(dayOfYear);
    }
}
