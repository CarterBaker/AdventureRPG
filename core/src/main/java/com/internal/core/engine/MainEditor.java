package com.internal.core.engine;

import java.io.File;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;
import com.internal.core.engine.settings.Settings;

public class MainEditor extends Game {

    /*
     * MainEditor serves as the editor application entry point.
     * Mirrors Main but routes execution through EditorEngine instead
     * of GameEngine, allowing the editor to share the same codebase
     * while running an entirely independent pipeline flow.
     */

    // Root
    private final File GAME_DIRECTORY;
    private final Settings settings;
    private final Gson gson;

    // Internal
    private EditorEngine internal;

    // Base \\

    public MainEditor(
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
        EnginePackage.setupConstructor(
                settings,
                this,
                GAME_DIRECTORY,
                gson);

        this.internal = new EditorEngine();

        EnginePackage.ENGINE_STRUCT.remove();

        internal.execute();
    }

    @Override
    public void render() {
        setDeltaTime();
        super.render();
        internal.execute();
    }

    @Override
    public void dispose() {

        // Internal Engine
        internal.setInternalState(EngineState.EXIT);

        // Settings
        handleGameWindow();
        handleSettingsFile();

        // Game
        super.dispose();
    }

    // Utility \\

    private void setDeltaTime() {
        internal.setDeltaTime(Gdx.graphics.getDeltaTime());
    }

    private void handleGameWindow() {
        if (getScreen() != null)
            getScreen().dispose();
    }

    private void handleSettingsFile() {

        if (!settings.debug)
            return;

        File settingsFile = new File(GAME_DIRECTORY, "settings.json");

        if (settingsFile.exists()) {

            boolean deleted = settingsFile.delete();

            if (!deleted)
                UtilityPackage.throwException("File: " + settingsFile.getName() + ", Failed to delete on editor close");
        }
    }
}