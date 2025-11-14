package com.AdventureRPG.Core.RenderPipeline.RenderManager;

public class SpriteBatch extends RenderContext {

    public SpriteBatch() {
        this("SpriteBatch");
    }

    public SpriteBatch(String name) {
        this.name = name;
    }
}
