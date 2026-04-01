package program.bootstrap.calendarpipeline.calendar;

import program.core.engine.HandlePackage;

public class CalendarHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded calendar definition. Registered and
     * owned by CalendarManager. Delegates all accessors through CalendarData.
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
}