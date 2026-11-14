package editor.runtime.recordersettings;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.recorder.RecorderSetting;
import editor.runtime.recorder.RecorderTabProvider;
import editor.runtime.recorder.panel.RecorderRowSystem;
import editor.runtime.recorder.panel.RecorderSettingsSystem;
import engine.root.ContextPackage;

public class RecorderSettingsContext extends ContextPackage implements RecorderTabProvider, FileMenuProvider {

    /*
     * Editor tab for the recorder's settings: the channels armed for the next
     * take and the open recording's export — its size, frame rate, format,
     * motion blur, supersampling and range — with the export itself and its
     * progress. It is no pane, so working in it leaves the pane the keys act
     * on as it is. All state lives in RecorderManager; clicks go to
     * RecorderBranch.
     */

    // Internal
    private RecorderRowSystem recorderRowSystem;
    private RecorderSettingsSystem recorderSettingsSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.recorderRowSystem = create(RecorderRowSystem.class);
        this.recorderSettingsSystem = create(RecorderSettingsSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tab \\

    @Override
    public String getRecorderMenu() {
        return RecorderSetting.MENU_SETTINGS;
    }

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
