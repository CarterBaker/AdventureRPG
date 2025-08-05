package com.AdventureRPG;

import java.io.File;

import com.AdventureRPG.InputSystem.InputSystem;
import com.AdventureRPG.PlayerSystem.PlayerSystem;
import com.AdventureRPG.SaveSystem.SaveSystem;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.UISystem.LoadScreen;
import com.AdventureRPG.UISystem.Menu;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.Screen;
import com.google.gson.Gson;

public class GameManager implements Screen {

    // Delta
    private float DeltaTime;

    // Paths and Settings
    public final Main game;
    public final File path;
    public final Settings settings;
    public final Gson gson;

    // Game Systems
    public final SaveSystem SaveSystem;
    public final UISystem UISystem;
    public final WorldSystem WorldSystem;
    public final PlayerSystem PlayerSystem;
    public final InputSystem InputSystem;

    // Main
    private GameState gameState;
    private final GameRenderer Renderer;

    // Temp
    private LoadScreen LoadScreen;

    // Base \\

    public GameManager(Main game, File path, Settings settings, Gson gson) {

        // Setup default Paths and Settings
        this.game = game;
        this.path = path;
        this.settings = settings;
        this.gson = gson;

        // Setup Game Systems
        this.SaveSystem = new SaveSystem(this);
        this.UISystem = new UISystem(this);
        this.WorldSystem = new WorldSystem(this);
        this.PlayerSystem = new PlayerSystem(this);
        this.InputSystem = new InputSystem(this);

        // Main
        this.gameState = GameState.START;
        this.Renderer = new GameRenderer(this);

        // Temp
        this.LoadScreen = null;

        // Awake
        Awake();
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {

        // First run Update()
        Update(delta);

        // Then delegate all rendering to the dedicated class
        Renderer.Draw(game.spriteBatch, game.modelBatch);
    }

    @Override
    public void resize(int width, int height) {
        PlayerSystem.camera.updateViewport(width, height);
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

    // Game Manager \\

    // Awake is called before the first frame after all constructors finish
    private void Awake() {
        PlayerSystem.Awake();
    }

    // Update is called once per frame before rendering
    private void Update(float delta) {

        DeltaTime = delta;

        switch (gameState) {
            case START -> START();
            case Loading -> Loading();
            case Ready -> Ready();
        }

        WorldSystem.Update();
    }

    public float DeltaTime() {
        return DeltaTime;
    }

    private enum GameState {
        START,
        Loading,
        Ready
    }

    // Start is run the very first frame
    private void START() {

        PlayerSystem.Start();
        InputSystem.Start();

        StartLoading();
        // UISystem.Open(Menu.Main);
    }

    // Created seperate logic so this can be called when loading a save
    public void StartLoading() {

        WorldSystem.LoadChunks();

        LoadScreen = (LoadScreen) UISystem.Open(Menu.LoadScreen);
        LoadScreen.SetMaxProgrss(WorldSystem.ChunkSystem.QueueSize());

        gameState = GameState.Loading;
    }

    // Run once per frame when the game is loading
    private void Loading() {

        if (WorldSystem.ChunkSystem.HasQueue())
            LoadScreen.SetProgrss(WorldSystem.ChunkSystem.QueueSize());
        else {
            LoadScreen.SetProgrss(WorldSystem.ChunkSystem.QueueSize());
            UISystem.Close(LoadScreen);
            gameState = GameState.Ready;
        }
    }

    // Ready can be used as an exclusive Update() when the game is not loading
    private void Ready() {
        PlayerSystem.Update();
        InputSystem.Update();
    }
}
