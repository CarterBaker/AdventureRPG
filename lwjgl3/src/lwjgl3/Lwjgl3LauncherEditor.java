package lwjgl3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import program.core.app.CoreContext;
import program.core.backends.lwjgl3.Lwjgl3Application;
import program.core.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import program.core.backends.lwjgl3.Lwjgl3Graphics;
import program.core.backends.lwjgl3.Lwjgl3Window;
import program.core.backends.lwjgl3.Lwjgl3WindowAdapter;
import program.core.engine.MainEditor;
import program.core.settings.Loader;
import program.core.settings.Settings;

import java.io.File;

public class Lwjgl3LauncherEditor {

    /*
     * Entry point for the editor. Loads editor settings, configures the GLFW window
     * at GL 4.1, and hands control to Lwjgl3Application.
     */

    // Identity
    private static final String GAME_DIRECTORY = "AdventureRPG";
    private static final Gson ENGINE_GSON = new GsonBuilder()
            .setPrettyPrinting()
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
        Lwjgl3ApplicationConfiguration config = buildConfig(settings);

        config.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public boolean closeRequested() {
                saveWindowInfoOnClose(settingsFile, settings);
                CoreContext.app.exit();
                return true;
            }
        });

        new Lwjgl3Application(new MainEditor(baseGameDir, settings, ENGINE_GSON, new Lwjgl3WindowPlatform()), config);
    }

    private static Lwjgl3ApplicationConfiguration buildConfig(Settings settings) {

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setOpenGLVersion(4, 1);
        config.setTitle("AdventureRPG — Editor");
        config.useVsync(true);

        if (settings.fullscreen) {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        } else {
            config.setWindowedMode(settings.windowWidth, settings.windowHeight);
            if (settings.windowX >= 0 && settings.windowY >= 0)
                config.setWindowPosition(settings.windowX, settings.windowY);
        }

        return config;
    }

    private static void saveWindowInfoOnClose(File file, Settings settings) {

        if (!(CoreContext.graphics instanceof Lwjgl3Graphics graphics))
            return;

        Lwjgl3Window window = graphics.getWindow();
        settings.windowWidth = CoreContext.graphics.getWidth();
        settings.windowHeight = CoreContext.graphics.getHeight();
        settings.windowX = window.getPositionX();
        settings.windowY = window.getPositionY();
        settings.fullscreen = CoreContext.graphics.isFullscreen();
        Loader.save(file, settings, ENGINE_GSON);
    }
}