package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
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
     * own change checks. Every day gets its own seed, hashed from the
     * world's seed and the absolute day, so no two days share one and the
     * same day always reproduces the same seed on any load. Any system can
     * draw its own independent random stream from it through
     * resolveDailyRandom(), which eases each day's value into the next
     * day's across the day so nothing driven by it ever pops at rollover.
     */

    // Internal
    private long STREAM_SALT;
    private float NOISE_MIN;
    private CalendarHandle calendarHandle;
    private ClockHandle clockHandle;

    // Conversion Tables
    private Int2ObjectOpenHashMap<Int2IntOpenHashMap> monthToDayOfMonthToDayOfYear;
    private Int2IntOpenHashMap dayOfYearToDayOfMonth;
    private Int2IntOpenHashMap dayOfYearToMonth;

    // Calendar Offset
    private long startDayOffset;

    // Day Seed
    private long worldSeed;
    private long currentDaySeed;
    private long nextDaySeed;
    private float daySeedBlend;

    // Tracking
    private long lastDayElapsed;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.STREAM_SALT = EngineSetting.CLOCK_DAILY_STREAM_SALT;
        this.NOISE_MIN = EngineSetting.CLOCK_NOISE_MIN;

        // Conversion Tables
        this.monthToDayOfMonthToDayOfYear = new Int2ObjectOpenHashMap<>();
        this.dayOfYearToDayOfMonth = new Int2IntOpenHashMap();
        this.dayOfYearToMonth = new Int2IntOpenHashMap();

        // Tracking
        this.lastDayElapsed = -1;
    }

    // Assignment \\

    void assignData(CalendarHandle calendarHandle, ClockHandle clockHandle, long worldSeed) {

        // Internal
        this.calendarHandle = calendarHandle;
        this.clockHandle = clockHandle;

        // Day Seed
        this.worldSeed = worldSeed;

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

        this.currentDaySeed = calculateDaySeed(totalDaysWithOffset);
        this.nextDaySeed = calculateDaySeed(totalDaysWithOffset + 1);

        clockHandle.setCurrentDaySeed(currentDaySeed);
        clockHandle.setTotalDaysWithOffset(totalDaysWithOffset);
        clockHandle.setYearProgress(calculateYearProgress(dayOfYear));
        clockHandle.setCurrentDayOfWeek(calculateDayOfWeek(totalDaysWithOffset));
        clockHandle.setCurrentDayOfMonth(getDayOfMonthFromDayOfYear(dayOfYear));
        clockHandle.setCurrentMonth(getMonthFromDayOfYear(dayOfYear));

        return true;
    }

    // Daily Random \\

    void advanceDayBlend() {

        float t = (float) clockHandle.getDayProgress();

        this.daySeedBlend = t * t * (3f - 2f * t);

        float noise = resolveDailyRandom01(EngineSetting.CLOCK_DAILY_STREAM_NOISE);

        clockHandle.setRandomNoiseFromDay(Math.max(NOISE_MIN, noise));
    }

    // Signed [-1, 1] random for one stream of today's seed, eased into the
    // same stream of tomorrow's seed as the day progresses.
    float resolveDailyRandom(long stream) {
        return resolveDailyRandom01(stream) * 2f - 1f;
    }

    private float resolveDailyRandom01(long stream) {

        long streamSalt = stream * STREAM_SALT;
        float current = BiomeFieldUtility.hash01(currentDaySeed ^ streamSalt);
        float next = BiomeFieldUtility.hash01(nextDaySeed ^ streamSalt);

        return current + (next - current) * daySeedBlend;
    }

    // Calculations \\

    long calculateStartDayOffset() {

        int startDayOfYear = getDayOfYearFromDayAndMonth(
                calendarHandle.getStartDayOfMonth(),
                calendarHandle.getStartMonth());

        return (long) calendarHandle.getStartYear() * calendarHandle.getTotalDaysInYear() + startDayOfYear - 1;
    }

    long calculateDaySeed(long totalDaysWithOffset) {
        return BiomeFieldUtility.hashCell(
                worldSeed,
                (int) totalDaysWithOffset,
                (int) (totalDaysWithOffset >>> 32));
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
