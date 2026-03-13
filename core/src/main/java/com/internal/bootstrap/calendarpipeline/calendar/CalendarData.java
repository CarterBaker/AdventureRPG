package com.internal.bootstrap.calendarpipeline.calendar;

import com.internal.core.engine.DataPackage;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CalendarData extends DataPackage {

    // Internal
    public final String calendarName;
    public final ObjectArrayList<String> daysOfWeek;
    public final ObjectArrayList<String> monthNames;
    public final Object2ByteOpenHashMap<String> monthDays;

    // Calculated
    public final int totalDaysInYear;

    // Constructor \\
    public CalendarData(
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
}