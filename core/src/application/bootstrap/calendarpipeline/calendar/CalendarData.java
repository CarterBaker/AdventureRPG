package application.bootstrap.calendarpipeline.calendar;

import java.util.Arrays;

import engine.root.DataPackage;
import engine.util.mathematics.extras.SeasonBlendUtility;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CalendarData extends DataPackage {

    /*
     * Immutable calendar definition loaded from JSON. Holds the day and
     * month layout for one named calendar, the exact starting point in the
     * calendar's own units of time, the shape of its day and year, and the
     * named seasons that divide its year. Owned by CalendarHandle for the
     * engine lifetime.
     */

    // Internal
    private final String calendarName;
    private final ObjectArrayList<String> daysOfWeek;
    private final ObjectArrayList<String> monthNames;
    private final Object2ByteOpenHashMap<String> monthDays;

    // Calculated
    private final int totalDaysInYear;

    // Starting Point
    private final CalendarStartStruct start;

    // Time Structure
    private final CalendarTimeStruct time;

    // Seasons
    private final ObjectArrayList<SeasonRangeStruct> seasons;
    private SeasonKeyframeStruct seasonKeyframes;

    // Constructor \\

    public CalendarData(
            String calendarName,
            ObjectArrayList<String> daysOfWeek,
            ObjectArrayList<String> monthNames,
            Object2ByteOpenHashMap<String> monthDays,
            int totalDaysInYear,
            CalendarStartStruct start,
            CalendarTimeStruct time,
            ObjectArrayList<SeasonRangeStruct> seasons) {

        // Internal
        this.calendarName = calendarName;
        this.daysOfWeek = daysOfWeek;
        this.monthNames = monthNames;
        this.monthDays = monthDays;

        // Calculated
        this.totalDaysInYear = totalDaysInYear;

        // Starting Point
        this.start = start;

        // Time Structure
        this.time = time;

        // Seasons
        this.seasons = seasons;
    }

    // Accessible \\

    public String getCalendarName() {
        return calendarName;
    }

    public ObjectArrayList<String> getDaysOfWeek() {
        return daysOfWeek;
    }

    public ObjectArrayList<String> getMonthNames() {
        return monthNames;
    }

    public Object2ByteOpenHashMap<String> getMonthDays() {
        return monthDays;
    }

    public int getTotalDaysInYear() {
        return totalDaysInYear;
    }

    public CalendarStartStruct getStart() {
        return start;
    }

    public CalendarTimeStruct getTime() {
        return time;
    }

    public ObjectArrayList<SeasonRangeStruct> getSeasons() {
        return seasons;
    }

    // Season Resolution \\

    /*
     * Resolves the name of whichever season owns the given month/day. A
     * season runs from its own (startMonth, startDayOfMonth) up to — but
     * not including — the next season's start date; the last season in
     * the list wraps around and also covers any date before the first
     * season's start date. Returns null only if this calendar defines no
     * seasons.
     */
    public String getSeasonNameForDate(int monthIndex, int dayOfMonth) {

        if (seasons.isEmpty())
            return null;

        String result = seasons.get(seasons.size() - 1).getName();

        for (int i = 0; i < seasons.size(); i++) {

            SeasonRangeStruct entry = seasons.get(i);

            if (isAtOrAfterStart(monthIndex, dayOfMonth, entry))
                result = entry.getName();
            else
                break;
        }

        return result;
    }

    private boolean isAtOrAfterStart(int monthIndex, int dayOfMonth, SeasonRangeStruct entry) {

        if (monthIndex != entry.getStartMonth())
            return monthIndex > entry.getStartMonth();

        return dayOfMonth >= entry.getStartDayOfMonth();
    }

    // Season Keyframes \\

    /*
     * Lazily builds and caches this calendar's seasons as sorted, wrapped
     * keyframe centers — shared by every system that blends a value across
     * the season year (day length, sky color, or anything else), so the
     * center/sort math exists in exactly one place.
     */
    public SeasonKeyframeStruct getSeasonKeyframes() {

        if (seasonKeyframes != null)
            return seasonKeyframes;

        int count = seasons.size();
        double[] startFractions = new double[count];

        for (int i = 0; i < count; i++) {
            SeasonRangeStruct season = seasons.get(i);
            int dayOfYear = dayOfYearZeroIndexed(season.getStartMonth(), season.getStartDayOfMonth());
            startFractions[i] = dayOfYear / (double) totalDaysInYear;
        }

        double[] rawCenters = new double[count];

        for (int i = 0; i < count; i++) {
            double start = startFractions[i];
            double end = (i + 1 < count) ? startFractions[i + 1] : startFractions[0] + 1.0;
            rawCenters[i] = SeasonBlendUtility.wrapFraction((start + end) * 0.5);
        }

        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++)
            order[i] = i;

        Arrays.sort(order, (a, b) -> Double.compare(rawCenters[a], rawCenters[b]));

        double[] sortedCenters = new double[count];
        int[] sortedOrder = new int[count];

        for (int i = 0; i < count; i++) {
            sortedOrder[i] = order[i];
            sortedCenters[i] = rawCenters[order[i]];
        }

        this.seasonKeyframes = new SeasonKeyframeStruct(sortedCenters, sortedOrder, count);
        return seasonKeyframes;
    }

    private int dayOfYearZeroIndexed(int month, int dayOfMonth) {

        int dayOfYear = 0;

        for (int i = 0; i < month; i++)
            dayOfYear += monthDays.getByte(monthNames.get(i));

        dayOfYear += dayOfMonth - 1;

        return dayOfYear;
    }
}