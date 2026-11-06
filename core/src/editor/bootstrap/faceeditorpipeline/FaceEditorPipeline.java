package editor.bootstrap.faceeditorpipeline;

import editor.bootstrap.faceeditorpipeline.faceeditormanager.FaceEditorManager;
import engine.root.PipelinePackage;

public class FaceEditorPipeline extends PipelinePackage {

    /*
     * Registers the face editor's shared state, so every Face Editor,
     * Face Outliner and Face Properties window works on the same face
     * control of the character open in the Character Editor.
     */

    // Internal \\

    @Override
    protected void create() {
        create(FaceEditorManager.class);
    }
}
