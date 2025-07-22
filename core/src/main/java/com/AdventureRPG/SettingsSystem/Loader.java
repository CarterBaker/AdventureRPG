package com.AdventureRPG.SettingsSystem;

import java.io.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Loader {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static Settings load(File file) {
        if (!file.exists()) {
            Settings defaultSettings = new Settings();
            save(file, defaultSettings); // Save default settings
            return defaultSettings;
        }

        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, Settings.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new Settings(); // fallback
        }
    }

    public static void save(File file, Settings settings) {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
