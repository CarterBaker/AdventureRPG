package com.internal.core.engine;

import com.internal.bootstrap.BootstrapAssembly;
import com.internal.runtime.RuntimeContext;

public class GameEngine extends EnginePackage {

    /*
     * GameEngine defines the concrete engine instance.
     * Registers pipelines and managers, and routes execution
     * from Main to internal systems.
     */

    // BootStrap
    private BootstrapAssembly bootstrapAssembly;

    // Runtime
    private RuntimeContext runtimeContext;

    // BootStrap \\

    @Override
    protected void bootstrap() {

        // BootStrap
        this.bootstrapAssembly = create(BootstrapAssembly.class);
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
