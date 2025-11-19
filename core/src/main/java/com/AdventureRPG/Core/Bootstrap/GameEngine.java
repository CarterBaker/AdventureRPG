package com.AdventureRPG.Core.Bootstrap;

import java.io.File;

import com.AdventureRPG.Core.PhysicsPipeline.InputSystem.InputSystem;
import com.AdventureRPG.Core.PhysicsPipeline.MovementManager.MovementManager;
import com.AdventureRPG.Core.RenderPipeline.CameraSystem.CameraSystem;
import com.AdventureRPG.Core.RenderPipeline.MaterialSystem.MaterialSystem;
import com.AdventureRPG.Core.RenderPipeline.PassSystem.PassSystem;
import com.AdventureRPG.Core.RenderPipeline.RenderManager.RenderManager;
import com.AdventureRPG.Core.RenderPipeline.ShaderManager.ShaderManager;
import com.AdventureRPG.Core.RenderPipeline.TextureSystem.TextureSystem;
import com.AdventureRPG.Core.ScenePipeline.WorldEngineSystem.WorldEngineSystem;
import com.AdventureRPG.Core.ThreadPipeline.ThreadSystem;
import com.AdventureRPG.LightingSystem.LightingManager;
import com.AdventureRPG.PlayerManager.PlayerManager;
import com.AdventureRPG.SaveManager.SaveManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.TimeSystem.TimeSystem;
import com.AdventureRPG.UISystem.LoadScreen;
import com.AdventureRPG.UISystem.Menu;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.WorldManager.WorldManager;
import com.badlogic.gdx.Screen;
import com.google.gson.Gson;

public class GameEngine extends EngineFrame implements Screen {

    // Kernel
    private ThreadSystem threadSystem;
    private WorldEngineSystem worldEngineSystem;
    private CameraSystem cameraSystem;
    private InputSystem inputSystem;
    private MovementManager movementManager;
    private TextureSystem textureSystem;
    private ShaderManager shaderManager;
    private PassSystem passSystem;
    private MaterialSystem materialSystem;
    private RenderManager renderManager;

    // Core
    private SaveManager saveManager;
    private UISystem UISystem;
    private LightingManager lightingManager;
    private TimeSystem timeSystem;
    private PlayerManager playerManager;
    private WorldManager worldManager;

    // UI
    private LoadScreen loadScreen;

    // Base \\

    public GameEngine(
            Settings settings,
            Main main,
            File path,
            Gson gson) {
        super(
                settings,
                main,
                path,
                gson);
    }

    @Override
    protected void bootKernel() {

        // Kernel
        this.threadSystem = (ThreadSystem) register(new ThreadSystem());
        this.worldEngineSystem = (WorldEngineSystem) register(new WorldEngineSystem());
        this.cameraSystem = (CameraSystem) register(new CameraSystem());
        this.inputSystem = (InputSystem) register(new InputSystem());
        this.movementManager = (MovementManager) register(new MovementManager());
        this.textureSystem = (TextureSystem) register(new TextureSystem());
        this.shaderManager = (ShaderManager) register(new ShaderManager());
        this.passSystem = (PassSystem) register(new PassSystem());
        this.materialSystem = (MaterialSystem) register(new MaterialSystem());
        this.renderManager = (RenderManager) register(new RenderManager());
    }

    @Override
    protected void create() {

        // Core
        saveManager = (SaveManager) register(new SaveManager());
        UISystem = (UISystem) register(new UISystem());
        lightingManager = (LightingManager) register(new LightingManager());
        timeSystem = (TimeSystem) register(new TimeSystem());
        playerManager = (PlayerManager) register(new PlayerManager());
        worldManager = (WorldManager) register(new WorldManager());
    }

    @Override
    protected void start() {

        startLoading();
        // UISystem.open(Menu.Main); // TODO: Commented out for debugging
    }

    public void startLoading() {

        worldManager.loadChunks();

        loadScreen = (LoadScreen) UISystem.open(Menu.LoadScreen);
        loadScreen.setMaxProgrss(worldManager.queueSystem.totalQueueSize());

        requestInternalState(InternalState.MENU_EXCLUSIVE);
    }

    @Override
    protected void menuExclusiveUpdate() { // TODO: This whole thing needs a rework
        // Game should be the natural state and these things should just sort of work
        // dynamically
        // Goes with the todo in main class

        if (worldManager.queueSystem.hasQueue())
            loadScreen.setProgrss(worldManager.queueSystem.totalQueueSize());

        else {

            loadScreen.setProgrss(worldManager.queueSystem.totalQueueSize());
            UISystem.close(loadScreen);

            requestInternalState(InternalState.GAME_EXCLUSIVE);
        }
    }

    @Override
    protected void render() {
        this.renderManager.draw();
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
        cameraSystem.resize(width, height);
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
}
