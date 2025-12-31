package com.AdventureRPG.core.engine;

import java.io.File;

import com.AdventureRPG.core.engine.settings.Settings;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;

public class Main extends Game {

    /*
     * Main serves as the bridge between libGDX and the internal engine.
     * While libGDX provides cross-platform support, the internal engine
     * handles core systems such as lifecycle management, updates, and rendering.
     * This class ensures that libGDX delegates execution to the engine while
     * keeping the two systems decoupled.
     */

    // Root
    private final File GAME_DIRECTORY;
    private final Settings settings;
    private final Gson gson;

    // Internal
    private GameEngine internal;

    // Base \\

    public Main(
            File GAME_DIRECTORY,
            Settings settings,
            Gson gson) {

        // Root
        this.GAME_DIRECTORY = GAME_DIRECTORY;
        this.settings = settings;
        this.gson = gson;
    }

    @Override
    public void create() {

        // Internal
        this.internal = new GameEngine(
                settings,
                this,
                GAME_DIRECTORY,
                gson);

        internal.updateInternalState();
    }

    @Override
    public void render() {

        setDeltaTime();

        super.render();

        internal.updateInternalState();
    }

    @Override
    public void dispose() {

        // Internal Engine
        internal.setInternalState(InternalState.EXIT);

        // Settings
        HandleGameWindow();
        HandleSettingsFile();

        // Game
        super.dispose();
    }

    // Utility \\

    // Set the games delta time for ease of access across all systems
    private void setDeltaTime() {
        internal.setDeltaTime(Gdx.graphics.getDeltaTime());
    }

    // Close the main window on game close
    private void HandleGameWindow() {
        if (getScreen() != null)
            getScreen().dispose();
    }

    // Save the window size to the settings file
    private void HandleSettingsFile() {

        if (!settings.debug)
            return;

        File settingsFile = new File(GAME_DIRECTORY, "settings.json");

        if (settingsFile.exists()) {

            boolean deleted = settingsFile.delete();

            if (!deleted)
                throw new RuntimeException(
                        "File: " + settingsFile.getName() + ", Failed to delete on game close");
        }
    }
}