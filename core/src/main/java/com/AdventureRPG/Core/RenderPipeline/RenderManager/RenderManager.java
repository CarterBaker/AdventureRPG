package com.AdventureRPG.Core.RenderPipeline.RenderManager;

import com.AdventureRPG.Core.RenderPipeline.CameraSystem.CameraSystem;
import com.AdventureRPG.Core.RenderPipeline.PassSystem.PassData;
import com.AdventureRPG.Core.RenderPipeline.ShaderManager.ShaderManager;
import com.AdventureRPG.Core.Root.ManagerFrame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

public class RenderManager extends ManagerFrame {

    // Root
    private ShaderManager shaderManager;
    private CameraSystem cameraSystem;
    private RenderQueueSystem renderQueueSystem;

    // Base \\

    @Override
    protected void create() {

        this.renderQueueSystem = (RenderQueueSystem) register(new RenderQueueSystem());
    }

    @Override
    protected void init() {

        // Root
        this.shaderManager = engineManager.get(ShaderManager.class);
        this.cameraSystem = engineManager.get(CameraSystem.class);
    }

    @Override
    protected void awake() {

        // Core passes
        renderQueueSystem.addPass(new PassData(
                0, "3D_PASS", -1, null, null,
                shaderManager.universalUniform,
                ctx -> {
                    ctx.modelBatch.begin(cameraSystem.mainCamera().getPerspectiveCamera());
                    ctx.modelBatch.end();
                }), 0);

        renderQueueSystem.addPass(new PassData(
                0, "2D_PASS", -1, null, null,
                shaderManager.universalUniform,
                ctx -> {
                    ctx.spriteBatch.begin();
                    ctx.spriteBatch.end();
                }), 0);
    }

    // Core Logic \\

    public void draw(SpriteBatch spriteBatch, ModelBatch modelBatch) {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);

        RenderContext context = new RenderContext(spriteBatch, modelBatch);

        context.deltaTime = Gdx.graphics.getDeltaTime();
        renderQueueSystem.renderAll(context);
    }

    // Accessible \\

    public void enqueue(PassData pass, int sortOrder) {
        renderQueueSystem.addPass(pass, sortOrder);
    }
}
