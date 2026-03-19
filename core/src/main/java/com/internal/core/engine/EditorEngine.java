package com.internal.core.engine;

import com.internal.bootstrap.renderpipeline.rendermanager.RenderManager;
import com.internal.bootstrap.renderpipeline.windowmanager.WindowManager;
import com.internal.editor.runtime.RuntimeContext;

public class EditorEngine extends EnginePackage {

    /*
     * EditorEngine defines the concrete editor engine instance. Bootstraps
     * both the shared game pipeline and the editor-specific pipeline via
     * their respective BootstrapAssemblies, then creates the editor context
     * paired with the main window in awake() after get() has resolved
     * the window manager.
     */

    // Bootstrap
    private com.internal.bootstrap.BootstrapAssembly bootstrapAssembly;
    private com.internal.editor.bootstrap.BootstrapAssembly editorBootstrapAssembly;

    // Runtime
    private RuntimeContext runtimeContext;

    // Render
    private WindowManager windowManager;
    private RenderManager renderManager;

    // Bootstrap \\

    @Override
    protected void bootstrap() {

        // Bootstrap
        this.bootstrapAssembly = create(com.internal.bootstrap.BootstrapAssembly.class);
        this.editorBootstrapAssembly = create(com.internal.editor.bootstrap.BootstrapAssembly.class);
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