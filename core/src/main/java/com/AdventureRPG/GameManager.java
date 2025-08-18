package com.AdventureRPG;

import java.io.File;

import com.AdventureRPG.InputSystem.InputSystem;
import com.AdventureRPG.LightingSystem.LightingSystem;
import com.AdventureRPG.TextureManager.TextureManager;
import com.AdventureRPG.MaterialManager.MaterialManager;
import com.AdventureRPG.PlayerSystem.PlayerSystem;
import com.AdventureRPG.SaveSystem.SaveSystem;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.ShaderManager.ShaderManager;
import com.AdventureRPG.ThreadSystem.ThreadManager;
import com.AdventureRPG.TimeSystem.TimeSystem;
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

    // Paths and Settings
    public final Main game;
    public final File path;
    public final Settings settings;
    public final Gson gson;

    // Game Systems
    public final ThreadManager threadManager;
    public final TextureManager TextureManager;
    public final MaterialManager materialManager;
    public final ShaderManager shaderManager;
    public final SaveSystem saveSystem;
    public final UISystem UISystem;
    public final TimeSystem timeSystem;
    public final LightingSystem lightingSystem;
    public final WorldSystem worldSystem;
    public final PlayerSystem playerSystem;
    public final InputSystem inputSystem;

    // Game Manager
    private GameState gameState;
    private final GameRenderer renderer;
    public final Environment environment;

    // UI System
    private LoadScreen loadScreen;

    // Delta
    private float deltaTime;

    // Base \\

    public GameManager(Main game, File path, Settings settings, Gson gson) {

        // Paths and Settings
        this.game = game;
        this.path = path;
        this.settings = settings;
        this.gson = gson;

        // Game Systems
        this.threadManager = new ThreadManager(this);
        this.TextureManager = new TextureManager(this);
        this.materialManager = new MaterialManager(this);
        this.shaderManager = new ShaderManager(this);
        this.saveSystem = new SaveSystem(this);
        this.UISystem = new UISystem(this);
        this.timeSystem = new TimeSystem(this);
        this.lightingSystem = new LightingSystem(this);
        this.worldSystem = new WorldSystem(this);
        this.playerSystem = new PlayerSystem(this);
        this.inputSystem = new InputSystem(this);

        // Game Manager
        this.gameState = GameState.START;
        this.renderer = new GameRenderer(this);
        this.environment = new Environment();

        // UI System
        this.loadScreen = null;

        // Awake
        awake();
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {

        // First run Update()
        update(delta);

        // Then delegate all rendering to the dedicated class
        renderer.draw(game.spriteBatch, game.modelBatch);
    }

    @Override
    public void resize(int width, int height) {

        // On resize update the main camera
        playerSystem.camera.updateViewport(width, height);
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
        threadManager.dispose();
        TextureManager.dispose();
    }

    // Game Manager \\

    // Awake is called before the first frame after all constructors finish
    private void awake() {

        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

        saveSystem.awake();
        UISystem.awake();
        timeSystem.awake();
        worldSystem.awake();
        playerSystem.awake();
        inputSystem.awake();
    }

    // Update is called once per frame before rendering
    private void update(float delta) {

        deltaTime = delta;

        switch (gameState) {
            case START -> start();
            case Loading -> loading();
            case Ready -> ready();
        }

        worldSystem.update();
    }

    public float deltaTime() {
        return deltaTime;
    }

    // Start is run the very first frame
    private void start() {

        saveSystem.start();
        UISystem.start();
        timeSystem.start();
        worldSystem.start();
        playerSystem.start();
        inputSystem.start();

        startLoading();
        // UISystem.Open(Menu.Main); TODO: re add line to final version
    }

    // Created seperate logic so this can be called when loading a save
    public void startLoading() {

        worldSystem.loadChunks();

        loadScreen = (LoadScreen) UISystem.open(Menu.LoadScreen);
        loadScreen.setMaxProgrss(worldSystem.gridSystem.totalQueueSize());

        gameState = GameState.Loading;
    }

    // Run once per frame when the game is loading
    private void loading() {

        if (worldSystem.gridSystem.hasQueue())
            loadScreen.setProgrss(worldSystem.gridSystem.totalQueueSize());
        else {
            loadScreen.setProgrss(worldSystem.gridSystem.totalQueueSize());
            UISystem.close(loadScreen);
            gameState = GameState.Ready;
        }
    }

    // Ready can be used as an exclusive Update() when the game is not loading
    private void ready() {

        saveSystem.update();
        UISystem.update();
        timeSystem.update();
        playerSystem.update();
        inputSystem.update();
    }

    private enum GameState {
        START,
        Loading,
        Ready
    }
}
