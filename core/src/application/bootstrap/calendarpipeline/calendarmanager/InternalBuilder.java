package application.bootstrap.calendarpipeline.calendarmanager;

import java.io.File;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.calendarpipeline.calendar.CalendarData;
import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.calendar.CalendarStartStruct;
import application.bootstrap.calendarpipeline.calendar.CalendarTimeStruct;
import application.bootstrap.calendarpipeline.calendar.SeasonRangeStruct;
import engine.root.BuilderPackage;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses calendar JSON into a CalendarData and wraps it in a CalendarHandle.
     * Computes total days in year from parsed month data. Also parses this
     * calendar's own day/year shape (daysPerDay, hoursPerDay, minutesPerHour,
     * lunarCycleDays, middayOffset, yearsPerAge — formerly fixed engine-wide
     * constants), its own starting point (the "start" block — also formerly
     * fixed constants), and its own named seasons, all validated against the
     * parsed month layout. Bootstrap-only.
     */

    // Build \\

    CalendarHandle build(File file, String calendarName) {

        JsonObject json = JsonUtility.loadJsonObject(file);
        ObjectArrayList<String> daysOfWeek = parseDaysOfWeek(json);
        ObjectArrayList<String> monthNames = new ObjectArrayList<>();
        Object2ByteOpenHashMap<String> monthDays = parseMonths(json, monthNames);
        int totalDaysInYear = calculateTotalDaysInYear(monthDays);

        CalendarTimeStruct time = parseTime(json);
        CalendarStartStruct start = parseStart(json, calendarName, monthNames, monthDays, time);
        ObjectArrayList<SeasonRangeStruct> seasons = parseSeasons(json, calendarName, monthNames, monthDays);

        CalendarData calendarData = new CalendarData(
                calendarName, daysOfWeek, monthNames, monthDays, totalDaysInYear,
                start, time, seasons);

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

    private CalendarTimeStruct parseTime(JsonObject json) {

        int daysPerDay = JsonUtility.validateInt(json, "daysPerDay");
        int hoursPerDay = JsonUtility.validateInt(json, "hoursPerDay");
        int minutesPerHour = JsonUtility.validateInt(json, "minutesPerHour");
        int lunarCycleDays = JsonUtility.validateInt(json, "lunarCycleDays");
        float middayOffset = JsonUtility.validateFloat(json, "middayOffset");
        int yearsPerAge = JsonUtility.validateInt(json, "yearsPerAge");

        return new CalendarTimeStruct(daysPerDay, hoursPerDay, minutesPerHour, lunarCycleDays, middayOffset,
                yearsPerAge);
    }

    private CalendarStartStruct parseStart(
            JsonObject json,
            String calendarName,
            ObjectArrayList<String> monthNames,
            Object2ByteOpenHashMap<String> monthDays,
            CalendarTimeStruct time) {

        if (!json.has("start"))
            throwException("Calendar \"" + calendarName + "\" JSON missing 'start' block");

        JsonObject startObject = json.getAsJsonObject("start");

        int year = JsonUtility.validateInt(startObject, "year");
        int age = JsonUtility.validateInt(startObject, "age");
        int month = JsonUtility.validateInt(startObject, "month");
        int dayOfMonth = JsonUtility.validateInt(startObject, "dayOfMonth");
        int hour = JsonUtility.validateInt(startObject, "hour");
        int minute = JsonUtility.validateInt(startObject, "minute");

        validateStartDate(calendarName, month, dayOfMonth, monthNames, monthDays);
        validateStartTime(calendarName, hour, minute, time);

        return new CalendarStartStruct(year, age, month, dayOfMonth, hour, minute);
    }

    private ObjectArrayList<SeasonRangeStruct> parseSeasons(
            JsonObject json,
            String calendarName,
            ObjectArrayList<String> monthNames,
            Object2ByteOpenHashMap<String> monthDays) {

        JsonArray seasonsArray = JsonUtility.validateArray(json, "seasons");

        if (seasonsArray.isEmpty())
            throwException("Calendar \"" + calendarName + "\" 'seasons' array must define at least one season");

        ObjectArrayList<SeasonRangeStruct> seasons = new ObjectArrayList<>(seasonsArray.size());
        int lastStartMonth = -1;
        int lastStartDay = -1;

        for (JsonElement element : seasonsArray) {

            JsonObject seasonObject = element.getAsJsonObject();
            String name = JsonUtility.validateString(seasonObject, "name");
            int startMonth = JsonUtility.validateInt(seasonObject, "startMonth");
            int startDayOfMonth = JsonUtility.getInt(seasonObject, "startDayOfMonth", 1);

            if (startMonth < 0 || startMonth >= monthNames.size())
                throwException("Calendar \"" + calendarName + "\" season \"" + name + "\" startMonth " + startMonth +
                        " is out of range — must be between 0 and " + (monthNames.size() - 1));

            byte daysInStartMonth = monthDays.getByte(monthNames.get(startMonth));

            if (startDayOfMonth < 1 || startDayOfMonth > daysInStartMonth)
                throwException("Calendar \"" + calendarName + "\" season \"" + name + "\" startDayOfMonth " +
                        startDayOfMonth + " is out of range for month \"" + monthNames.get(startMonth) +
                        "\" (" + daysInStartMonth + " days)");

            boolean isAfterPrevious = startMonth > lastStartMonth
                    || (startMonth == lastStartMonth && startDayOfMonth > lastStartDay);

            if (!isAfterPrevious)
                throwException("Calendar \"" + calendarName + "\" season \"" + name +
                        "\" must start later in the year than the previous season — seasons must be listed in order");

            lastStartMonth = startMonth;
            lastStartDay = startDayOfMonth;

            seasons.add(new SeasonRangeStruct(name, startMonth, startDayOfMonth));
        }

        return seasons;
    }

    // Validation \\

    private void validateStartDate(
            String calendarName,
            int month,
            int dayOfMonth,
            ObjectArrayList<String> monthNames,
            Object2ByteOpenHashMap<String> monthDays) {

        int monthCount = monthNames.size();

        if (month < 0 || month >= monthCount)
            throwException("Calendar \"" + calendarName + "\" start.month " + month +
                    " is out of range — must be between 0 and " + (monthCount - 1));

        byte daysInStartMonth = monthDays.getByte(monthNames.get(month));

        if (dayOfMonth < 1 || dayOfMonth > daysInStartMonth)
            throwException("Calendar \"" + calendarName + "\" start.dayOfMonth " + dayOfMonth +
                    " is out of range for month \"" + monthNames.get(month) +
                    "\" (" + daysInStartMonth + " days)");
    }

    private void validateStartTime(String calendarName, int hour, int minute, CalendarTimeStruct time) {

        if (hour < 0 || hour >= time.getHoursPerDay())
            throwException("Calendar \"" + calendarName + "\" start.hour " + hour +
                    " is out of range — must be between 0 and " + (time.getHoursPerDay() - 1));

        if (minute < 0 || minute >= time.getMinutesPerHour())
            throwException("Calendar \"" + calendarName + "\" start.minute " + minute +
                    " is out of range — must be between 0 and " + (time.getMinutesPerHour() - 1));
    }

    // Calculations \\

    private int calculateTotalDaysInYear(Object2ByteOpenHashMap<String> monthDays) {

        int total = 0;

        for (byte days : monthDays.values())
            total += days;

        return total;
    }
}