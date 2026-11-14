package editor.bootstrap.faceeditorpipeline;

import editor.bootstrap.faceeditorpipeline.faceeditormanager.FaceEditorManager;
import engine.root.PipelinePackage;

public class FaceEditorPipeline extends PipelinePackage {

    /*
     * Registers the face editor's shared state, so the Character Editor's
     * Face layer, the Face Outliner window and the Properties tab work on the
     * same face control of the character open in the Character Editor.
     */

    // Internal \\

    @Override
    protected void create() {
        create(FaceEditorManager.class);
    }
}
