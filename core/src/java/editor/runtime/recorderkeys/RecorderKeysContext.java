package editor.runtime.recorderkeys;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.recorder.RecorderTabProvider;
import editor.runtime.recorder.panel.RecorderRowSystem;
import editor.runtime.recorder.panel.RecorderKeysSystem;
import engine.root.ContextPackage;

public class RecorderKeysContext extends ContextPackage implements RecorderTabProvider, FileMenuProvider {

    /*
     * Editor tab for the keys of the take the Recorder's selected clip
     * plays: an outliner of its tracks, each value a curve of keys on a
     * strip across the stretch of the timeline the Recorder shows, every
     * character's movements and gestures as nodes, and the take's sounds;
     * a graph of the selected curve to retime, move and bend its keys; and
     * an inspector of whatever is selected. While it is worked in, the
     * recorder's keys and deletes act on its keys. All state lives in
     * RecorderManager; clicks, drags and the wheel go to RecorderBranch.
     */

    // Internal
    private RecorderRowSystem recorderRowSystem;
    private RecorderKeysSystem recorderKeysSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.recorderRowSystem = create(RecorderRowSystem.class);
        this.recorderKeysSystem = create(RecorderKeysSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tab \\

    @Override
    public String getRecorderMenu() {
        return EditorSetting.RECORDER_KEYS_MENU;
    }

    @Override
    public RecorderPane getRecorderPane() {
        return RecorderPane.KEYS;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_RECORDER;
    }
}
