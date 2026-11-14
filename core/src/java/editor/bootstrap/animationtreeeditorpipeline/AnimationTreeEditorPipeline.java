package editor.bootstrap.animationtreeeditorpipeline;

import editor.bootstrap.animationtreeeditorpipeline.animationtreeeditormanager.AnimationTreeEditorManager;
import engine.root.PipelinePackage;

public class AnimationTreeEditorPipeline extends PipelinePackage {

    /*
     * Registers the animation tree editor's shared state, so every Animation
     * Tree Editor tab works on the same tree and plays the same simulation.
     */

    // Internal \\

    @Override
    protected void create() {
        create(AnimationTreeEditorManager.class);
    }
}
