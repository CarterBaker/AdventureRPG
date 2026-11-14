package application.kernel.inputpipeline;

import application.kernel.inputpipeline.inputmanager.InputManager;
import engine.root.PipelinePackage;

public class InputPipeline extends PipelinePackage {

    /*
     * Registers all input systems for the engine. Owns the InputSystem
     * which handles raw platform input and exposes clean state to the rest
     * of the engine.
     */

    @Override
    protected void create() {
        create(InputManager.class);
    }
}