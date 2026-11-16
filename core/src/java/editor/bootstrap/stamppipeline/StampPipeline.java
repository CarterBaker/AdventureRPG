package editor.bootstrap.stamppipeline;

import editor.bootstrap.stamppipeline.stampmanager.StampManager;
import engine.root.PipelinePackage;

public class StampPipeline extends PipelinePackage {

    /*
     * Registers the editor's stamps, the body parts a character is built
     * from, each carrying its own bones and the loops it joins by.
     */

    // Internal \\

    @Override
    protected void create() {
        create(StampManager.class);
    }
}
