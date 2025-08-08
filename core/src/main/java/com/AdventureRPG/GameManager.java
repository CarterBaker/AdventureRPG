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
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
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
    public final SaveSystem saveSystem;
    public final UISystem UISystem;
    public final WorldSystem worldSystem;
    public final PlayerSystem playerSystem;
    public final InputSystem inputSystem;

    // UI System
    private LoadScreen loadScreen;

    // Game Manager
    private GameState gameState;
    private final GameRenderer Renderer;
    public final Environment environment;

    // Base \\

    public GameManager(Main game, File path, Settings settings, Gson gson) {

        // Paths and Settings
        this.game = game;
        this.path = path;
        this.settings = settings;
        this.gson = gson;

        // Game Systems
        this.saveSystem = new SaveSystem(this);
        this.UISystem = new UISystem(this);
        this.worldSystem = new WorldSystem(this);
        this.playerSystem = new PlayerSystem(this);
        this.inputSystem = new InputSystem(this);

        // UI System
        this.loadScreen = null;

        // Game Manager
        this.gameState = GameState.START;
        this.Renderer = new GameRenderer(this);
        this.environment = new Environment();

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

        // On resize update the main camera
        playerSystem.camera.UpdateViewport(width, height);
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

        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

        saveSystem.Awake();
        UISystem.Awake();
        worldSystem.Awake();
        playerSystem.Awake();
        inputSystem.Awake();
    }

    // Update is called once per frame before rendering
    private void Update(float delta) {

        DeltaTime = delta;

        switch (gameState) {
            case START -> START();
            case Loading -> Loading();
            case Ready -> Ready();
        }

        worldSystem.Update();
    }

    public float DeltaTime() {
        return DeltaTime;
    }

    // Start is run the very first frame
    private void START() {

        saveSystem.Start();
        UISystem.Start();
        worldSystem.Start();
        playerSystem.Start();
        inputSystem.Start();

        StartLoading();
        // UISystem.Open(Menu.Main);
    }

    // Created seperate logic so this can be called when loading a save
    public void StartLoading() {

        worldSystem.LoadChunks();

        loadScreen = (LoadScreen) UISystem.Open(Menu.LoadScreen);
        loadScreen.SetMaxProgrss(worldSystem.chunkSystem.QueueSize());

        gameState = GameState.Loading;
    }

    // Run once per frame when the game is loading
    private void Loading() {

        if (worldSystem.chunkSystem.HasQueue())
            loadScreen.SetProgrss(worldSystem.chunkSystem.QueueSize());
        else {
            loadScreen.SetProgrss(worldSystem.chunkSystem.QueueSize());
            UISystem.Close(loadScreen);
            gameState = GameState.Ready;
        }
    }

    // Ready can be used as an exclusive Update() when the game is not loading
    private void Ready() {

        saveSystem.Update();
        UISystem.Update();
        playerSystem.Update();
        inputSystem.Update();
    }

    private enum GameState {
        START,
        Loading,
        Ready
    }
}
