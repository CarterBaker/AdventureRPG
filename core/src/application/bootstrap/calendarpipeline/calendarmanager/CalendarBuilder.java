package application.bootstrap.calendarpipeline.calendarmanager;

import java.io.File;

import application.bootstrap.calendarpipeline.calendar.CalendarData;
import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.calendar.CalendarStarStruct;
import application.bootstrap.calendarpipeline.calendar.CalendarStartStruct;
import application.bootstrap.calendarpipeline.calendar.CalendarTimeStruct;
import application.bootstrap.calendarpipeline.calendar.SeasonRangeStruct;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CalendarBuilder extends BuilderPackage {

    /*
     * Parses calendar ARPG into a CalendarData/CalendarHandle: the day-of-
     * week and month layout, this calendar's own day/year shape, its
     * starting point, the star its world orbits, and its named seasons —
     * each anchoring a name to a
     * start date and a day length. A season's climate and sky-color
     * values are resolved separately, by name, through SeasonManager.
     * Bootstrap-only.
     */

    // Build \\

    CalendarHandle build(File file, String calendarName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        ObjectArrayList<String> daysOfWeek = parseDaysOfWeek(arpg);
        ObjectArrayList<String> monthNames = new ObjectArrayList<>();
        Object2ByteOpenHashMap<String> monthDays = parseMonths(arpg, monthNames);
        int totalDaysInYear = calculateTotalDaysInYear(monthDays);

        CalendarTimeStruct time = parseTime(arpg, calendarName);
        CalendarStartStruct start = parseStart(arpg, calendarName, monthNames, monthDays, time);
        CalendarStarStruct star = parseStar(arpg, calendarName);
        ObjectArrayList<SeasonRangeStruct> seasons = parseSeasons(arpg, calendarName, monthNames, monthDays);

        CalendarData calendarData = new CalendarData(
                calendarName, daysOfWeek, monthNames, monthDays, totalDaysInYear,
                start, time, star, seasons);

        CalendarHandle calendarHandle = create(CalendarHandle.class);
        calendarHandle.constructor(calendarData);

        return calendarHandle;
    }

    // Parsing \\

    private ObjectArrayList<String> parseDaysOfWeek(ArpgObjectStruct arpg) {

        if (!arpg.has("daysOfWeek"))
            throwException("Calendar ARPG missing 'daysOfWeek' field");

        ArpgArrayStruct daysArray = arpg.getAsArray("daysOfWeek");
        ObjectArrayList<String> daysOfWeek = new ObjectArrayList<>(daysArray.size());

        for (ArpgElementStruct element : daysArray)
            daysOfWeek.add(element.getAsString());

        return daysOfWeek;
    }

    private Object2ByteOpenHashMap<String> parseMonths(
            ArpgObjectStruct arpg,
            ObjectArrayList<String> monthNames) {

        if (!arpg.has("months"))
            throwException("Calendar ARPG missing 'months' field");

        ArpgArrayStruct monthsArray = arpg.getAsArray("months");
        Object2ByteOpenHashMap<String> monthDays = new Object2ByteOpenHashMap<>(monthsArray.size());

        for (ArpgElementStruct element : monthsArray) {

            ArpgObjectStruct monthObject = element.getAsObject();
            String name = monthObject.get("name").getAsString();
            byte days = (byte) monthObject.get("days").getAsInt();

            monthNames.add(name);
            monthDays.put(name, days);
        }

        return monthDays;
    }

    private CalendarTimeStruct parseTime(ArpgObjectStruct arpg, String calendarName) {

        int daysPerDay = ArpgUtility.validateInt(arpg, "daysPerDay");
        int hoursPerDay = ArpgUtility.validateInt(arpg, "hoursPerDay");
        int minutesPerHour = ArpgUtility.validateInt(arpg, "minutesPerHour");
        int lunarCycleDays = ArpgUtility.validateInt(arpg, "lunarCycleDays");
        float middayOffset = ArpgUtility.validateFloat(arpg, "middayOffset");
        int yearsPerAge = ArpgUtility.validateInt(arpg, "yearsPerAge");

        validateTime(calendarName, daysPerDay, hoursPerDay, minutesPerHour, lunarCycleDays, middayOffset,
                yearsPerAge);

        return new CalendarTimeStruct(daysPerDay, hoursPerDay, minutesPerHour, lunarCycleDays, middayOffset,
                yearsPerAge);
    }

    private CalendarStartStruct parseStart(
            ArpgObjectStruct arpg,
            String calendarName,
            ObjectArrayList<String> monthNames,
            Object2ByteOpenHashMap<String> monthDays,
            CalendarTimeStruct time) {

        if (!arpg.has("start"))
            throwException("Calendar \"" + calendarName + "\" ARPG missing 'start' block");

        ArpgObjectStruct startObject = arpg.getAsObject("start");

        int year = ArpgUtility.validateInt(startObject, "year");
        int age = ArpgUtility.validateInt(startObject, "age");
        int month = ArpgUtility.validateInt(startObject, "month");
        int dayOfMonth = ArpgUtility.validateInt(startObject, "dayOfMonth");
        int hour = ArpgUtility.validateInt(startObject, "hour");
        int minute = ArpgUtility.validateInt(startObject, "minute");

        validateStartDate(calendarName, month, dayOfMonth, monthNames, monthDays);
        validateStartTime(calendarName, hour, minute, time);

        return new CalendarStartStruct(year, age, month, dayOfMonth, hour, minute);
    }

    private CalendarStarStruct parseStar(ArpgObjectStruct arpg, String calendarName) {

        ArpgObjectStruct starObject = ArpgUtility.validateObject(arpg, "star");

        float distance = ArpgUtility.validateFloat(starObject, "distance");
        float luminosity = ArpgUtility.validateFloat(starObject, "luminosity");

        if (distance <= 0f)
            throwException("Calendar \"" + calendarName + "\" star.distance " + distance +
                    " is out of range — must be greater than 0.0");

        if (luminosity <= 0f)
            throwException("Calendar \"" + calendarName + "\" star.luminosity " + luminosity +
                    " is out of range — must be greater than 0.0");

        return new CalendarStarStruct(distance, luminosity);
    }

    private ObjectArrayList<SeasonRangeStruct> parseSeasons(
            ArpgObjectStruct arpg,
            String calendarName,
            ObjectArrayList<String> monthNames,
            Object2ByteOpenHashMap<String> monthDays) {

        ArpgArrayStruct seasonsArray = ArpgUtility.validateArray(arpg, "seasons");

        if (seasonsArray.isEmpty())
            throwException("Calendar \"" + calendarName + "\" 'seasons' array must define at least one season");

        ObjectArrayList<SeasonRangeStruct> seasons = new ObjectArrayList<>(seasonsArray.size());
        int lastStartMonth = -1;
        int lastStartDay = -1;

        for (ArpgElementStruct element : seasonsArray) {

            ArpgObjectStruct seasonObject = element.getAsObject();
            String name = ArpgUtility.validateString(seasonObject, "name");
            int startMonth = ArpgUtility.validateInt(seasonObject, "startMonth");
            int startDayOfMonth = ArpgUtility.getInt(seasonObject, "startDayOfMonth", 1);
            float dayLength = ArpgUtility.validateFloat(seasonObject, "dayLength");

            if (startMonth < 0 || startMonth >= monthNames.size())
                throwException("Calendar \"" + calendarName + "\" season \"" + name + "\" startMonth " + startMonth +
                        " is out of range — must be between 0 and " + (monthNames.size() - 1));

            byte daysInStartMonth = monthDays.getByte(monthNames.get(startMonth));

            if (startDayOfMonth < 1 || startDayOfMonth > daysInStartMonth)
                throwException("Calendar \"" + calendarName + "\" season \"" + name + "\" startDayOfMonth " +
                        startDayOfMonth + " is out of range for month \"" + monthNames.get(startMonth) +
                        "\" (" + daysInStartMonth + " days)");

            if (dayLength < 0f || dayLength > 1f)
                throwException("Calendar \"" + calendarName + "\" season \"" + name + "\" dayLength " +
                        dayLength + " is out of range — must be between 0.0 and 1.0");

            boolean isAfterPrevious = startMonth > lastStartMonth
                    || (startMonth == lastStartMonth && startDayOfMonth > lastStartDay);

            if (!isAfterPrevious)
                throwException("Calendar \"" + calendarName + "\" season \"" + name +
                        "\" must start later in the year than the previous season — seasons must be listed in order");

            lastStartMonth = startMonth;
            lastStartDay = startDayOfMonth;

            seasons.add(new SeasonRangeStruct(name, startMonth, startDayOfMonth, dayLength));
        }

        return seasons;
    }

    // Validation \\

    private void validateTime(
            String calendarName,
            int daysPerDay,
            int hoursPerDay,
            int minutesPerHour,
            int lunarCycleDays,
            float middayOffset,
            int yearsPerAge) {

        validatePositive(calendarName, "daysPerDay", daysPerDay);
        validatePositive(calendarName, "hoursPerDay", hoursPerDay);
        validatePositive(calendarName, "minutesPerHour", minutesPerHour);
        validatePositive(calendarName, "yearsPerAge", yearsPerAge);

        if (lunarCycleDays < 0)
            throwException("Calendar \"" + calendarName + "\" lunarCycleDays " + lunarCycleDays +
                    " is out of range — must be 0 or greater");

        if (middayOffset < 0f || middayOffset >= 1f)
            throwException("Calendar \"" + calendarName + "\" middayOffset " + middayOffset +
                    " is out of range — must be at least 0.0 and below 1.0");
    }

    private void validatePositive(String calendarName, String field, int value) {

        if (value < 1)
            throwException("Calendar \"" + calendarName + "\" " + field + " " + value +
                    " is out of range — must be 1 or greater");
    }

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