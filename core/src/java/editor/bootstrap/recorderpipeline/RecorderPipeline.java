package editor.bootstrap.recorderpipeline;

import editor.bootstrap.recorderpipeline.pianorollmanager.PianoRollManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import engine.root.PipelinePackage;

public class RecorderPipeline extends PipelinePackage {

    /*
     * Registers the recorder's shared state, so every Recorder tab and every
     * Dev window works on the same recording, transport and playhead, and the
     * Piano Roll's, which edits the notes clip selected on its Timeline.
     */

    // Internal \\

    @Override
    protected void create() {
        create(RecorderManager.class);
        create(PianoRollManager.class);
    }
}
