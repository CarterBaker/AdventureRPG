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
     * day-of-month resolution. Month/day-of-month/day-of-week/year-progress
     * are always derived directly from the elapsed day count (never
     * incremented one day at a time), so any gap — the game being closed
     * for days, months, or years — lands on the correct values in one shot.
     *
     * advanceTime() returns true whenever the day was actually recomputed
     * (the elapsed day count changed) — not only when the new day-of-month
     * happens to be 1. MonthTrackerBranch/YearTrackerBranch each do their
     * own direct "did this actually change" comparison downstream, so it's
     * cheap and correct to let them check every time a day passes; gating
     * on day-of-month == 1 would miss any month/year rollover that doesn't
     * land exactly on the 1st, which is guaranteed to happen after a long
     * enough absence (or even on a freshly created world whose calendar
     * start date isn't the 1st of its first month).
     *
     * The world's starting year/month/day and years-per-age all come from
     * the active calendar now, rather than fixed engine-wide constants.
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
    }

    private void buildDayConversionTables() {

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

        long totalDaysWithOffset = calculateTotalDaysWithOffset(totalDaysElapsed);
        float randomNoise = calculateRandomNoise(totalDaysWithOffset);
        double yearProgress = calculateYearProgress(totalDaysWithOffset);
        double visualYearProgress = calculateVisualYearProgress(yearProgress);
        int currentDayOfWeek = calculateDayOfWeek(totalDaysWithOffset);

        int totalDaysInYear = calendarHandle.getTotalDaysInYear();
        int dayOfYear = (int) ((totalDaysWithOffset % totalDaysInYear) + 1);
        int currentMonth = getMonthFromDayOfYear(dayOfYear);
        int currentDayOfMonth = getDayOfMonthFromDayOfYear(dayOfYear);

        clockHandle.setTotalDaysWithOffset(totalDaysWithOffset);
        clockHandle.setRandomNoiseFromDay(randomNoise);
        clockHandle.setYearProgress(yearProgress);
        clockHandle.setVisualYearProgress(visualYearProgress);
        clockHandle.setCurrentDayOfWeek(currentDayOfWeek);
        clockHandle.setCurrentDayOfMonth(currentDayOfMonth);
        clockHandle.setCurrentMonth(currentMonth);

        return true;
    }

    // Calculations \\

    long calculateTotalDaysWithOffset(long totalDaysElapsed) {

        int startMonth = calendarHandle.getStartMonth();
        int startDayOfMonth = calendarHandle.getStartDayOfMonth();
        int startYear = calendarHandle.getStartYear();

        int dayOfYear = 0;

        for (int i = 0; i < startMonth; i++)
            dayOfYear += calendarHandle.getMonthDays(i);

        dayOfYear += startDayOfMonth - 1;

        long dayOffset = (long) startYear * calendarHandle.getTotalDaysInYear() + dayOfYear;

        return totalDaysElapsed + dayOffset;
    }

    float calculateRandomNoise(long totalDaysWithOffset) {

        long mixed = totalDaysWithOffset ^ System.currentTimeMillis();

        mixed ^= mixed >>> 33;
        mixed *= NOISE_MULTIPLIER;
        mixed ^= mixed >>> 33;

        double normalized = (double) (mixed & NOISE_MASK) / NOISE_DIVISOR;

        return (float) Math.max(NOISE_MIN, normalized);
    }

    double calculateYearProgress(long totalDaysWithOffset) {

        int totalDaysInYear = calendarHandle.getTotalDaysInYear();
        long yearsPerAgeDays = (long) calendarHandle.getYearsPerAge() * totalDaysInYear;
        long dayOfAge = totalDaysWithOffset % yearsPerAgeDays;

        return (double) (dayOfAge % totalDaysInYear) / totalDaysInYear;
    }

    double calculateVisualYearProgress(double yearProgress) {
        return yearProgress;
    }

    int calculateDayOfWeek(long totalDaysWithOffset) {
        return (int) ((totalDaysWithOffset % calendarHandle.getDaysPerWeek()) + 1);
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