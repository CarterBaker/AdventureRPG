package editor.runtime.animationtimeline;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.runtime.EditorSetting;
import editor.runtime.animationtimeline.menu.AnimationTimelineMenuSystem;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;

public class AnimationTimelineContext extends ContextPackage implements FileMenuProvider {

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
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.animationTimelineMenuSystem = create(AnimationTimelineMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_ANIMATION;
    }
}
