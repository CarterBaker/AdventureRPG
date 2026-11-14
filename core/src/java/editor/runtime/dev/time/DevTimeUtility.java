package editor.runtime.dev.time;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.calendar.SeasonRangeStruct;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.calendarpipeline.util.CelestialUtility;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class DevTimeUtility extends EngineUtility {

    /*
     * Stateless reads and moves of the shared clock for each unit the time
     * command advances, shared by the Dev window that runs it and the command
     * console's dials that send it. Each unit reads as a whole number in a
     * range that may depend on the date — the day runs to the current month's
     * length. Advancing a unit by an amount moves the clock as a real one
     * runs: every smaller unit stays as it is and every larger one carries,
     * so sixty minutes past the hour is the next hour and a month past the
     * last is the next year. A month keeps the day of the month, held to the
     * new month's length; a season lands on the first day of the season it
     * reaches; the weekday and the moon move the date a day at a time. The
     * clock reaches from the first day of the calendar's starting age to the
     * last day of the final age the editor allows.
     */

    // Availability \\

    public static boolean isAvailable(DevTimeUnit unit, CalendarHandle calendar) {
        return switch (unit) {
            case SEASON -> !calendar.getSeasons().isEmpty();
            case MOON -> calendar.hasMoon();
            default -> true;
        };
    }

    // Range \\

    public static int getMinimum(DevTimeUnit unit, CalendarHandle calendar) {
        return switch (unit) {
            case HOUR, MINUTE, YEAR -> 0;
            case WEEKDAY, DAY, MONTH, SEASON, MOON -> 1;
            case AGE -> calendar.getStartAge();
        };
    }

    public static int getMaximum(DevTimeUnit unit, ClockHandle clock, CalendarHandle calendar) {
        return switch (unit) {
            case HOUR -> calendar.getHoursPerDay() - 1;
            case MINUTE -> calendar.getMinutesPerHour() - 1;
            case WEEKDAY -> calendar.getDaysPerWeek();
            case DAY -> calendar.getMonthDays(clock.getCurrentMonth());
            case MONTH -> calendar.getMonthCount();
            case SEASON -> calendar.getSeasons().size();
            case YEAR -> calendar.getYearsPerAge() - 1;
            case AGE -> calendar.getStartAge() + EditorSetting.DEV_TIME_AGE_SPAN;
            case MOON -> calendar.getLunarCycleDays();
        };
    }

    // Value \\

    public static int getValue(DevTimeUnit unit, ClockHandle clock, CalendarHandle calendar) {
        return switch (unit) {
            case HOUR -> clock.getCurrentHour();
            case MINUTE -> clock.getCurrentMinute();
            case WEEKDAY -> clock.getCurrentDayOfWeek();
            case DAY -> clock.getCurrentDayOfMonth();
            case MONTH -> clock.getCurrentMonth() + 1;
            case SEASON -> resolveSeasonValue(clock, calendar);
            case YEAR -> clock.getCurrentYear();
            case AGE -> clock.getCurrentAge();
            case MOON -> resolveMoonValue(clock, calendar);
        };
    }

    public static int getMinuteOfDay(ClockHandle clock, CalendarHandle calendar) {
        return clock.getCurrentHour() * calendar.getMinutesPerHour() + clock.getCurrentMinute();
    }

    private static int resolveSeasonValue(ClockHandle clock, CalendarHandle calendar) {

        String seasonName = clock.getCurrentSeason();

        for (int i = 0; i < calendar.getSeasons().size(); i++)
            if (calendar.getSeasons().get(i).getName().equals(seasonName))
                return i + 1;

        return getMinimum(DevTimeUnit.SEASON, calendar);
    }

    private static int resolveMoonValue(ClockHandle clock, CalendarHandle calendar) {

        int lunarCycleDays = calendar.getLunarCycleDays();
        int offsetDays = (int) Math.floor(calendar.getMoonPhaseOffset() * lunarCycleDays);

        return (int) Math.floorMod(clock.getTotalDaysWithOffset() + offsetDays, (long) lunarCycleDays) + 1;
    }

    // Format \\

    public static String formatValue(DevTimeUnit unit, int value, CalendarHandle calendar) {
        return switch (unit) {
            case MINUTE -> String.format(EditorSetting.DEV_TIME_MINUTE_FORMAT, value);
            case WEEKDAY -> calendar.getDay(value - 1);
            case MONTH -> calendar.getMonthName(value - 1);
            case SEASON -> calendar.getSeasons().get(value - 1).getName();
            case MOON -> resolveMoonPhaseName(value, calendar);
            default -> String.valueOf(value);
        };
    }

    // The named phase nearest a day of the lunar cycle, new on its first day and full halfway through
    private static String resolveMoonPhaseName(int value, CalendarHandle calendar) {

        int phaseCount = EditorSetting.DEV_TIME_MOON_PHASES.length;
        int phase = Math.round((value - 1) * phaseCount / (float) calendar.getLunarCycleDays()) % phaseCount;

        return EditorSetting.DEV_TIME_MOON_PHASES[phase];
    }

    public static String describeClock(ClockHandle clock, CalendarHandle calendar) {

        StringBuilder description = new StringBuilder()
                .append(formatValue(DevTimeUnit.HOUR, clock.getCurrentHour(), calendar))
                .append(EditorSetting.DEV_TIME_CLOCK_SEPARATOR)
                .append(formatValue(DevTimeUnit.MINUTE, clock.getCurrentMinute(), calendar))
                .append(EditorSetting.DEV_TIME_DATE_SEPARATOR)
                .append(formatValue(DevTimeUnit.WEEKDAY, clock.getCurrentDayOfWeek(), calendar))
                .append(EditorSetting.DEV_TIME_WORD_SEPARATOR)
                .append(clock.getCurrentDayOfMonth())
                .append(EditorSetting.DEV_TIME_WORD_SEPARATOR)
                .append(formatValue(DevTimeUnit.MONTH, clock.getCurrentMonth() + 1, calendar));

        if (isAvailable(DevTimeUnit.SEASON, calendar))
            description.append(EditorSetting.DEV_TIME_DATE_SEPARATOR).append(clock.getCurrentSeason());

        description.append(EditorSetting.DEV_TIME_DATE_SEPARATOR)
                .append(EditorSetting.DEV_TIME_YEAR_PREFIX)
                .append(clock.getCurrentYear())
                .append(EditorSetting.DEV_TIME_AGE_PREFIX)
                .append(clock.getCurrentAge());

        if (isAvailable(DevTimeUnit.MOON, calendar))
            description.append(EditorSetting.DEV_TIME_DATE_SEPARATOR)
                    .append(formatValue(DevTimeUnit.MOON, resolveMoonValue(clock, calendar), calendar))
                    .append(EditorSetting.DEV_TIME_MOON_SUFFIX);

        return description.toString();
    }

    // Target \\

    // The day, counted from the calendar's starting age, the clock reaches after advancing this unit
    public static long resolveTargetDay(DevTimeUnit unit, long amount, ClockHandle clock, CalendarHandle calendar) {

        long day = clock.getTotalDaysWithOffset();

        return switch (unit) {
            case HOUR, MINUTE -> Math.floorDiv(resolveTargetMinutes(unit, amount, clock, calendar),
                    resolveMinutesPerDay(calendar));
            case WEEKDAY, DAY, MOON -> day + amount;
            case MONTH -> resolveMonthTarget(amount, clock, calendar);
            case SEASON -> resolveSeasonTarget(amount, clock, calendar);
            case YEAR -> day + amount * calendar.getTotalDaysInYear();
            case AGE -> day + amount * resolveDaysPerAge(calendar);
        };
    }

    // The minute of the day the clock reaches after advancing this unit
    public static int resolveTargetMinuteOfDay(
            DevTimeUnit unit,
            long amount,
            ClockHandle clock,
            CalendarHandle calendar) {

        return switch (unit) {
            case HOUR, MINUTE -> (int) Math.floorMod(resolveTargetMinutes(unit, amount, clock, calendar),
                    (long) resolveMinutesPerDay(calendar));
            default -> getMinuteOfDay(clock, calendar);
        };
    }

    // The last day the clock may reach, the final day of the last age the editor allows
    public static long getLastDay(CalendarHandle calendar) {
        return (EditorSetting.DEV_TIME_AGE_SPAN + 1L) * resolveDaysPerAge(calendar) - 1;
    }

    // The minute of the day at which the sun stands at this raw time of day, midnight at 0 and noon at one half
    public static int resolveMinuteOfDayAtRawTime(double rawTimeOfDay, CalendarHandle calendar) {

        int minutesPerDay = resolveMinutesPerDay(calendar);
        double dayProgress = CelestialUtility.wrapFraction(
                rawTimeOfDay - EngineSetting.CLOCK_NOON + calendar.getMiddayOffset());

        return (int) (Math.round(dayProgress * minutesPerDay) % minutesPerDay);
    }

    // Minutes counted from the calendar's first day, carried through the hour and the day
    private static long resolveTargetMinutes(
            DevTimeUnit unit,
            long amount,
            ClockHandle clock,
            CalendarHandle calendar) {

        long minutesPerUnit = unit == DevTimeUnit.HOUR ? calendar.getMinutesPerHour() : 1;

        return clock.getTotalDaysWithOffset() * resolveMinutesPerDay(calendar) + getMinuteOfDay(clock, calendar)
                + amount * minutesPerUnit;
    }

    // The same day of the month that many months on, held to that month's length, carried through the year
    private static long resolveMonthTarget(long amount, ClockHandle clock, CalendarHandle calendar) {

        int monthCount = calendar.getMonthCount();
        long totalMonths = resolveYearCount(clock, calendar) * monthCount + clock.getCurrentMonth() + amount;
        int month = (int) Math.floorMod(totalMonths, (long) monthCount);
        int dayOfMonth = Math.min(clock.getCurrentDayOfMonth(), calendar.getMonthDays(month));

        return Math.floorDiv(totalMonths, monthCount) * calendar.getTotalDaysInYear()
                + calendar.getDayOfYearZeroIndexed(month, dayOfMonth);
    }

    // The first day of the season that many seasons on, counted from the year the current season began in
    private static long resolveSeasonTarget(long amount, ClockHandle clock, CalendarHandle calendar) {

        int seasonCount = calendar.getSeasons().size();
        int season = resolveSeasonValue(clock, calendar) - 1;
        int dayOfYear = calendar.getDayOfYearZeroIndexed(clock.getCurrentMonth(), clock.getCurrentDayOfMonth());
        long seasonYear = resolveYearCount(clock, calendar)
                - (resolveSeasonStart(calendar.getSeasons().get(season), calendar) > dayOfYear ? 1 : 0);
        long totalSeasons = season + amount;
        SeasonRangeStruct target = calendar.getSeasons().get((int) Math.floorMod(totalSeasons, (long) seasonCount));

        return (seasonYear + Math.floorDiv(totalSeasons, seasonCount)) * calendar.getTotalDaysInYear()
                + resolveSeasonStart(target, calendar);
    }

    // Whole years counted from the calendar's first day, across every age
    private static long resolveYearCount(ClockHandle clock, CalendarHandle calendar) {
        return Math.floorDiv(clock.getTotalDaysWithOffset(), calendar.getTotalDaysInYear());
    }

    private static long resolveDaysPerAge(CalendarHandle calendar) {
        return (long) calendar.getYearsPerAge() * calendar.getTotalDaysInYear();
    }

    private static int resolveMinutesPerDay(CalendarHandle calendar) {
        return calendar.getHoursPerDay() * calendar.getMinutesPerHour();
    }

    private static int resolveSeasonStart(SeasonRangeStruct season, CalendarHandle calendar) {
        return calendar.getDayOfYearZeroIndexed(season.getStartMonth(), season.getStartDayOfMonth());
    }

    // Units \\

    public static String listUnitNames() {

        StringBuilder unitNames = new StringBuilder();

        for (int i = 0; i < DevTimeUnit.VALUES.length; i++) {

            if (i > 0)
                unitNames.append(EditorSetting.COMMAND_MESSAGE_LIST_SEPARATOR);

            unitNames.append(DevTimeUnit.VALUES[i].getUnitName());
        }

        return unitNames.toString();
    }
}
