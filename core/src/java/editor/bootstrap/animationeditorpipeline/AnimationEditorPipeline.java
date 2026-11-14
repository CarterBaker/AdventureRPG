package editor.bootstrap.animationeditorpipeline;

import editor.bootstrap.animationeditorpipeline.animationeditormanager.AnimationEditorManager;
import engine.root.PipelinePackage;

public class AnimationEditorPipeline extends PipelinePackage {

    /*
     * Registers the animation editor's shared state, so every Animation
     * Editor tab works on the same clip at the same moment of it.
     */

    // Internal \\

    @Override
    protected void create() {
        create(AnimationEditorManager.class);
    }
}
