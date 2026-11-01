package editor.runtime.animationtimeline;

import editor.runtime.animationtimeline.menu.AnimationTimelineMenuSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;

public class AnimationTimelineContext extends ContextPackage {

    /*
     * Docker window holding the animation editor's timeline, the way an
     * animation program's timeline docker does: the transport, a summary
     * track to scrub and play under the playhead, and a dope sheet of one
     * lane per bone marking its keys. Picking a lane picks its bone and
     * moves the playhead for every Animation Editor tab; the state lives in
     * AnimationEditorManager. Its own keys play, step and undo; the shared
     * tool keys work while it is focused.
     */

    // Internal
    private AnimationTimelineMenuSystem animationTimelineMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;

    // Internal \\

    @Override
    protected void create() {
        this.animationTimelineMenuSystem = create(AnimationTimelineMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
