package com.internal.bootstrap.inputpipeline;

import com.internal.bootstrap.inputpipeline.input.InputSystem;
import com.internal.core.engine.PipelinePackage;

public class InputPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Input Pipeline
        create(InputSystem.class);
    }
}
