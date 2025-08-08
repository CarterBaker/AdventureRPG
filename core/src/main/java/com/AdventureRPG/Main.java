package com.AdventureRPG;

import java.io.File;
import com.AdventureRPG.SettingsSystem.*;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.google.gson.Gson;

public class Main extends Game {

    // Settings
    private final File GAME_DIRECTORY;
    private final Settings settings;
    private final Gson gson;

    // Rendering
    public SpriteBatch spriteBatch;
    public ModelBatch modelBatch;

    // Base \\

    public Main(File GAME_DIRECTORY, Settings settings, Gson gson) {

        // Settings
        this.GAME_DIRECTORY = GAME_DIRECTORY;
        this.settings = settings;
        this.gson = gson;
    }

    @Override
    public void create() {

        // Rendering
        spriteBatch = new SpriteBatch();
        modelBatch = new ModelBatch();

        // Game
        setScreen(new GameManager(this, GAME_DIRECTORY, settings, gson));
    }

    @Override
    public void dispose() {

        // Settings
        HandleGameWindow();
        HandleSettingsFile();

        // Game
        spriteBatch.dispose();
        modelBatch.dispose();
        super.dispose();
    }

    // Close the main window on game close
    private void HandleGameWindow() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }

    // Save the window size to the settings file
    private void HandleSettingsFile() {

        if (!settings.debug)
            return;

        File settingsFile = new File(GAME_DIRECTORY, "settings.json");

        if (settingsFile.exists()) {
            boolean deleted = settingsFile.delete();
            if (!deleted) {
                System.err.println("Failed to delete settings file: " + settingsFile.getAbsolutePath());
            }
        }
    }
}
