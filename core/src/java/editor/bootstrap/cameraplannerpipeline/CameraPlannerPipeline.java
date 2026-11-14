package editor.bootstrap.cameraplannerpipeline;

import editor.bootstrap.cameraplannerpipeline.cameraplannermanager.CameraPlannerManager;
import engine.root.PipelinePackage;

public class CameraPlannerPipeline extends PipelinePackage {

    /*
     * Registers the camera planner's shared state, so every Camera Planner
     * tab designs the same camera, the World Editor places exactly what it
     * designs, and every Preview tab's main menu looks out from the camera
     * it edits.
     */

    // Internal \\

    @Override
    protected void create() {
        create(CameraPlannerManager.class);
    }
}
