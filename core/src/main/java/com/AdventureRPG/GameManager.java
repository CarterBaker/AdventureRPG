package com.AdventureRPG;

import java.io.File;

import com.badlogic.gdx.Screen;

import com.AdventureRPG.SettingsSystem.*;
import com.AdventureRPG.UISystem.*;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.PlayerSystem.InputHandler;

public class GameManager implements Screen {

    // Paths and Settings
    public final Main game;
    public final File path;
    public final Settings settings;

    // Game Systems
    public final UISystem UISystem;
    public final WorldSystem WorldSystem;
    public final InputHandler InputHandler;

    // Main
    public final GameRenderer Renderer;

    public GameManager(Main game, File path, Settings settings) {

        // Setup default Paths and Settings
        this.game = game;
        this.path = path;
        this.settings = settings;

        // Setup Game Systems
        UISystem = new UISystem(this);
        WorldSystem = new WorldSystem(this, settings);
        InputHandler = new InputHandler(this);

        // Main
        Renderer = new GameRenderer(this);
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        Renderer.Draw(game.batch);
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
    }

    // References \\

    // UISystem

    public void Open(Menu Menu) {
        UISystem.Open(Menu);
    }

    public void Close(Menu Menu) {
        UISystem.Close(Menu);
    }

    public void Close(MenuType Menu) {
        UISystem.Close(Menu);
    }

    // Player System

    public void BlockInput(boolean allowInput) {
        InputHandler.BlockInput(allowInput);
    }
}
