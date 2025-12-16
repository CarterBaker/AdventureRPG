package com.AdventureRPG.timesystem;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

// TODO: AI made this
public class Loader {

    public static Calendar load(File file, Gson gson) {

        if (file == null || !file.exists()) { // TODO: Add my own error
            throw new RuntimeException("Calendar file does not exist.");
        }

        try (Reader reader = new FileReader(file)) {

            CalendarData calendarData = gson.fromJson(reader, CalendarData.class);

            if (calendarData == null) { // TODO: Add my own error
                throw new RuntimeException("Calendar data is empty or malformed.");
            }

            return new Calendar(
                    calendarData.months,
                    calendarData.daysOfWeek);

        } catch (IOException e) { // TODO: Add my own error
            throw new RuntimeException("Failed to load calendar file.", e);
        }
    }
}
