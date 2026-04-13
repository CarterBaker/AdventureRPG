package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.core.engine.BranchPackage;
import application.core.settings.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

class DayTrackerBranch extends BranchPackage {

    /*
     * Advances the day-level clock when the day rolls over. Builds lookup
     * tables from the calendar definition for fast day-of-year to month and
     * day-of-month resolution. Returns true when the month advances.
     */

    // Internal
    private int STARTING_DAY_OF_MONTH;
    private int STARTING_MONTH;
    private int STARTING_YEAR;
    private int YEARS_PER_AGE;
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
        this.STARTING_DAY_OF_MONTH = EngineSetting.STARTING_DAY_OF_MONTH;
        this.STARTING_MONTH = EngineSetting.STARTING_MONTH;
        this.STARTING_YEAR = EngineSetting.STARTING_YEAR;
        this.YEARS_PER_AGE = EngineSetting.YEARS_PER_AGE;
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

        return currentDayOfMonth == 1;
    }

    // Calculations \\

    long calculateTotalDaysWithOffset(long totalDaysElapsed) {

        int dayOfYear = 0;

        for (int i = 0; i < STARTING_MONTH - 1; i++)
            dayOfYear += calendarHandle.getMonthDays(i);

        dayOfYear += STARTING_DAY_OF_MONTH - 1;

        long dayOffset = (long) STARTING_YEAR * calendarHandle.getTotalDaysInYear() + dayOfYear;

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
        long dayOfAge = totalDaysWithOffset % ((long) YEARS_PER_AGE * totalDaysInYear);

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