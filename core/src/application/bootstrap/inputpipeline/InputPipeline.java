package application.bootstrap.inputpipeline;

import application.bootstrap.inputpipeline.inputsystem.InputSystem;
import application.core.engine.PipelinePackage;

public class InputPipeline extends PipelinePackage {

    /*
     * Registers all input systems for the engine. Owns the InputSystem
     * which handles raw platform input and exposes clean state to the rest
     * of the engine.
     */

    @Override
    protected void create() {
        create(InputSystem.class);
    }
}