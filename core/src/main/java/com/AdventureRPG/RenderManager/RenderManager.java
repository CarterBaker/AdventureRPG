package com.AdventureRPG.RenderManager;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.PlayerSystem.PlayerSystem;
import com.AdventureRPG.UISystem.UISystem;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

public class RenderManager {

    // Game Manager
    private final UISystem UISystem;
    private final WorldSystem worldSystem;
    private final PlayerSystem playerSystem;

    // Render Queue
    private final RenderQueue renderQueue;

    // Base \\

    public RenderManager(GameManager GameManager) {

        // Game Manager
        this.UISystem = GameManager.UISystem;
        this.worldSystem = GameManager.worldSystem;
        this.playerSystem = GameManager.playerSystem;

        this.renderQueue = new RenderQueue(GameManager);

        // Core passes
        renderQueue.addPass(new RenderPass(
                0, "3D_PASS", -1, null, null,
                ctx -> {
                    ctx.modelBatch.begin(playerSystem.getCamera());
                    worldSystem.render(ctx.modelBatch);
                    playerSystem.render();
                    ctx.modelBatch.end();
                }));

        renderQueue.addPass(new RenderPass(
                0, "2D_PASS", -1, null, null,
                ctx -> {
                    ctx.spriteBatch.begin();
                    UISystem.render();
                    ctx.spriteBatch.end();
                }));
    }

    // Core Logic \\

    public void draw(SpriteBatch spriteBatch, ModelBatch modelBatch, float deltaTime) {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        RenderContext context = new RenderContext(spriteBatch, modelBatch);

        context.deltaTime = deltaTime;
        renderQueue.renderAll(context);
    }

    // Accessible \\

    public void enqueue(RenderPass pass) {
        renderQueue.addPass(pass);
    }
}
