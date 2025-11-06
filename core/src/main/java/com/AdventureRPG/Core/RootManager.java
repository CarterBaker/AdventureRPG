package com.AdventureRPG.Core;

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

public class RootManager extends GameManager implements Screen {

    // Root
    public final Main game;
    public final File path;
    public final Gson gson;

    // LibGDX
    public ShaderProvider defaultShaderProvider;
    public Environment environment;

    // Core
    public ThreadManager threadManager;
    public TextureManager textureManager;
    public ShaderManager shaderManager;
    public MaterialManager materialManager;
    public PassManager passManager;
    public SaveSystem saveSystem;
    public UISystem UISystem;
    public TimeSystem timeSystem;
    public LightingSystem lightingSystem;
    public PlayerSystem playerSystem;
    public WorldSystem worldSystem;
    public InputSystem inputSystem;
    public RenderManager renderManager;

    // UI
    private LoadScreen loadScreen;

    // Base \\

    public RootManager(
            Main game,
            File path,
            Gson gson) {

        // Root
        this.game = game;
        this.path = path;
        this.gson = gson;

        // LibGDX
        this.defaultShaderProvider = new DefaultShaderProvider();
        this.environment = new Environment();

        // Core
        threadManager = (ThreadManager) register(new ThreadManager());
        shaderManager = (ShaderManager) register(new ShaderManager());
        textureManager = (TextureManager) register(new TextureManager());
        materialManager = (MaterialManager) register(new MaterialManager());
        passManager = (PassManager) register(new PassManager());
        saveSystem = (SaveSystem) register(new SaveSystem());
        UISystem = (UISystem) register(new UISystem());
        timeSystem = (TimeSystem) register(new TimeSystem());
        lightingSystem = (LightingSystem) register(new LightingSystem());
        playerSystem = (PlayerSystem) register(new PlayerSystem());
        worldSystem = (WorldSystem) register(new WorldSystem());
        inputSystem = (InputSystem) register(new InputSystem());
        renderManager = (RenderManager) register(new RenderManager());
    }

    @Override
    public void init() {

        // UI
        this.loadScreen = null;
    }

    @Override
    public void awake() {
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));
    }

    @Override
    public void start() {

        startLoading();
        // UISystem.open(Menu.Main); // TODO: Commented out for debugging
    }

    public void startLoading() {

        worldSystem.loadChunks();

        loadScreen = (LoadScreen) UISystem.open(Menu.LoadScreen);
        loadScreen.setMaxProgrss(worldSystem.queueSystem.totalQueueSize());

        setGameState(GameState.MENU);
    }

    @Override
    public void menuExclusiveUpdate() {

        if (worldSystem.queueSystem.hasQueue())
            loadScreen.setProgrss(worldSystem.queueSystem.totalQueueSize());

        else {

            loadScreen.setProgrss(worldSystem.queueSystem.totalQueueSize());
            UISystem.close(loadScreen);

            setGameState(GameState.GAME);
        }
    }

    @Override
    public void render() {

        renderManager.draw(game.spriteBatch, game.modelBatch);
    }

    @Override
    public void dispose() {

        defaultShaderProvider.dispose();
    }

    // Screen \\

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {

    }

    @Override
    public void resize(int width, int height) {

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

    // Accessible \\

    public void setGameState(GameState gameState) {
        game.setGameState(gameState);
    }
}
