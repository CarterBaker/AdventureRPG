package editor.runtime.transitions;

import editor.runtime.transitions.panel.TransitionsPanelSystem;
import engine.root.ContextPackage;

public class TransitionsContext extends ContextPackage {

    /*
     * Editor tab for the transitions, the way a paint program's brush docker
     * holds its brushes: every fade, dissolve, wipe, push and iris by name,
     * the one selected marked with how it looks and how long it first runs. A
     * transition is picked up from its row and let go over a clip's edge on a
     * Recorder tab's timeline to lay it there, or laid on the Recorder's
     * selected clip from the toolbar. Transitions are content, made and
     * changed in the Hierarchy and Info Panel. All state lives in
     * TransitionManager and RecorderManager; this tab only shows it and hands
     * clicks and drags to TransitionsBranch.
     */

    // Internal
    private TransitionsPanelSystem transitionsPanelSystem;

    // Internal \\

    @Override
    protected void create() {
        this.transitionsPanelSystem = create(TransitionsPanelSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
