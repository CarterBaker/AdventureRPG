package com.AdventureRPG.core.kernel;

import java.io.File;

import com.AdventureRPG.core.geometrypipeline.GeometryPipeline;
import com.AdventureRPG.core.physicspipeline.input.InputSystem;
import com.AdventureRPG.core.physicspipeline.movement.MovementManager;
import com.AdventureRPG.core.renderpipeline.RenderPipeline;
import com.AdventureRPG.core.renderpipeline.camerasystem.CameraSystem;
import com.AdventureRPG.core.scenepipeline.worldenginesystem.WorldEngineSystem;
import com.AdventureRPG.core.settings.Settings;
import com.AdventureRPG.core.shaderpipeline.ShaderPipeline;
import com.AdventureRPG.core.threadpipeline.ThreadSystem;
import com.AdventureRPG.lightingsystem.LightingManager;
import com.AdventureRPG.playermanager.PlayerManager;
import com.AdventureRPG.savemanager.SaveManager;
import com.AdventureRPG.timesystem.TimeSystem;
import com.AdventureRPG.uisystem.LoadScreen;
import com.AdventureRPG.uisystem.Menu;
import com.AdventureRPG.uisystem.UISystem;
import com.AdventureRPG.worldmanager.WorldManager;
import com.badlogic.gdx.Screen;
import com.google.gson.Gson;

public class GameEngine extends EngineFrame implements Screen {

    // Kernel
    private ThreadSystem threadSystem;
    private WorldEngineSystem worldEngineSystem;
    private CameraSystem cameraSystem;
    private InputSystem inputSystem;
    private MovementManager movementManager;
    private ShaderPipeline shaderPipeline;
    private GeometryPipeline geometryPipeline;
    private RenderPipeline renderPipeline;

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
        this.shaderPipeline = (ShaderPipeline) register(new ShaderPipeline());
        this.geometryPipeline = (GeometryPipeline) register(new GeometryPipeline());
        this.renderPipeline = (RenderPipeline) register(new RenderPipeline());
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
    protected void draw() {
        this.renderPipeline.draw();
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
