package editor.runtime.recorder;

import editor.runtime.recorder.panel.RecorderPanelSystem;
import engine.root.ContextPackage;

public class RecorderContext extends ContextPackage {

    /*
     * Editor tab for the recorder: the recordings saved in the game's
     * directory, the transport that records, plays and stops in the Dev
     * window, the bin of takes to drag onto the timeline, the timeline's
     * lanes of clips to move, split, trim, stamp, mute and strip of channels,
     * the markers, the channels armed for the next take, and the export
     * settings. All state lives in RecorderManager; this tab only shows it
     * and hands clicks and drags to RecorderBranch.
     */

    // Internal
    private RecorderPanelSystem recorderPanelSystem;

    // Internal \\

    @Override
    protected void create() {
        this.recorderPanelSystem = create(RecorderPanelSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
