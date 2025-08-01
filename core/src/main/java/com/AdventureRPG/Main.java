package com.AdventureRPG;

import java.io.File;

import com.AdventureRPG.SettingsSystem.*;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.google.gson.Gson;

public class Main extends Game {
    public SpriteBatch batch;

    private final File GAME_DIRECTORY;
    private final Settings settings;
    private final Gson gson;

    public Main(File GAME_DIRECTORY, Settings settings, Gson gson) {
        this.GAME_DIRECTORY = GAME_DIRECTORY;
        this.settings = settings;
        this.gson = gson;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        setScreen(new GameManager(this, GAME_DIRECTORY, settings, gson));
    }

    @Override
    public void dispose() {

        HandleGameWindow();
        HandleSettingsFile();

        batch.dispose();
        super.dispose();
    }

    private void HandleGameWindow() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }

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
