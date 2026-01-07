package com.AdventureRPG.lwjgl3;

import java.io.File;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.AdventureRPG.core.engine.Main;
import com.AdventureRPG.core.engine.settings.*;

public class Lwjgl3Launcher {

    private static final String GAME_DIRECTORY = "AdventureRPG";

    private static final Gson ENGINE_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired())
            return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        File baseGameDir = new File(System.getProperty("user.home"), "Documents/My Games/" + GAME_DIRECTORY);
        if (!baseGameDir.exists())
            baseGameDir.mkdirs();

        File settingsFile = new File(baseGameDir, "settings.json");
        Settings settings = Loader.load(settingsFile, ENGINE_GSON);

        Main mainGame = new Main(baseGameDir, settings, ENGINE_GSON);
        Lwjgl3ApplicationConfiguration config = getConfigurationFromSettings(settings);

        config.setWindowListener(new com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter() {
            @Override
            public boolean closeRequested() {
                saveWindowInfoOnClose(settingsFile, settings);
                return true;
            }

        });

        return new Lwjgl3Application(mainGame, config);
    }

    private static Lwjgl3ApplicationConfiguration getConfigurationFromSettings(Settings settings) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);

        config.setTitle("AdventureRPG");

        if (settings.fullscreen) {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        } else {
            config.setWindowedMode(settings.windowWidth, settings.windowHeight);

            if (settings.windowX >= 0 && settings.windowY >= 0) {
                config.setWindowPosition(settings.windowX, settings.windowY);
            }
        }

        config.useVsync(true);
        config.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);

        return config;
    }

    private static void saveWindowInfoOnClose(File file, Settings settings) {
        if (Gdx.graphics instanceof Lwjgl3Graphics) {
            Lwjgl3Window window = ((Lwjgl3Graphics) Gdx.graphics).getWindow();
            settings.windowWidth = Gdx.graphics.getWidth();
            settings.windowHeight = Gdx.graphics.getHeight();
            settings.windowX = window.getPositionX();
            settings.windowY = window.getPositionY();
            settings.fullscreen = Gdx.graphics.isFullscreen();
            Loader.save(file, settings, ENGINE_GSON);
        }
    }
}
