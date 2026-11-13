package editor.runtime.recorder;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.recorder.panel.RecorderRowSystem;
import editor.runtime.recorder.panel.RecorderTransportSystem;
import engine.root.ContextPackage;

public class RecorderContext extends ContextPackage implements RecorderTabProvider, FileMenuProvider {

    /*
     * Editor tab for the recorder's transport: record, play, pause and stop
     * in the Dev window, to the start and to the end, the loop range and
     * whether playing loops round it, the clock, and what is open — the
     * recording, and the recordings it was opened inside, with Back to
     * return to the one before. Its other work is in the tabs beside it: the
     * Timeline, where clips are laid and edited; the Recorder Keys, where
     * every value a take recorded is a curve of keys; the Recorder Bin of
     * takes beside the selected clip's edits; the Recorder Assets the
     * recording uses; and the Recorder Settings, the channels armed and the
     * export. The recordings themselves are made and opened in the
     * Recordings tool. All state lives in RecorderManager; this tab only
     * shows it and hands clicks to RecorderBranch.
     */

    // Internal
    private RecorderRowSystem recorderRowSystem;
    private RecorderTransportSystem recorderTransportSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.recorderRowSystem = create(RecorderRowSystem.class);
        this.recorderTransportSystem = create(RecorderTransportSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tab \\

    @Override
    public String getRecorderMenu() {
        return EditorSetting.RECORDER_MENU;
    }

    // The transport is no pane, so working in it leaves the pane the keys act on as it is
    @Override
    public RecorderPane getRecorderPane() {
        return null;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_RECORDER;
    }
}
