package com.AdventureRPG.SettingsSystem;

import java.io.*;
import com.badlogic.gdx.utils.Json;

public class Loader {

    public static Settings load(File file) {
        Json json = new Json();
        json.setUsePrototypes(false);  // Disable prototype references
        json.setIgnoreUnknownFields(true);  // ignore unknown JSON keys

        if (!file.exists()) {
            Settings defaultSettings = new Settings();
            save(file, defaultSettings); // Save default settings
            return defaultSettings;
        }

        try (FileReader reader = new FileReader(file)) {
            return json.fromJson(Settings.class, reader);
        } catch (IOException e) {
            e.printStackTrace();
            return new Settings(); // fallback
        }
    }

    public static void save(File file, Settings settings) {
        Json json = new Json();
        json.setUsePrototypes(false);
        try (FileWriter writer = new FileWriter(file)) {
            json.toJson(settings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
