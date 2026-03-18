package com.internal.core.engine;

import com.internal.bootstrap.renderpipeline.rendermanager.RenderManager;
import com.internal.bootstrap.renderpipeline.windowmanager.WindowManager;
import com.internal.editor.runtime.RuntimeContext;

public class EditorEngine extends EnginePackage {

    /*
     * EditorEngine defines the concrete editor engine instance.
     * Registers editor-specific pipelines and managers, and routes
     * execution from MainEditor to internal systems.
     */

    // Bootstrap
    private com.internal.bootstrap.BootstrapAssembly bootstrapAssembly;
    private com.internal.editor.bootstrap.BootstrapAssembly editorBootstrapPipeline;

    // Runtime
    private RuntimeContext runtimeContext;

    // Render
    private WindowManager windowManager;
    private RenderManager renderSystem;

    // Bootstrap \\

    @Override
    protected void bootstrap() {

        // Bootstrap
        this.bootstrapAssembly = create(com.internal.bootstrap.BootstrapAssembly.class);
        this.editorBootstrapPipeline = create(com.internal.editor.bootstrap.BootstrapAssembly.class);
    }

    // Create \\

    @Override
    protected void create() {

        // Runtime
        this.runtimeContext = create(RuntimeContext.class);
    }

    // Get \\

    @Override
    protected void get() {

        // Render
        this.windowManager = get(WindowManager.class);
        this.renderSystem = get(RenderManager.class);
    }

    // Render \\

    @Override
    void draw() {
        windowManager.setActiveWindow(windowManager.getMainWindow());
        renderSystem.draw();
    }
}