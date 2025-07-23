package com.AdventureRPG;

import java.io.File;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.Vector3;

import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.SettingsSystem.*;
import com.AdventureRPG.UISystem.*;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.PlayerSystem.*;
import com.AdventureRPG.SaveSystem.SaveSystem;

public class GameManager implements Screen {

    // Delta
    private float DeltaTime;

    // Paths and Settings
    public final Main game;
    public final File path;
    public final Settings settings;

    // Game Systems
    public final SaveSystem SaveSystem;
    public final UISystem UISystem;
    public final WorldSystem WorldSystem;
    public final Player Player;
    public final InputHandler InputHandler;

    // Main
    public final GameUpdate Update;

    public GameManager(Main game, File path, Settings settings) {

        // Setup default Paths and Settings
        this.game = game;
        this.path = path;
        this.settings = settings;

        // Setup Game Systems
        this.SaveSystem = new SaveSystem(this);
        this.UISystem = new UISystem(this);
        this.WorldSystem = new WorldSystem(this);
        this.InputHandler = new InputHandler(this);
        this.Player = new Player(this);

        // Main
        this.Update = new GameUpdate(this);
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        DeltaTime = delta;
        Update.Draw(game.batch);
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

    public float DeltaTime() {
        return DeltaTime;
    }

    public void Move(Vector3Int input) {
        Player.Move(input);
        WorldSystem.Move(Player.Position());
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

    // World System

    public Vector3 WrapAroundBlock(Vector3 input) {
        return WorldSystem.WrapAroundBlock(input);
    }

    public Vector3Int WrapAroundWorld(Vector3Int input) {
        return WorldSystem.WrapAroundWorld(input);
    }

    // Player System

    public void BlockInput(boolean allowInput) {
        InputHandler.BlockInput(allowInput);
    }
}
