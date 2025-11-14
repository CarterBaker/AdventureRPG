package com.AdventureRPG.Core.Bootstrap;

import java.io.File;

import com.AdventureRPG.Core.PhysicsPipeline.MovementManager.MovementManager;
import com.AdventureRPG.Core.RenderPipeline.CameraSystem.CameraSystem;
import com.AdventureRPG.Core.RenderPipeline.MaterialSystem.MaterialSystem;
import com.AdventureRPG.Core.RenderPipeline.PassSystem.PassSystem;
import com.AdventureRPG.Core.RenderPipeline.RenderManager.RenderManager;
import com.AdventureRPG.Core.RenderPipeline.ShaderManager.ShaderManager;
import com.AdventureRPG.Core.RenderPipeline.TextureSystem.TextureSystem;
import com.AdventureRPG.Core.ScenePipeline.WorldEngineSystem.WorldEngineSystem;
import com.AdventureRPG.Core.ThreadPipeline.ThreadSystem;
import com.AdventureRPG.SettingsSystem.Settings;
import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;

// TODO: Needs to be cleaned a bit. The whole framework needs a look over
public class EngineFrame extends ManagerFrame {

    // Root
    public Main main;
    public File path;
    public Gson gson;

    protected ThreadSystem threadSystem;
    protected WorldEngineSystem worldEngineSystem;
    protected CameraSystem cameraSystem;
    protected MovementManager movementManager;
    protected TextureSystem textureSystem;
    protected ShaderManager shaderManager;
    protected PassSystem passSystem;
    protected MaterialSystem materialSystem;
    protected RenderManager renderManager;

    // Internal
    private SystemFrame[] engineSystems = new SystemFrame[9];

    // Base \\

    public EngineFrame() {

        // Root
        this.engineManager = this;
        this.localManager = this;
    }

    // Internal Process \\

    final InternalProcess getInternalRootProcess() {
        return internalProcess;
    }

    final void setInternalRootProcess(InternalProcess internalProcess) {
        this.internalProcess = internalProcess;
    }

    // Internal State \\

    final InternalState getInternalRootState() {
        return internalState;
    }

    final void setInternalRootState(InternalState internalState) {
        this.internalState = internalState;
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
        threadSystem = (ThreadSystem) register(new ThreadSystem());
        worldEngineSystem = (WorldEngineSystem) register(new WorldEngineSystem());
        cameraSystem = (CameraSystem) register(new CameraSystem());
        movementManager = (MovementManager) register(new MovementManager());
        textureSystem = (TextureSystem) register(new TextureSystem());
        shaderManager = (ShaderManager) register(new ShaderManager());
        passSystem = (PassSystem) register(new PassSystem());
        materialSystem = (MaterialSystem) register(new MaterialSystem());
        renderManager = (RenderManager) register(new RenderManager());

        engineSystems[0] = threadSystem;
        engineSystems[1] = worldEngineSystem;
        engineSystems[2] = cameraSystem;
        engineSystems[3] = movementManager;
        engineSystems[4] = textureSystem;
        engineSystems[5] = shaderManager;
        engineSystems[6] = passSystem;
        engineSystems[7] = materialSystem;
        engineSystems[8] = renderManager;

        preCreate();
        preInit();
        preAwake();

        setInternalRootProcess(InternalProcess.CREATE);
    }

    private void preCreate() {

        setInternalRootProcess(InternalProcess.CREATE);

        for (int i = 0; i < engineSystems.length; i++)
            engineSystems[i].internalCreate(settings, engineManager);
    }

    private void preInit() {

        setInternalRootProcess(InternalProcess.INIT);

        for (int i = 0; i < engineSystems.length; i++)
            engineSystems[i].internalInit();
    }

    private void preAwake() {

        setInternalRootProcess(InternalProcess.AWAKE);

        for (int i = 0; i < engineSystems.length; i++)
            engineSystems[i].internalAwake();
    }

    // Render \\

    @Override
    void internalRender() {

        super.internalRender();

        renderManager.draw();
    }

    // Accessible \\

    public float getDeltaTime() {
        return Gdx.graphics.getDeltaTime();
    }
}
