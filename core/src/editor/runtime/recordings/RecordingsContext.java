package editor.runtime.recordings;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.recordings.panel.RecordingsPanelSystem;
import engine.root.ContextPackage;

public class RecordingsContext extends ContextPackage implements FileMenuProvider {

    /*
     * Editor tab for the recordings saved in the game's directory: every one
     * by name, most recently changed first, the open one marked, and what the
     * open one holds. A row opens its recording in the recorder every
     * Recorder tab edits, the toolbar renames the open one, and the editor's
     * File menu makes, copies, saves and deletes them while this tab is the
     * one worked in. All state lives in RecorderManager; this tab only shows
     * it and hands clicks to RecordingsBranch.
     */

    // Internal
    private RecordingsPanelSystem recordingsPanelSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.recordingsPanelSystem = create(RecordingsPanelSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_RECORDINGS;
    }
}
