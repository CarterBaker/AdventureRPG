package com.internal.bootstrap.calendarpipeline.calendar;

import com.internal.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CalendarHandle extends HandlePackage {

    // Internal
    private String calendarName; // ← NEW — full path e.g. "standard/Overworld"
    private ObjectArrayList<String> daysOfWeek;
    private ObjectArrayList<String> monthNames;
    private Object2ByteOpenHashMap<String> monthDays;

    // Calculated Data
    private int totalDaysInYear;

    // Constructor \\

    public void constructor(
            String calendarName,
            ObjectArrayList<String> daysOfWeek,
            ObjectArrayList<String> monthNames,
            Object2ByteOpenHashMap<String> monthDays,
            int totalDaysInYear) {

        this.calendarName = calendarName;
        this.daysOfWeek = daysOfWeek;
        this.monthNames = monthNames;
        this.monthDays = monthDays;
        this.totalDaysInYear = totalDaysInYear;
    }

    // Accessible \\

    public String getCalendarName() {
        return calendarName;
    }

    public int getDaysPerWeek() {
        return daysOfWeek.size();
    }

    public int getMonthCount() {
        return monthNames.size();
    }

    public int getTotalDaysInYear() {
        return totalDaysInYear;
    }

    public String getDay(int index) {
        return daysOfWeek.get(index);
    }

    public String getMonthName(int index) {
        return monthNames.get(index);
    }

    public byte getMonthDays(int index) {
        return monthDays.getByte(monthNames.get(index));
    }

    public byte getMonthDays(String monthName) {
        return monthDays.getByte(monthName);
    }
}