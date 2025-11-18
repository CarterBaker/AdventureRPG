package com.AdventureRPG.Core.RenderPipeline.RenderManager;

import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.RenderPipeline.CameraSystem.CameraSystem;
import com.AdventureRPG.Core.RenderPipeline.PassSystem.PassData;
import com.AdventureRPG.Core.RenderPipeline.RenderableInstance.MeshPacket;
import com.AdventureRPG.Core.RenderPipeline.ShaderManager.ShaderManager;
import com.AdventureRPG.Core.RenderPipeline.Util.GPUCall;

public class RenderManager extends ManagerFrame {

    // Root
    private ShaderManager shaderManager;
    private CameraSystem cameraSystem;

    private UHandleSystem uHandleSystem;
    private RenderQueueSystem renderQueueSystem;

    private SpriteBatch spriteBatch;
    private ModelBatch modelBatch;

    private RenderContext renderContext;

    // Base \\

    @Override
    protected void create() {

        // Root
        this.uHandleSystem = (UHandleSystem) register(new UHandleSystem());
        this.renderQueueSystem = (RenderQueueSystem) register(new RenderQueueSystem());

        this.spriteBatch = (SpriteBatch) register(new SpriteBatch());
        this.modelBatch = (ModelBatch) register(new ModelBatch());

        this.renderContext = (RenderContext) register(new RenderContext());

        GPUCall.enableDepth();
    }

    @Override
    protected void init() {

        // Root
        this.shaderManager = gameEngine.get(ShaderManager.class);
        this.cameraSystem = gameEngine.get(CameraSystem.class);
    }

    @Override
    protected void awake() {

        // Core passes
        renderQueueSystem.addPass(
                new PassData(
                        0,
                        "3D_PASS",
                        -1,
                        null,
                        null,
                        shaderManager.universalUniformSystem,
                        ctx -> {
                            modelBatch.draw();
                        }),
                0);

        renderQueueSystem.addPass(
                new PassData(
                        0,
                        "2D_PASS",
                        -1,
                        null,
                        null,
                        shaderManager.universalUniformSystem,
                        ctx -> {
                        }),
                0);
    }

    public void draw() {

        GPUCall.clearBuffer();
        renderQueueSystem.draw(renderContext);
    }

    // Pass System \\

    public void enqueue(PassData pass, int sortOrder) {
        renderQueueSystem.addPass(pass, sortOrder);
    }

    // U Handle System \\

    public int createUniqueHandle() {
        return uHandleSystem.createKey();
    }

    public void releaseHandle(int key) {
        uHandleSystem.removeKey(key);
    }

    // Model Batch

    public void addModel(MeshPacket meshPacket) {
        modelBatch.addModel(meshPacket);
    }

    public void removeModel(MeshPacket meshPacket) {
        modelBatch.removeModel(meshPacket);
    }
}
