package engine.settings;

import com.google.gson.Gson;
import java.io.*;

public class LoadUtility {

    public static Settings load(File file, Gson gson) {
        if (!file.exists()) {
            Settings defaults = new Settings();
            save(file, defaults, gson);
            return defaults;
        }
        try (Reader reader = new FileReader(file)) {
            Settings loaded = gson.fromJson(reader, Settings.class);
            if (loaded == null)
                return new Settings();
            sanitize(loaded);
            return loaded;
        } catch (IOException e) {
            e.printStackTrace();
            return new Settings();
        }
    }

    public static void save(File file, Settings settings, Gson gson) {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sanitize(Settings settings) {
        if (settings.windowWidth < EngineSetting.MIN_WINDOW_DIMENSION)
            settings.windowWidth = EngineSetting.MIN_WINDOW_DIMENSION;
        if (settings.windowHeight < EngineSetting.MIN_WINDOW_DIMENSION)
            settings.windowHeight = EngineSetting.MIN_WINDOW_DIMENSION;
    }
}