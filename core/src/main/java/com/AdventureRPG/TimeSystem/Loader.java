package com.AdventureRPG.TimeSystem;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class Loader {

    public static Calendar load(File file, Gson gson) {
        if (!file.exists()) {
            throw new RuntimeException("Calendar file not found: " + file.getAbsolutePath());
        }

        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, Calendar.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load calendar from: " + file.getAbsolutePath(), e);
        }
    }
}
