package editor.runtime.recorderbin;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.recorder.RecorderSetting;
import editor.runtime.recorder.RecorderTabProvider;
import editor.runtime.recorder.panel.RecorderRowSystem;
import editor.runtime.recorder.panel.RecorderBinSystem;
import engine.root.ContextPackage;

public class RecorderBinContext extends ContextPackage implements RecorderTabProvider, FileMenuProvider {

    /*
     * Editor tab for the recorder's bin: every take recorded, to drag onto
     * the Recorder's timeline, lay at the playhead, rename or delete, beside
     * the clip selected on the timeline with what it plays, what is selected
     * on it and every edit it takes. While it is worked in, the recorder's
     * deletes act on the clip. All state lives in RecorderManager; clicks and
     * drags go to RecorderBranch.
     */

    // Internal
    private RecorderRowSystem recorderRowSystem;
    private RecorderBinSystem recorderBinSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.recorderRowSystem = create(RecorderRowSystem.class);
        this.recorderBinSystem = create(RecorderBinSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tab \\

    @Override
    public String getRecorderMenu() {
        return RecorderSetting.MENU_BIN;
    }

    @Override
    public RecorderPane getRecorderPane() {
        return RecorderPane.BIN;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_RECORDER;
    }
}
