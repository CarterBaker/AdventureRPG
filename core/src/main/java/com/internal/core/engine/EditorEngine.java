package com.internal.core.engine;

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

    // Bootstrap \\

    @Override
    protected void bootstrap() {

        // Bootstrap
        this.bootstrapAssembly = create(com.internal.bootstrap.BootstrapAssembly.class);
        this.editorBootstrapPipeline = create(com.internal.editor.bootstrap.BootstrapAssembly.class);
    }

    // Runtime \\

    @Override
    protected void create() {

        // Runtime
        this.runtimeContext = create(RuntimeContext.class);
    }

    @Override
    void draw() {
        this.bootstrapAssembly.draw();
    }
}