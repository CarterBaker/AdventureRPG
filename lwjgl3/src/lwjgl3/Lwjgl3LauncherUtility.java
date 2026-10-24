package lwjgl3;

import java.io.File;
import java.util.function.Function;
import java.util.function.Supplier;

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
     * Shared startup path for the game, editor and launcher entry points.
     * Resolves the game directory, opens the session log, loads settings,
     * builds the GLFW window configuration, and runs the engine the entry
     * point supplies until the application exits, handing the engine back. A
     * windowed engine saves its window placement on close; a borderless one
     * opens transparent, fixed and centred, never switches fullscreen, and
     * leaves the game's placement alone, so engines can run one after another
     * in the same JVM.
     */

    // Launch \\

    static void configureAwtForEngineRasterization() {
        for (String[] property : EngineSetting.AWT_RASTERIZATION_PROPERTIES)
            System.setProperty(property[0], property[1]);
    }

    static <T extends EnginePackage> T launch(
            String logSession,
            String settingsFileName,
            String title,
            Supplier<T> engineFactory,
            String... requiredDirectories) {
        return run(
                logSession,
                settingsFileName,
                engineFactory,
                settings -> buildConfig(settings, title),
                true,
                requiredDirectories);
    }

    static <T extends EnginePackage> T launchBorderless(
            String logSession,
            String settingsFileName,
            String title,
            int width,
            int height,
            Supplier<T> engineFactory) {
        return run(
                logSession,
                settingsFileName,
                engineFactory,
                settings -> buildBorderlessConfig(settings, title, width, height),
                false);
    }

    private static <T extends EnginePackage> T run(
            String logSession,
            String settingsFileName,
            Supplier<T> engineFactory,
            Function<Settings, Lwjgl3Configuration> configFactory,
            boolean persistPlacement,
            String... requiredDirectories) {

        File baseGameDir = resolveDirectory(
                new File(System.getProperty(EngineSetting.USER_HOME_PROPERTY)),
                EngineSetting.GAME_DOCUMENTS_SUBPATH + EngineSetting.PATH_SEPARATOR + EngineSetting.GAME_DIRECTORY);

        LogUtility.openSession(baseGameDir, logSession);

        for (String directory : requiredDirectories)
            resolveDirectory(baseGameDir, directory);

        File settingsFile = new File(baseGameDir, settingsFileName);
        Settings settings = SettingsUtility.load(settingsFile);
        SettingsUtility.applyBindings(settings);

        Lwjgl3Configuration config = configFactory.apply(settings);
        Lwjgl3WindowPlatform platform = new Lwjgl3WindowPlatform();

        config.setCloseCallback(() -> {

            if (persistPlacement)
                saveWindowInfoOnClose(settingsFile, settings);

            platform.exit();
            return true;
        });

        EnginePackage.setupConstructor(settings, settingsFile, baseGameDir, platform);

        T engine = engineFactory.get();
        new Lwjgl3Application(engine, config, platform);
        LogUtility.closeSession();

        return engine;
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

    private static Lwjgl3Configuration buildBorderlessConfig(Settings settings, String title, int width, int height) {

        Lwjgl3Configuration config = new Lwjgl3Configuration();
        config.setOpenGLVersion(EngineSetting.OPENGL_VERSION_MAJOR, EngineSetting.OPENGL_VERSION_MINOR);
        config.setTitle(title);
        config.useVsync(settings.vsync);
        config.setWindowedMode(width, height);
        config.setDecorated(false);
        config.setResizable(false);
        config.setTransparentFramebuffer(true);
        config.setDisplayModeLocked(true);

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
        SettingsUtility.save(file, settings);
    }
}
