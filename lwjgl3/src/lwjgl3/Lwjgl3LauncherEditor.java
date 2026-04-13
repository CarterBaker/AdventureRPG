package lwjgl3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import engine.lwjgl3.Lwjgl3Application;
import engine.lwjgl3.Lwjgl3ApplicationConfiguration;
import engine.lwjgl3.Lwjgl3Graphics;
import engine.lwjgl3.Lwjgl3Window;
import engine.lwjgl3.Lwjgl3WindowAdapter;
import engine.lwjgl3.Lwjgl3WindowPlatform;
import engine.root.EditorEngine;
import engine.root.EngineContext;
import engine.root.EnginePackage;
import engine.settings.EngineSetting;
import engine.settings.Loader;
import engine.settings.Settings;
import engine.settings.SettingsDeserializer;
import java.io.File;

public class Lwjgl3LauncherEditor {

    /*
     * Entry point for the editor. Loads editor settings, configures the GLFW
     * window at GL 4.1, and hands control to Lwjgl3Application.
     */

    // Identity
    private static final String GAME_DIRECTORY = "AdventureRPG";
    private static final Gson ENGINE_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Settings.class, new SettingsDeserializer())
            .create();

    public static void main(String[] args) {
        configureAwtForEngineRasterization();
        if (StartupHelper.startNewJvmIfRequired())
            return;
        createApplication();
    }

    private static void configureAwtForEngineRasterization() {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("sun.java2d.d3d", "false");
        System.setProperty("sun.java2d.opengl", "false");
    }

    private static void createApplication() {

        File baseGameDir = new File(System.getProperty("user.home"), "Documents/My Games/" + GAME_DIRECTORY);

        if (!baseGameDir.exists())
            baseGameDir.mkdirs();

        File settingsFile = new File(baseGameDir, "EditorSettings.json");
        Settings settings = Loader.load(settingsFile, ENGINE_GSON);

        editor.runtime.input.Bindings.load(settings);

        Lwjgl3ApplicationConfiguration config = buildConfig(settings);
        Lwjgl3WindowPlatform platform = new Lwjgl3WindowPlatform();

        config.setWindowListener(new Lwjgl3WindowAdapter() {

            @Override
            public boolean closeRequested() {
                saveWindowInfoOnClose(settingsFile, settings);
                platform.exit();
                return true;
            }
        });

        EnginePackage.setupConstructor(settings, baseGameDir, ENGINE_GSON, platform);

        EditorEngine engine = new EditorEngine();
        new Lwjgl3Application(engine, config, platform);
    }

    private static Lwjgl3ApplicationConfiguration buildConfig(Settings settings) {

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setOpenGLVersion(4, 1);
        config.setTitle("AdventureRPG — Editor");
        config.useVsync(true);

        if (settings.fullscreen) {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        } else {
            int width = Math.max(EngineSetting.MIN_WINDOW_DIMENSION, settings.windowWidth);
            int height = Math.max(EngineSetting.MIN_WINDOW_DIMENSION, settings.windowHeight);
            config.setWindowedMode(width, height);
            if (settings.windowX >= 0 && settings.windowY >= 0)
                config.setWindowPosition(settings.windowX, settings.windowY);
        }

        return config;
    }

    private static void saveWindowInfoOnClose(File file, Settings settings) {

        if (!(EngineContext.graphics instanceof Lwjgl3Graphics graphics))
            return;

        Lwjgl3Window window = graphics.getWindow();
        settings.windowWidth = Math.max(EngineSetting.MIN_WINDOW_DIMENSION, EngineContext.graphics.getWidth());
        settings.windowHeight = Math.max(EngineSetting.MIN_WINDOW_DIMENSION, EngineContext.graphics.getHeight());
        settings.windowX = window.getPositionX();
        settings.windowY = window.getPositionY();
        settings.fullscreen = EngineContext.graphics.isFullscreen();

        Loader.save(file, settings, ENGINE_GSON);
    }
}