package com.AdventureRPG.Core.Root;

import java.io.File;

import com.AdventureRPG.InputSystem.InputSystem;
import com.AdventureRPG.LightingSystem.LightingManager;
import com.AdventureRPG.MaterialSystem.MaterialSystem;
import com.AdventureRPG.PassSystem.PassSystem;
import com.AdventureRPG.PlayerSystem.PlayerManager;
import com.AdventureRPG.RenderManager.RenderManager;
import com.AdventureRPG.SaveManager.SaveManager;
import com.AdventureRPG.ShaderManager.ShaderManager;
import com.AdventureRPG.TextureSystem.TextureSystem;
import com.AdventureRPG.ThreadSystem.ThreadSystem;
import com.AdventureRPG.TimeSystem.TimeSystem;
import com.AdventureRPG.UISystem.LoadScreen;
import com.AdventureRPG.UISystem.Menu;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.WorldManager.WorldManager;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.scenes.scene2d.ui.Table.Debug;
import com.google.gson.Gson;

public class RootManager extends ManagerFrame implements Screen {

    // Root
    public final Main game;
    public final File path;
    public final Gson gson;

    /// Rendering
    public final ShaderProvider shaderProvider;
    public final Environment environment;
    public final SpriteBatch spriteBatch;
    public final ModelBatch modelBatch;

    // Core
    private ThreadSystem threadSystem;
    private TextureSystem textureSystem;
    private ShaderManager shaderManager;
    private MaterialSystem materialSystem;
    private PassSystem passSystem;
    private SaveManager saveManager;
    private UISystem UISystem;
    private LightingManager lightingManager;
    private TimeSystem timeSystem;
    private PlayerManager playerManager;
    private WorldManager worldManager;
    private InputSystem inputSystem;
    private RenderManager renderManager;

    // UI
    private LoadScreen loadScreen;

    // Base \\

    public RootManager(
            Main game,
            File path,
            Gson gson,
            ShaderProvider shaderProvider,
            Environment environment,
            SpriteBatch spriteBatch,
            ModelBatch modelBatch) {

        // Root
        this.game = game;
        this.path = path;
        this.gson = gson;

        // Rendering
        this.shaderProvider = shaderProvider;
        this.environment = environment;
        this.spriteBatch = spriteBatch;
        this.modelBatch = modelBatch;
    }

    @Override
    protected void create() {

        // Core
        threadSystem = (ThreadSystem) register(new ThreadSystem());
        shaderManager = (ShaderManager) register(new ShaderManager());
        textureSystem = (TextureSystem) register(new TextureSystem());
        materialSystem = (MaterialSystem) register(new MaterialSystem());
        passSystem = (PassSystem) register(new PassSystem());
        saveManager = (SaveManager) register(new SaveManager());
        UISystem = (UISystem) register(new UISystem());
        lightingManager = (LightingManager) register(new LightingManager());
        timeSystem = (TimeSystem) register(new TimeSystem());
        playerManager = (PlayerManager) register(new PlayerManager());
        worldManager = (WorldManager) register(new WorldManager());
        inputSystem = (InputSystem) register(new InputSystem());
        renderManager = (RenderManager) register(new RenderManager());
    }

    @Override
    protected void init() {

        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));
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
    protected void menuExclusiveUpdate() {

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

        renderManager.draw(spriteBatch, modelBatch);
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

        playerManager.updateViewport(width, height);
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

        shaderProvider.dispose();
    }
}
