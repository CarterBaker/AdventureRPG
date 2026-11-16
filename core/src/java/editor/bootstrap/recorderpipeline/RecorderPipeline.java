package editor.bootstrap.recorderpipeline;

import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import engine.root.PipelinePackage;

public class RecorderPipeline extends PipelinePackage {

    /*
     * Registers the recorder's shared state, so every Recorder tab and every
     * Dev window works on the same recording, transport and playhead.
     */

    // Internal \\

    @Override
    protected void create() {
        create(RecorderManager.class);
    }
}
