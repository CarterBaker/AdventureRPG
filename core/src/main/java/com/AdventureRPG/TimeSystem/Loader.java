package com.AdventureRPG.timesystem;

import com.AdventureRPG.core.util.Exceptions.FileException;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class Loader {

    public static Calendar load(File file, Gson gson) {

        if (!file.exists())
            throw new FileException.FileNotFoundException(file);

        try (Reader reader = new FileReader(file)) {
            CalendarData dto = gson.fromJson(reader, CalendarData.class);
            return new Calendar(dto.months, dto.daysOfWeek);
        }

        catch (IOException e) {
            throw new FileException.FileLoadException(file, e);
        }
    }
}
