package lwjgl3;

import java.io.File;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import engine.lwjgl3.Lwjgl3Application;
import engine.lwjgl3.Lwjgl3Configuration;
import engine.lwjgl3.Lwjgl3Display;
import engine.lwjgl3.Lwjgl3WindowPlatform;
import engine.root.EngineContext;
import engine.root.EnginePackage;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.settings.Settings;
import engine.settings.SettingsUtility;
import engine.util.log.LogUtility;

class Lwjgl3LauncherUtility extends EngineUtility {

    /*
     * Shared startup path for the game and editor launchers. Resolves the
     * game directory, opens the session log, loads settings, builds the GLFW
     * window configuration, and runs the engine the launcher supplies until
     * the application exits, saving window placement on close.
     */

    // Internal
    private static final Gson ENGINE_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    // Launch \\

    static void configureAwtForEngineRasterization() {
        for (String[] property : EngineSetting.AWT_RASTERIZATION_PROPERTIES)
            System.setProperty(property[0], property[1]);
    }

    static void launch(
            String logSession,
            String settingsFileName,
            String title,
            Supplier<EnginePackage> engineFactory,
            String... requiredDirectories) {

        File baseGameDir = resolveDirectory(
                new File(System.getProperty(EngineSetting.USER_HOME_PROPERTY)),
                EngineSetting.GAME_DOCUMENTS_SUBPATH + EngineSetting.PATH_SEPARATOR + EngineSetting.GAME_DIRECTORY);

        LogUtility.openSession(baseGameDir, logSession);

        for (String directory : requiredDirectories)
            resolveDirectory(baseGameDir, directory);

        File settingsFile = new File(baseGameDir, settingsFileName);
        Settings settings = SettingsUtility.load(settingsFile, ENGINE_GSON);
        SettingsUtility.applyBindings(settings);

        Lwjgl3Configuration config = buildConfig(settings, title);
        Lwjgl3WindowPlatform platform = new Lwjgl3WindowPlatform();

        config.setCloseCallback(() -> {
            saveWindowInfoOnClose(settingsFile, settings);
            platform.exit();
            return true;
        });

        EnginePackage.setupConstructor(settings, settingsFile, baseGameDir, ENGINE_GSON, platform);
        new Lwjgl3Application(engineFactory.get(), config, platform);
        LogUtility.closeSession();
    }

    // Utility \\

    private static File resolveDirectory(File parent, String path) {

        File directory = new File(parent, path);

        if (!directory.exists())
            directory.mkdirs();

        return directory;
    }

    private static Lwjgl3Configuration buildConfig(Settings settings, String title) {

        Lwjgl3Configuration config = new Lwjgl3Configuration();
        config.setOpenGLVersion(EngineSetting.OPENGL_VERSION_MAJOR, EngineSetting.OPENGL_VERSION_MINOR);
        config.setTitle(title);
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
