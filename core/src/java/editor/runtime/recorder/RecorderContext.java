package editor.runtime.recorder;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.recorder.panel.RecorderRowSystem;
import editor.runtime.recorder.panel.RecorderTransportSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderContext extends ContextPackage
        implements RecorderTabProvider, FileMenuProvider, PropertiesProvider, CharacterViewRowSource {

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
     * shows it and hands clicks to RecorderBranch. With a Music file open it
     * shows its tempo, meter and key and the metronome, and the Properties
     * tab sets them.
     */

    // Internal
    private RecorderRowSystem recorderRowSystem;
    private RecorderTransportSystem recorderTransportSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private RecorderManager recorderManager;

    // Internal \\

    @Override
    protected void create() {
        this.recorderRowSystem = create(RecorderRowSystem.class);
        this.recorderTransportSystem = create(RecorderTransportSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
    }

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
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

    // Properties \\

    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.ROWS;
    }

    @Override
    public int getRowRevision() {
        return recorderManager.getRevision();
    }

    @Override
    public String getRowTitle() {
        return EditorSetting.RECORDER_TRANSPORT_PROPERTIES_TITLE;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        recorderManager.buildRows(rows, false);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.RECORDER_MUSIC_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        recorderManager.stepRow(key, steps);
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_RECORDER;
    }
}
