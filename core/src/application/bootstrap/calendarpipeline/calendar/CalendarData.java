package application.bootstrap.calendarpipeline.calendar;

import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CalendarData extends DataPackage {

    /*
     * Immutable calendar definition loaded from JSON. Holds the day and month
     * layout for one named calendar, the exact point in the calendar year (and
     * calendar's own units of time) a new world starts at, the shape of this
     * calendar's day and year, and the named seasons that divide its year.
     * All of it used to be fixed engine-wide constants; a world picks its
     * calendar, and the calendar now owns every bit of this, so completely
     * alien worlds are free to define however many hours in a day, seasons
     * in a year, or months in a year they like. Owned by CalendarHandle for
     * the engine lifetime.
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
}