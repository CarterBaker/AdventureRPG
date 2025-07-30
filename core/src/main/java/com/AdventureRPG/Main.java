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

        if (getScreen() != null) {
            getScreen().dispose();
        }

        batch.dispose();
        super.dispose();
    }
}