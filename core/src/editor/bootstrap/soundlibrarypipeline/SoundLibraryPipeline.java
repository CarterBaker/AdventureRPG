package editor.bootstrap.soundlibrarypipeline;

import editor.bootstrap.soundlibrarypipeline.soundlibrarymanager.SoundLibraryManager;
import engine.root.PipelinePackage;

public class SoundLibraryPipeline extends PipelinePackage {

    /*
     * Registers the sound library's shared state, so every Sound Library tab
     * shows the same sounds, selection and preview.
     */

    // Internal \\

    @Override
    protected void create() {
        create(SoundLibraryManager.class);
    }
}
