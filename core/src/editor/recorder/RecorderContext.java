package editor.recorder;

import editor.recorder.panel.RecorderPanelSystem;
import engine.root.ContextPackage;

public class RecorderContext extends ContextPackage {

    /*
     * Editor tab for the recorder: the recordings saved in the game's
     * directory, the transport that records, plays and stops in the Dev
     * window, the timeline with every take and marker, the takes and markers
     * to rename, mute and delete, the channels armed for the next take, and
     * the export settings. All state lives in RecorderManager; this tab only
     * shows it and hands clicks to RecorderBranch.
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
