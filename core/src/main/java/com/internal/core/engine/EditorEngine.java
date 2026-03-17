package com.internal.core.engine;

import com.internal.editor.runtime.RuntimePipeline;

public class EditorEngine extends EnginePackage {

    /*
     * EditorEngine defines the concrete editor engine instance.
     * Registers editor-specific pipelines and managers, and routes
     * execution from MainEditor to internal systems.
     */

    // Bootstrap
    private com.internal.bootstrap.BootstrapPipeline bootstrapPipeline;
    private com.internal.editor.bootstrap.BootstrapPipeline editorBootstrapPipeline;

    // Runtime
    private RuntimePipeline runtimePipeline;

    // Bootstrap \\

    @Override
    protected void bootstrap() {

        // Bootstrap
        this.bootstrapPipeline = create(com.internal.bootstrap.BootstrapPipeline.class);
        this.editorBootstrapPipeline = create(com.internal.editor.bootstrap.BootstrapPipeline.class);
    }

    // Runtime \\

    @Override
    protected void create() {

        // Runtime
        this.runtimePipeline = create(RuntimePipeline.class);
    }

    @Override
    void draw() {
        this.bootstrapPipeline.draw();
    }
}