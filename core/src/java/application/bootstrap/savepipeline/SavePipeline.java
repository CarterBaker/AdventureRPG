package application.bootstrap.savepipeline;

import application.bootstrap.savepipeline.savemanager.SaveManager;
import engine.root.PipelinePackage;

public class SavePipeline extends PipelinePackage {

    /*
     * Registers the save pipeline. SaveManager owns every character save on
     * disk and is the single entry point runtime systems route persistence
     * through.
     */

    @Override
    protected void create() {
        create(SaveManager.class);
    }
}