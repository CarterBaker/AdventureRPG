package program.core.settings;

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
            Settings loaded = gson.fromJson(reader, Settings.class);

            if (loaded == null)
                return new Settings.Builder().build();

            sanitizeWindowSettings(loaded);
            return loaded;
        }

        catch (IOException e) {

            e.printStackTrace();

            // Fallback to default built settings if loading fails
            return new Settings.Builder().build();
        }
    }

    private static void sanitizeWindowSettings(Settings settings) {

        if (settings.windowWidth < EngineSetting.MIN_WINDOW_DIMENSION)
            settings.windowWidth = EngineSetting.MIN_WINDOW_DIMENSION;

        if (settings.windowHeight < EngineSetting.MIN_WINDOW_DIMENSION)
            settings.windowHeight = EngineSetting.MIN_WINDOW_DIMENSION;
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