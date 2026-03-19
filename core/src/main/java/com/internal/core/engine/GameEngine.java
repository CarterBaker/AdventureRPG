package com.internal.core.engine;

import com.internal.bootstrap.BootstrapAssembly;
import com.internal.bootstrap.renderpipeline.rendermanager.RenderManager;
import com.internal.bootstrap.renderpipeline.windowmanager.WindowManager;
import com.internal.runtime.RuntimeContext;

public class GameEngine extends EnginePackage {

    /*
     * GameEngine defines the concrete game engine instance. Bootstraps all
     * pipelines and managers via BootstrapAssembly, then creates the game
     * runtime context. windowManager and renderManager are resolved in start()
     * not get() — bootstrap must be complete before they exist in the registry.
     */

    // Bootstrap
    private BootstrapAssembly bootstrapAssembly;

    // Runtime
    private RuntimeContext runtimeContext;

    // Render
    private WindowManager windowManager;
    private RenderManager renderManager;

    // Bootstrap \\

    @Override
    protected void bootstrap() {
        this.bootstrapAssembly = create(BootstrapAssembly.class);
    }

    // Create \\

    @Override
    protected void create() {
        this.runtimeContext = create(RuntimeContext.class);
    }

    // Get \\

    @Override
    protected void get() {

        // Render
        this.windowManager = get(WindowManager.class);
        this.renderManager = get(RenderManager.class);
    }

    // Awake \\

    @Override
    protected void awake() {

        // Render
        this.runtimeContext.setWindow(windowManager.getMainWindow());
    }

    // Render \\

    @Override
    void draw() {
        windowManager.setActiveWindow(windowManager.getMainWindow());
        renderManager.draw();
    }
}