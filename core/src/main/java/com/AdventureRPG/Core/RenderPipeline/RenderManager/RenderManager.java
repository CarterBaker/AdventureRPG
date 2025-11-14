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

    // Base \\

    @Override
    protected void create() {

        // Root
        this.uHandleSystem = (UHandleSystem) register(new UHandleSystem());
        this.renderQueueSystem = (RenderQueueSystem) register(new RenderQueueSystem());

        this.spriteBatch = (SpriteBatch) create(new SpriteBatch());
        this.modelBatch = (ModelBatch) create(new ModelBatch());

        GPUCall.enableDepth();
    }

    @Override
    protected void init() {

        // Root
        this.shaderManager = engineManager.get(ShaderManager.class);
        this.cameraSystem = engineManager.get(CameraSystem.class);
    }

    public void draw() {

        GPUCall.clearBuffer();
        renderQueueSystem.renderAll();
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

    // Accessible \\

    public void enqueue(PassData pass, int sortOrder) {
        renderQueueSystem.addPass(pass, sortOrder);
    }
}
