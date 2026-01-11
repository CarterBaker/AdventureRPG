package com.AdventureRPG.bootstrap.calendarpipeline.calendarmanager;

import java.io.File;

import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.util.JsonUtility;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuildSystem extends SystemPackage {

    // Build \\

    CalendarHandle buildCalendarHandle(File file) {

        JsonObject jsonObject = JsonUtility.loadJsonObject(file);

        ObjectArrayList<String> daysOfWeek = parseDaysOfWeek(jsonObject);
        ObjectArrayList<String> monthNames = new ObjectArrayList<>();
        Object2ByteOpenHashMap<String> monthDays = parseMonths(jsonObject, monthNames);

        int totalDaysInYear = calculateTotalDaysInYear(monthDays);

        return createCalendarHandle(daysOfWeek, monthNames, monthDays, totalDaysInYear);
    }

    private CalendarHandle createCalendarHandle(
            ObjectArrayList<String> daysOfWeek,
            ObjectArrayList<String> monthNames,
            Object2ByteOpenHashMap<String> monthDays,
            int totalDaysInYear) {

        if (daysOfWeek == null || monthNames == null || monthDays == null) {
            return null;
        }

        CalendarHandle calendarHandle = create(CalendarHandle.class);
        calendarHandle.constructor(daysOfWeek, monthNames, monthDays, totalDaysInYear);

        return calendarHandle;
    }

    // Parsing \\

    private ObjectArrayList<String> parseDaysOfWeek(JsonObject jsonObject) {

        if (!jsonObject.has("daysOfWeek"))
            throwException("Calendar JSON missing 'daysOfWeek' field");

        JsonArray daysArray = jsonObject.getAsJsonArray("daysOfWeek");
        ObjectArrayList<String> daysOfWeek = new ObjectArrayList<>(daysArray.size());

        for (JsonElement element : daysArray) {
            daysOfWeek.add(element.getAsString());
        }

        return daysOfWeek;
    }

    private Object2ByteOpenHashMap<String> parseMonths(
            JsonObject jsonObject,
            ObjectArrayList<String> monthNames) {

        if (!jsonObject.has("months"))
            throwException("Calendar JSON missing 'months' field");

        JsonArray monthsArray = jsonObject.getAsJsonArray("months");
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