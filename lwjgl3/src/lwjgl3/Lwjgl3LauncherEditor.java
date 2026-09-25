package lwjgl3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import engine.lwjgl3.Lwjgl3Application;
import engine.lwjgl3.Lwjgl3Configuration;
import engine.lwjgl3.Lwjgl3Display;
import engine.lwjgl3.Lwjgl3WindowPlatform;
import engine.root.EditorEngine;
import engine.root.EngineContext;
import engine.root.EnginePackage;
import engine.root.EngineSetting;
import engine.settings.Settings;
import engine.settings.SettingsUtility;
import engine.util.log.LogUtility;

import java.io.File;

public class Lwjgl3LauncherEditor {

    /*
     * Entry point for the editor. Opens the session log, loads editor
     * settings, configures the GLFW window at GL 4.1, and hands control to
     * Lwjgl3Application. Output goes to the session log, readable in the
     * editor's Console tab and written to the log directory on any error.
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
        File baseGameDir = new File(
                System.getProperty("user.home"),
                EngineSetting.GAME_DOCUMENTS_SUBPATH + "/" + EngineSetting.GAME_DIRECTORY);
        if (!baseGameDir.exists())
            baseGameDir.mkdirs();

        LogUtility.openSession(baseGameDir, EngineSetting.LOG_SESSION_EDITOR);

        File editorLayoutDir = new File(
                baseGameDir,
                EngineSetting.BIN_DIRECTORY + "/" + EngineSetting.EDITOR_LAYOUT_DIRECTORY);
        if (!editorLayoutDir.exists())
            editorLayoutDir.mkdirs();

        File settingsFile = new File(baseGameDir, EngineSetting.EDITOR_SETTINGS_FILE_NAME);
        Settings settings = SettingsUtility.load(settingsFile, ENGINE_GSON);
        SettingsUtility.applyBindings(settings);

        Lwjgl3Configuration config = buildConfig(settings);
        Lwjgl3WindowPlatform platform = new Lwjgl3WindowPlatform();

        config.setCloseCallback(() -> {
            saveWindowInfoOnClose(settingsFile, settings);
            platform.exit();
            return true;
        });

        EnginePackage.setupConstructor(settings, settingsFile, baseGameDir, ENGINE_GSON, platform);
        EditorEngine engine = new EditorEngine();
        new Lwjgl3Application(engine, config, platform);
        LogUtility.closeSession();
    }

    private static Lwjgl3Configuration buildConfig(Settings settings) {

        Lwjgl3Configuration config = new Lwjgl3Configuration();
        config.setOpenGLVersion(4, 1);
        config.setTitle("AdventureRPG — Editor");
        config.useVsync(settings.vsync);

        int width = Math.max(EngineSetting.MIN_WINDOW_DIMENSION, settings.windowWidth);
        int height = Math.max(EngineSetting.MIN_WINDOW_DIMENSION, settings.windowHeight);
        config.setWindowedMode(width, height);
        config.setWindowPosition(settings.windowX, settings.windowY);
        config.setMaximized(settings.windowMaximized);

        if (settings.fullscreen)
            config.setFullscreenMode(Lwjgl3Configuration.getDisplayMode());

        return config;
    }

    private static void saveWindowInfoOnClose(File file, Settings settings) {

        if (!(EngineContext.display instanceof Lwjgl3Display display))
            return;

        settings.fullscreen = display.isFullscreen();
        settings.windowWidth = Math.max(EngineSetting.MIN_WINDOW_DIMENSION, display.getWindowWidth());
        settings.windowHeight = Math.max(EngineSetting.MIN_WINDOW_DIMENSION, display.getWindowHeight());
        settings.windowX = display.getPosX();
        settings.windowY = display.getPosY();
        settings.windowMaximized = display.isMaximized();

        SettingsUtility.flushBindings(settings);
        SettingsUtility.save(file, settings, ENGINE_GSON);
    }
}