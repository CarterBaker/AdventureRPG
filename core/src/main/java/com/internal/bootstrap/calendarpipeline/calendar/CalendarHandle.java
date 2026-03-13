package com.internal.bootstrap.calendarpipeline.calendar;

import com.internal.core.engine.HandlePackage;

public class CalendarHandle extends HandlePackage {
    // Internal
    private CalendarData calendarData;

    // Constructor \\
    public void constructor(CalendarData calendarData) {
        this.calendarData = calendarData;
    }

    // Accessible \\
    public CalendarData getCalendarData() {
        return calendarData;
    }

    public String getCalendarName() {
        return calendarData.calendarName;
    }

    public int getDaysPerWeek() {
        return calendarData.daysOfWeek.size();
    }

    public int getMonthCount() {
        return calendarData.monthNames.size();
    }

    public int getTotalDaysInYear() {
        return calendarData.totalDaysInYear;
    }

    public String getDay(int index) {
        return calendarData.daysOfWeek.get(index);
    }

    public String getMonthName(int index) {
        return calendarData.monthNames.get(index);
    }

    public byte getMonthDays(int index) {
        return calendarData.monthDays.getByte(calendarData.monthNames.get(index));
    }

    public byte getMonthDays(String monthName) {
        return calendarData.monthDays.getByte(monthName);
    }
}