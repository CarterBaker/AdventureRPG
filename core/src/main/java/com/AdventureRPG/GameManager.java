package com.AdventureRPG;

import java.io.File;

import com.AdventureRPG.InputSystem.InputSystem;
import com.AdventureRPG.LightingSystem.LightingSystem;
import com.AdventureRPG.TextureManager.TextureManager;
import com.AdventureRPG.ThreadManager.ThreadManager;
import com.AdventureRPG.MaterialManager.MaterialManager;
import com.AdventureRPG.PassManager.PassManager;
import com.AdventureRPG.PlayerSystem.PlayerSystem;
import com.AdventureRPG.RenderManager.RenderManager;
import com.AdventureRPG.SaveSystem.SaveSystem;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.ShaderManager.ShaderManager;
import com.AdventureRPG.TimeSystem.TimeSystem;
import com.AdventureRPG.UISystem.LoadScreen;
import com.AdventureRPG.UISystem.Menu;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.google.gson.Gson;

public class GameManager implements Screen {

    // Debug
    private final boolean debug = false; // TODO: Debug line

    // Paths and Settings
    public final Main game;
    public final File path;
    public final Settings settings;
    public final Gson gson;

    // Core Systems
    public final ShaderProvider defaultShaderProvider;
    public final Environment environment;

    // Game Systems
    public final ThreadManager threadManager;
    public final TextureManager textureManager;
    public final ShaderManager shaderManager;
    public final MaterialManager materialManager;
    public final PassManager passManager;
    public final SaveSystem saveSystem;
    public final UISystem UISystem;
    public final TimeSystem timeSystem;
    public final LightingSystem lightingSystem;
    public final PlayerSystem playerSystem;
    public final WorldSystem worldSystem;
    public final InputSystem inputSystem;
    public final RenderManager renderManager;

    // Game Manager
    private GameState gameState;

    // UI System
    private LoadScreen loadScreen;

    // Base \\

    public GameManager(Main game, File path, Settings settings, Gson gson) {

        // Paths and Settings
        this.game = game;
        this.path = path;
        this.settings = settings;
        this.gson = gson;

        // Core Systems
        this.defaultShaderProvider = new DefaultShaderProvider();
        this.environment = new Environment();

        // Game Systems
        this.threadManager = new ThreadManager(this);
        this.textureManager = new TextureManager(this);
        this.shaderManager = new ShaderManager(this);
        this.materialManager = new MaterialManager(this);
        this.passManager = new PassManager(this);
        this.saveSystem = new SaveSystem(this);
        this.UISystem = new UISystem(this);
        this.timeSystem = new TimeSystem(this);
        this.lightingSystem = new LightingSystem(this);
        this.playerSystem = new PlayerSystem(this);
        this.worldSystem = new WorldSystem(this);
        this.inputSystem = new InputSystem(this);
        this.renderManager = new RenderManager(this);

        // Game Manager
        this.gameState = GameState.START;

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
        renderManager.draw(game.spriteBatch, game.modelBatch, delta);
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
    public void dispose() { // TODO: Add a dispose for all main systems

        // Core Systems
        defaultShaderProvider.dispose();

        // Game Systems
        threadManager.dispose();
        textureManager.dispose();
        shaderManager.dispose();
        materialManager.dispose();
        passManager.dispose();
        saveSystem.dispose();
        UISystem.dispose();
        timeSystem.dispose();
    }

    // Game Manager \\

    // Awake is called before the first frame after all constructors finish
    private void awake() {

        // Core Systems
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

        // Game Systems
        threadManager.awake();
        textureManager.awake();
        shaderManager.awake();
        materialManager.awake();
        passManager.awake();
        saveSystem.awake();
        UISystem.awake();
        timeSystem.awake();
        lightingSystem.awake();
        playerSystem.awake();
        worldSystem.awake();
        inputSystem.awake();
    }

    // Start is run the very first frame
    private void start() {

        // Game Systems
        threadManager.start();
        textureManager.start();
        shaderManager.start();
        materialManager.start();
        passManager.start();
        saveSystem.start();
        UISystem.start();
        timeSystem.start();
        lightingSystem.start();
        playerSystem.start();
        worldSystem.start();
        inputSystem.start();

        startLoading();
        // UISystem.open(Menu.Main); // TODO: Commented out for debugging
    }

    // Update is called once per frame before rendering
    private void update(float delta) {

        switch (gameState) {
            case START -> start();
            case Loading -> loading();
            case Ready -> ready();
        }

        // Game Systems
        threadManager.update();
        saveSystem.update();
        UISystem.update();
        worldSystem.update();
    }

    // Ready can be used as an exclusive Update() when the game is not loading
    private void ready() {

        // Game Systems
        textureManager.update();
        shaderManager.update();
        materialManager.update();
        passManager.update();
        timeSystem.update();
        lightingSystem.update();
        playerSystem.update();
        inputSystem.update();
    }

    // Created seperate logic so this can be called when loading a save
    public void startLoading() {

        worldSystem.loadChunks();

        loadScreen = (LoadScreen) UISystem.open(Menu.LoadScreen);
        loadScreen.setMaxProgrss(worldSystem.queueSystem.totalQueueSize());

        gameState = GameState.Loading;
    }

    // Run once per frame when the game is loading
    private void loading() {

        if (worldSystem.queueSystem.hasQueue())
            loadScreen.setProgrss(worldSystem.queueSystem.totalQueueSize());

        else {

            loadScreen.setProgrss(worldSystem.queueSystem.totalQueueSize());
            UISystem.close(loadScreen);
            gameState = GameState.Ready;

            if (debug) // TODO: Debug line
                debug();
        }
    }

    // Utility \\

    private enum GameState {
        START,
        Loading,
        Ready
    }

    // Debug \\

    private void debug() { // TODO: Debug line

        // Print memory usage
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory(); // Total memory currently allocated to JVM
        long freeMemory = runtime.freeMemory(); // Free memory within the allocated memory
        long usedMemory = totalMemory - freeMemory; // Used memory
        long maxMemory = runtime.maxMemory(); // Maximum memory JVM can allocate

        System.out.println("Used Memory: " + (usedMemory / 1024 / 1024) + " MB");
        System.out.println("Free Memory: " + (freeMemory / 1024 / 1024) + " MB");
        System.out.println("Total Allocated Memory: " + (totalMemory / 1024 / 1024) + " MB");
        System.out.println("Max JVM Memory: " + (maxMemory / 1024 / 1024) + " MB");
    }
}
