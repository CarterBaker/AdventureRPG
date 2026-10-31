package editor.bootstrap.transitionpipeline;

import editor.bootstrap.transitionpipeline.transitionmanager.TransitionManager;
import engine.root.PipelinePackage;

public class TransitionPipeline extends PipelinePackage {

    /*
     * Registers the editor's transitions, the fades, dissolves, wipes and
     * pushes every tool that cuts footage together lays between its clips,
     * whichever window it edits in.
     */

    // Internal \\

    @Override
    protected void create() {
        create(TransitionManager.class);
    }
}
