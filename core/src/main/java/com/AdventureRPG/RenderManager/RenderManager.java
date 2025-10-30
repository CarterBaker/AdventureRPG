package com.AdventureRPG.RenderManager;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.PassManager.PassData;
import com.AdventureRPG.PlayerSystem.PlayerSystem;
import com.AdventureRPG.ShaderManager.ShaderManager;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

public class RenderManager {

    // Game Manager
    private final ShaderManager shaderManager;
    private final UISystem UISystem;
    private final WorldSystem worldSystem;
    private final PlayerSystem playerSystem;

    // Render Queue
    private final RenderQueue renderQueue;

    // Base \\

    public RenderManager(GameManager gameManager) {

        // Game Manager
        this.shaderManager = gameManager.shaderManager;
        this.UISystem = gameManager.UISystem;
        this.worldSystem = gameManager.worldSystem;
        this.playerSystem = gameManager.playerSystem;

        this.renderQueue = new RenderQueue(gameManager);

        // Core passes

        renderQueue.addPass(new PassData(
                0, "WORLD_PASS", -1, null, null,
                shaderManager.universalUniform,
                ctx -> {
                    ctx.modelBatch.begin(playerSystem.getCamera());
                    worldSystem.renderManager.render();
                    ctx.modelBatch.end();
                }), 0);

        renderQueue.addPass(new PassData(
                0, "3D_PASS", -1, null, null,
                shaderManager.universalUniform,
                ctx -> {
                    ctx.modelBatch.begin(playerSystem.getCamera());
                    worldSystem.render(ctx.modelBatch);
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

    public void draw(SpriteBatch spriteBatch, ModelBatch modelBatch, float deltaTime) {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);

        RenderContext context = new RenderContext(spriteBatch, modelBatch);

        context.deltaTime = deltaTime;
        renderQueue.renderAll(context);
    }

    // Accessible \\

    public void enqueue(PassData pass, int sortOrder) {
        renderQueue.addPass(pass, sortOrder);
    }
}
