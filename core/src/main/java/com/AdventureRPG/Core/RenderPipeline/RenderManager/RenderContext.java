package com.AdventureRPG.Core.RenderPipeline.RenderManager;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

public class RenderContext {
    public final SpriteBatch spriteBatch;
    public final ModelBatch modelBatch;
    public float deltaTime;

    public RenderContext(SpriteBatch spriteBatch, ModelBatch modelBatch) {
        this.spriteBatch = spriteBatch;
        this.modelBatch = modelBatch;
    }
}
