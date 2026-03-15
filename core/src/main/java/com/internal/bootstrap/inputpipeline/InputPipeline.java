package com.internal.bootstrap.inputpipeline;

import com.internal.bootstrap.inputpipeline.inputsystem.InputSystem;
import com.internal.core.engine.PipelinePackage;

public class InputPipeline extends PipelinePackage {

    /*
     * Registers all input systems for the engine. Owns the InputSystem
     * which handles raw LibGDX input and exposes clean state to the rest
     * of the engine.
     */

    @Override
    protected void create() {
        create(InputSystem.class);
    }
}