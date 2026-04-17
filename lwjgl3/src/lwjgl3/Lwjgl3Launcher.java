package lwjgl3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import engine.lwjgl3.Lwjgl3Application;
import engine.lwjgl3.Lwjgl3Configuration;
import engine.lwjgl3.Lwjgl3Display;
import engine.lwjgl3.Lwjgl3WindowPlatform;
import engine.root.EngineContext;
import engine.root.EnginePackage;
import engine.root.EngineSetting;
import engine.root.GameEngine;
import engine.settings.Settings;
import engine.settings.SettingsUtility;

import java.io.File;

public class Lwjgl3Launcher {

    /*
     * Entry point for the game client. Loads settings, configures the GLFW
     * window, and hands control to Lwjgl3Application.
     */

    // Identity
    private static final String GAME_DIRECTORY = "AdventureRPG";
    private static final Gson ENGINE_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    // Entry \\

    public static void main(String[] args) {
        configureAwtForEngineRasterization();
        if (StartupHelper.startNewJvmIfRequired())
            return;
        createApplication();
    }

    // Internal \\

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

        File settingsFile = new File(baseGameDir, "Settings.json");
        Settings settings = SettingsUtility.load(settingsFile, ENGINE_GSON);
        SettingsUtility.applyBindings(settings);

        Lwjgl3Configuration config = buildConfig(settings);
        Lwjgl3WindowPlatform platform = new Lwjgl3WindowPlatform();

        config.setCloseCallback(() -> {
            saveWindowInfoOnClose(settingsFile, settings);
            platform.exit();
            return true;
        });

        EnginePackage.setupConstructor(settings, baseGameDir, ENGINE_GSON, platform);
        GameEngine engine = new GameEngine();
        new Lwjgl3Application(engine, config, platform);
    }

    private static Lwjgl3Configuration buildConfig(Settings settings) {

        Lwjgl3Configuration config = new Lwjgl3Configuration();
        config.setOpenGLVersion(4, 1);
        config.setTitle("AdventureRPG");
        config.useVsync(true);

        if (settings.fullscreen) {
            config.setFullscreenMode(Lwjgl3Configuration.getDisplayMode());
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

        if (!(EngineContext.display instanceof Lwjgl3Display display))
            return;

        settings.windowWidth = Math.max(EngineSetting.MIN_WINDOW_DIMENSION, display.getWidth());
        settings.windowHeight = Math.max(EngineSetting.MIN_WINDOW_DIMENSION, display.getHeight());
        settings.windowX = display.getPosX();
        settings.windowY = display.getPosY();
        settings.fullscreen = display.isFullscreen();
        SettingsUtility.save(file, settings, ENGINE_GSON);
    }
}