package com.AdventureRPG.RenderManager;

@FunctionalInterface
public interface RenderAction {
    void render(RenderContext context);
}