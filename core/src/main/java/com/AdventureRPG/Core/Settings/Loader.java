package com.AdventureRPG.core.settings;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.google.gson.Gson;

public class Loader {

    public static Settings load(File file, Gson gson) {

        if (!file.exists()) {

            // Create default Settings using Builder explicitly
            Settings defaultSettings = new Settings.Builder().build();
            save(file, defaultSettings, gson); // Save default settings JSON

            return defaultSettings;
        }

        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, Settings.class);
        }

        catch (IOException e) {

            e.printStackTrace();

            // Fallback to default built settings if loading fails
            return new Settings.Builder().build();
        }
    }

    public static void save(File file, Settings settings, Gson gson) {

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(settings, writer);
        }

        catch (IOException e) {
            e.printStackTrace();
        }
    }
}