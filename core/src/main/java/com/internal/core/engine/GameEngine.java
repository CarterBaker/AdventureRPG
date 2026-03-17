package com.internal.core.engine;

import com.internal.bootstrap.BootstrapPipeline;
import com.internal.runtime.RuntimePipeline;

public class GameEngine extends EnginePackage {

    /*
     * GameEngine defines the concrete engine instance.
     * Registers pipelines and managers, and routes execution
     * from Main to internal systems.
     */

    // BootStrap
    private BootstrapPipeline bootstrapPipeline;

    // Runtime
    private RuntimePipeline runtimePipeline;

    // BootStrap \\

    @Override
    protected void bootstrap() {

        // BootStrap
        this.bootstrapPipeline = create(BootstrapPipeline.class);
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
