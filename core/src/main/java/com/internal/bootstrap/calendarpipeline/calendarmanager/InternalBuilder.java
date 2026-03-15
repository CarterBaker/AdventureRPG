package com.internal.bootstrap.calendarpipeline.calendarmanager;

import java.io.File;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.internal.bootstrap.calendarpipeline.calendar.CalendarData;
import com.internal.bootstrap.calendarpipeline.calendar.CalendarHandle;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.JsonUtility;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses calendar JSON into a CalendarData and wraps it in a CalendarHandle.
     * Computes total days in year from parsed month data. Bootstrap-only.
     */

    // Build \\

    CalendarHandle build(File file, String calendarName) {

        JsonObject json = JsonUtility.loadJsonObject(file);
        ObjectArrayList<String> daysOfWeek = parseDaysOfWeek(json);
        ObjectArrayList<String> monthNames = new ObjectArrayList<>();
        Object2ByteOpenHashMap<String> monthDays = parseMonths(json, monthNames);
        int totalDaysInYear = calculateTotalDaysInYear(monthDays);

        CalendarData calendarData = new CalendarData(
                calendarName, daysOfWeek, monthNames, monthDays, totalDaysInYear);

        CalendarHandle calendarHandle = create(CalendarHandle.class);
        calendarHandle.constructor(calendarData);

        return calendarHandle;
    }

    // Parsing \\

    private ObjectArrayList<String> parseDaysOfWeek(JsonObject json) {

        if (!json.has("daysOfWeek"))
            throwException("Calendar JSON missing 'daysOfWeek' field");

        JsonArray daysArray = json.getAsJsonArray("daysOfWeek");
        ObjectArrayList<String> daysOfWeek = new ObjectArrayList<>(daysArray.size());

        for (JsonElement element : daysArray)
            daysOfWeek.add(element.getAsString());

        return daysOfWeek;
    }

    private Object2ByteOpenHashMap<String> parseMonths(
            JsonObject json,
            ObjectArrayList<String> monthNames) {

        if (!json.has("months"))
            throwException("Calendar JSON missing 'months' field");

        JsonArray monthsArray = json.getAsJsonArray("months");
        Object2ByteOpenHashMap<String> monthDays = new Object2ByteOpenHashMap<>(monthsArray.size());

        for (JsonElement element : monthsArray) {

            JsonObject monthObject = element.getAsJsonObject();
            String name = monthObject.get("name").getAsString();
            byte days = (byte) monthObject.get("days").getAsInt();

            monthNames.add(name);
            monthDays.put(name, days);
        }

        return monthDays;
    }

    // Calculations \\

    private int calculateTotalDaysInYear(Object2ByteOpenHashMap<String> monthDays) {

        int total = 0;

        for (byte days : monthDays.values())
            total += days;

        return total;
    }
}