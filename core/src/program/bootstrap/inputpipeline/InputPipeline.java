package program.bootstrap.inputpipeline;

import program.bootstrap.inputpipeline.inputsystem.InputSystem;
import program.core.engine.PipelinePackage;

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