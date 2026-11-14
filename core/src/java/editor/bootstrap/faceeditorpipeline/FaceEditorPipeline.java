package editor.bootstrap.faceeditorpipeline;

import editor.bootstrap.faceeditorpipeline.faceeditormanager.FaceEditorManager;
import engine.root.PipelinePackage;

public class FaceEditorPipeline extends PipelinePackage {

    /*
     * Registers the face editor's shared state, so every Face Editor and
     * Face Outliner window and the Properties tab work on the same face
     * control of the character open in the character tabs.
     */

    // Internal \\

    @Override
    protected void create() {
        create(FaceEditorManager.class);
    }
}
