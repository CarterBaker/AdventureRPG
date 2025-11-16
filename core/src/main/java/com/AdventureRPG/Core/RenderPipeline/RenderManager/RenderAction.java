package com.AdventureRPG.Core.RenderPipeline.RenderManager;

@FunctionalInterface
public interface RenderAction {
    void render(RenderContext context);
}