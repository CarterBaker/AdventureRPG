package editor.bootstrap.notelibrarypipeline;

import editor.bootstrap.notelibrarypipeline.notelibrarymanager.NoteLibraryManager;
import engine.root.PipelinePackage;

public class NoteLibraryPipeline extends PipelinePackage {

    /*
     * Registers the note library's shared state, so every Note Library tab
     * shows the same instrument, notes and preview.
     */

    // Internal \\

    @Override
    protected void create() {
        create(NoteLibraryManager.class);
    }
}
