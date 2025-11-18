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
import com.AdventureRPG.Core.Util.Exceptions.CoreException;
import com.AdventureRPG.SettingsSystem.Settings;
import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;

public class EngineFrame extends ManagerFrame {

    // Root
    public Main main;
    public File path;
    public Gson gson;

    protected ThreadSystem threadSystem;
    protected WorldEngineSystem worldEngineSystem;
    protected CameraSystem cameraSystem;
    protected InputSystem inputSystem;
    protected MovementManager movementManager;
    protected TextureSystem textureSystem;
    protected ShaderManager shaderManager;
    protected PassSystem passSystem;
    protected MaterialSystem materialSystem;
    protected RenderManager renderManager;

    InternalState internalState = InternalState.CONSTRUCTOR;

    // Internal
    private SystemFrame[] engineSystems = new SystemFrame[10];

    // Base \\

    public EngineFrame() {

        // Root
        this.gameEngine = this;
        this.localManager = this;
    }

    // Internal State \\

    final InternalState getInternalState() {
        return internalState;
    }

    final void setInternalState(InternalState target) {
        this.internalState = target;
    }

    public final void requestInternalState(InternalState target) {

        if (!target.accessible)
            throw new CoreException.GameStateException(target);

        this.setInternalState(target);
    }

    // Create \\

    void bootKernel(
            Settings setting,
            Main main,
            File path,
            Gson gson) {

        // Root
        this.settings = setting;
        this.main = main;
        this.path = path;
        this.gson = gson;

        // Engine
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

        this.engineSystems[0] = threadSystem;
        this.engineSystems[1] = worldEngineSystem;
        this.engineSystems[2] = cameraSystem;
        this.engineSystems[3] = inputSystem;
        this.engineSystems[4] = movementManager;
        this.engineSystems[5] = textureSystem;
        this.engineSystems[6] = shaderManager;
        this.engineSystems[7] = passSystem;
        this.engineSystems[8] = materialSystem;
        this.engineSystems[9] = renderManager;

        this.preCreate();
        this.preInit();
        this.preAwake();

        internalProcess = InternalProcess.CREATE;
    }

    private void preCreate() {
        for (int i = 0; i < this.engineSystems.length; i++)
            this.engineSystems[i].internalCreate(this.settings, this.gameEngine);
    }

    private void preInit() {

        internalProcess = InternalProcess.INIT;

        for (int i = 0; i < this.engineSystems.length; i++)
            this.engineSystems[i].internalInit();
    }

    private void preAwake() {

        internalProcess = InternalProcess.AWAKE;

        for (int i = 0; i < this.engineSystems.length; i++)
            this.engineSystems[i].internalAwake();
    }

    // Create \\

    @Override
    void internalCreate(Settings settings, EngineFrame gameEngine) {

        internalProcess = InternalProcess.CREATE;

        super.internalCreate(settings, gameEngine);
    }

    // Init \\

    @Override
    void internalInit() {

        internalProcess = InternalProcess.INIT;

        super.internalInit();
    }

    // Awake \\

    @Override
    void internalAwake() {

        internalProcess = InternalProcess.AWAKE;

        super.internalAwake();
    }

    // Start \\

    @Override
    void internalStart() {

        internalProcess = InternalProcess.START;

        super.internalStart();
    }

    // Menu Exclusive Update \\

    @Override
    void internalMenuExclusiveUpdate() {

        internalProcess = InternalProcess.MENU_EXCLUSIVE;

        super.internalMenuExclusiveUpdate();
    }

    // Game Exclusive Update \\

    @Override
    void internalGameExclusiveUpdate() {

        internalProcess = InternalProcess.GAME_EXCLUSIVE;

        super.internalGameExclusiveUpdate();
    }

    // Update \\

    @Override
    void internalUpdate() {

        internalProcess = InternalProcess.UPDATE;

        super.internalUpdate();
    }

    // Fixed Update \\

    @Override
    void internalFixedUpdate() {

        internalProcess = InternalProcess.FIXED_UPDATE;

        super.internalFixedUpdate();
    }

    // Late Update \\

    @Override
    void internalLateUpdate() {

        internalProcess = InternalProcess.LATE_UPDATE;

        super.internalLateUpdate();
    }

    // Dispose \\

    @Override
    void internalDispose() {

        internalProcess = InternalProcess.DISPOSE;

        super.internalDispose();
    }

    // Render \\

    @Override
    void internalRender() {

        internalProcess = InternalProcess.RENDER;

        super.internalRender();

        this.renderManager.draw();
    }

    // Accessible \\

    public float getDeltaTime() {
        return Gdx.graphics.getDeltaTime();
    }
}
