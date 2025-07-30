package com.AdventureRPG.SettingsSystem;

import java.io.*;

import com.google.gson.Gson;

public class Loader {

    public static Settings load(File file, Gson gson) {
        if (!file.exists()) {
            Settings defaultSettings = new Settings();
            save(file, defaultSettings, gson); // Save default settings
            return defaultSettings;
        }

        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, Settings.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new Settings(); // fallback
        }
    }

    public static void save(File file, Settings settings, Gson gson) {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
