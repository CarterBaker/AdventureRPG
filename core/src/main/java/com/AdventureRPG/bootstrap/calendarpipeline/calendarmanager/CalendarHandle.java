package com.AdventureRPG.bootstrap.calendarpipeline.calendarmanager;

import com.AdventureRPG.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CalendarHandle extends HandlePackage {

    // Internal
    private ObjectArrayList<String> daysOfWeek;
    private ObjectArrayList<String> monthNames;
    private Object2ByteOpenHashMap<String> monthDays;

    // Calculated Data
    private int totalDaysInYear;

    // Constructor \\

    void constructor(
            ObjectArrayList<String> daysOfWeek,
            ObjectArrayList<String> monthNames,
            Object2ByteOpenHashMap<String> monthDays,
            int totalDaysInYear) {

        this.daysOfWeek = daysOfWeek;
        this.monthNames = monthNames;
        this.monthDays = monthDays;
        this.totalDaysInYear = totalDaysInYear;
    }

    // Accessible \\

    public int getDaysPerWeek() {
        return daysOfWeek.size();
    }

    public int getMonthCount() {
        return monthNames.size();
    }

    public String getDayOfWeek(int index) {
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

    public int getTotalDaysInYear() {
        return totalDaysInYear;
    }
}