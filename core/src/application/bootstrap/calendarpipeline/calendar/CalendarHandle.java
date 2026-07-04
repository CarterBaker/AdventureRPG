package application.bootstrap.calendarpipeline.calendar;

import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CalendarHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded calendar definition. Registered and
     * owned by CalendarManager. Delegates all accessors through CalendarData,
     * including the calendar's starting point, its own day/year shape, and
     * its own named seasons.
     */

    // Internal
    private CalendarData calendarData;

    // Constructor \\

    public void constructor(CalendarData calendarData) {

        // Internal
        this.calendarData = calendarData;
    }

    // Accessible \\

    public CalendarData getCalendarData() {
        return calendarData;
    }

    public String getCalendarName() {
        return calendarData.getCalendarName();
    }

    public int getDaysPerWeek() {
        return calendarData.getDaysOfWeek().size();
    }

    public int getMonthCount() {
        return calendarData.getMonthNames().size();
    }

    public int getTotalDaysInYear() {
        return calendarData.getTotalDaysInYear();
    }

    public String getDay(int index) {
        return calendarData.getDaysOfWeek().get(index);
    }

    public String getMonthName(int index) {
        return calendarData.getMonthNames().get(index);
    }

    public byte getMonthDays(int index) {
        return calendarData.getMonthDays().getByte(calendarData.getMonthNames().get(index));
    }

    public byte getMonthDays(String monthName) {
        return calendarData.getMonthDays().getByte(monthName);
    }

    // Starting Point \\

    public int getStartYear() {
        return calendarData.getStart().getYear();
    }

    public int getStartAge() {
        return calendarData.getStart().getAge();
    }

    public int getStartMonth() {
        return calendarData.getStart().getMonth();
    }

    public int getStartDayOfMonth() {
        return calendarData.getStart().getDayOfMonth();
    }

    public int getStartHour() {
        return calendarData.getStart().getHour();
    }

    public int getStartMinute() {
        return calendarData.getStart().getMinute();
    }

    // Time Structure \\

    public int getDaysPerDay() {
        return calendarData.getTime().getDaysPerDay();
    }

    public int getHoursPerDay() {
        return calendarData.getTime().getHoursPerDay();
    }

    public int getMinutesPerHour() {
        return calendarData.getTime().getMinutesPerHour();
    }

    public int getLunarCycleDays() {
        return calendarData.getTime().getLunarCycleDays();
    }

    public float getMiddayOffset() {
        return calendarData.getTime().getMiddayOffset();
    }

    public int getYearsPerAge() {
        return calendarData.getTime().getYearsPerAge();
    }

    // Seasons \\

    public ObjectArrayList<SeasonRangeStruct> getSeasons() {
        return calendarData.getSeasons();
    }

    public String getSeasonNameForDate(int monthIndex, int dayOfMonth) {
        return calendarData.getSeasonNameForDate(monthIndex, dayOfMonth);
    }
}