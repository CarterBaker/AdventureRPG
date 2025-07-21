package com.AdventureRPG;

import java.io.File;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.AdventureRPG.PlayerSystem.InputHandler;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.UISystem.UISystem;
import com.badlogic.gdx.Gdx;

public class GameManager implements Screen {

    // Paths and Settings
    public final Main game;
    public final File path;
    public final Settings settings;

    // Game Systems
    public final UISystem UISystem;
    public final InputHandler InputHandler;

    public GameManager(Main game, File path, Settings settings) {

        // Setup default Paths and Settings
        this.game = game;
        this.path = path;
        this.settings = settings;

        // Setup Game Systems
        UISystem = new UISystem(this);
        InputHandler = new InputHandler(this);
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.batch.begin();
        // Draw stuff here
        game.batch.end();
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
