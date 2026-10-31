package editor.runtime.recordings;

import editor.runtime.recordings.panel.RecordingsPanelSystem;
import engine.root.ContextPackage;

public class RecordingsContext extends ContextPackage {

    /*
     * Editor tab for the recordings saved in the game's directory: every one
     * by name, most recently changed first, the open one marked, and what the
     * open one holds. A row opens its recording in the recorder every
     * Recorder tab edits, and the toolbar makes, renames, copies, saves and
     * deletes recordings. All state lives in RecorderManager; this tab only
     * shows it and hands clicks to RecordingsBranch.
     */

    // Internal
    private RecordingsPanelSystem recordingsPanelSystem;

    // Internal \\

    @Override
    protected void create() {
        this.recordingsPanelSystem = create(RecordingsPanelSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
