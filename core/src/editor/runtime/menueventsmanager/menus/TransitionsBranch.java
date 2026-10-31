package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderClipEdge;
import editor.bootstrap.transitionpipeline.transitionmanager.TransitionManager;
import engine.root.BranchPackage;

public class TransitionsBranch extends BranchPackage {

    /*
     * Menu event handlers for the Transitions tab. Every action works on the
     * one set of transitions all tabs share. Pressing a row selects its
     * transition and picks it up, to be laid on the clip edge of whichever
     * Recorder tab's lanes it is let go over; the toolbar lays the selected
     * transition on the start or the end of the Recorder's selected clip.
     * Rows hand back the name of the transition they show.
     */

    // Internal
    private TransitionManager transitionManager;
    private RecorderManager recorderManager;
    private RecorderBranch recorderBranch;

    // Base \\

    @Override
    protected void get() {
        this.transitionManager = get(TransitionManager.class);
        this.recorderManager = get(RecorderManager.class);
        this.recorderBranch = get(RecorderBranch.class);
    }

    // Transitions \\

    // The transition is selected on the press and carried toward the Recorder's lanes until let go
    public void dragTransition(String transitionName, WindowInstance window) {
        transitionManager.selectTransition(transitionName);
        recorderBranch.dragTransition(transitionName, window);
    }

    public void layOnClipStart() {
        recorderManager.applyTransition(RecorderClipEdge.START);
    }

    public void layOnClipEnd() {
        recorderManager.applyTransition(RecorderClipEdge.END);
    }
}
