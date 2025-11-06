package com.AdventureRPG.RenderManager;

import com.AdventureRPG.Core.GameManager;
import com.AdventureRPG.PassManager.PassData;
import com.AdventureRPG.PlayerSystem.PlayerSystem;
import com.AdventureRPG.ShaderManager.ShaderManager;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

public class RenderManager extends GameManager {

    // Root
    private ShaderManager shaderManager;
    private UISystem UISystem;
    private WorldSystem worldSystem;
    private PlayerSystem playerSystem;
    private RenderQueue renderQueue;

    // Base \\

    @Override
    public void init() {

        // Root
        this.shaderManager = rootManager.shaderManager;
        this.UISystem = rootManager.UISystem;
        this.worldSystem = rootManager.worldSystem;
        this.playerSystem = rootManager.playerSystem;
        this.renderQueue = (RenderQueue) register(new RenderQueue());

        // Core passes

        renderQueue.addPass(new PassData(
                0, "3D_PASS", -1, null, null,
                shaderManager.universalUniform,
                ctx -> {
                    ctx.modelBatch.begin(playerSystem.getCamera());
                    worldSystem.render();
                    playerSystem.render();
                    ctx.modelBatch.end();
                }), 0);

        renderQueue.addPass(new PassData(
                0, "2D_PASS", -1, null, null,
                shaderManager.universalUniform,
                ctx -> {
                    ctx.spriteBatch.begin();
                    UISystem.render();
                    ctx.spriteBatch.end();
                }), 0);
    }

    // Core Logic \\

    public void draw(SpriteBatch spriteBatch, ModelBatch modelBatch) {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);

        RenderContext context = new RenderContext(spriteBatch, modelBatch);

        context.deltaTime = Gdx.graphics.getDeltaTime();
        renderQueue.renderAll(context);
    }

    // Accessible \\

    public void enqueue(PassData pass, int sortOrder) {
        renderQueue.addPass(pass, sortOrder);
    }
}
