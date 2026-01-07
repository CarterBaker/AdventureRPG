package com.AdventureRPG.core.inputpipeline;

import com.AdventureRPG.core.engine.PipelinePackage;
import com.AdventureRPG.core.inputpipeline.input.InputSystem;
import com.AdventureRPG.core.inputpipeline.movementmanager.MovementManager;

public class InputPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Input Pipeline
        create(InputSystem.class);
        create(MovementManager.class);
    }
}
