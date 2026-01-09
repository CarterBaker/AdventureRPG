package com.AdventureRPG.bootstrap.inputpipeline;

import com.AdventureRPG.bootstrap.inputpipeline.input.InputSystem;
import com.AdventureRPG.core.engine.PipelinePackage;

public class InputPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Input Pipeline
        create(InputSystem.class);
    }
}
