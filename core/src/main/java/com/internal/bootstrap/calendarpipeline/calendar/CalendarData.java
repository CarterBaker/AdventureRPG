package com.internal.bootstrap.calendarpipeline.calendar;

import com.internal.core.engine.DataPackage;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CalendarData extends DataPackage {

    /*
     * Immutable calendar definition loaded from JSON. Holds the day and month
     * layout for one named calendar. Owned by CalendarHandle for the engine
     * lifetime.
     */

    // Internal
    private final String calendarName;
    private final ObjectArrayList<String> daysOfWeek;
    private final ObjectArrayList<String> monthNames;
    private final Object2ByteOpenHashMap<String> monthDays;

    // Calculated
    private final int totalDaysInYear;

    // Constructor \\

    public CalendarData(
            String calendarName,
            ObjectArrayList<String> daysOfWeek,
            ObjectArrayList<String> monthNames,
            Object2ByteOpenHashMap<String> monthDays,
            int totalDaysInYear) {

        // Internal
        this.calendarName = calendarName;
        this.daysOfWeek = daysOfWeek;
        this.monthNames = monthNames;
        this.monthDays = monthDays;

        // Calculated
        this.totalDaysInYear = totalDaysInYear;
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
}