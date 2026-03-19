package com.internal.core.engine;

import com.internal.bootstrap.BootstrapAssembly;
import com.internal.bootstrap.renderpipeline.rendermanager.RenderManager;
import com.internal.bootstrap.renderpipeline.windowmanager.WindowManager;
import com.internal.runtime.RuntimeContext;

public class GameEngine extends EnginePackage {

    /*
     * GameEngine defines the concrete game engine instance. Bootstraps all
     * pipelines and managers via BootstrapAssembly, then creates and pairs
     * the runtime context with the main window in awake() — after get() has
     * resolved the window manager. createContext() fires CREATE, GET, AWAKE,
     * and RELEASE on the context immediately, then START on the next frame.
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
        this.runtimeContext = createContext(RuntimeContext.class, windowManager.getMainWindow());
    }

    // Render \\

    @Override
    void draw() {
        windowManager.setActiveWindow(windowManager.getMainWindow());
        renderManager.draw();
    }
}