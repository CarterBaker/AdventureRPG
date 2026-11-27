package editor.runtime.recorder;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.propertiespipeline.properties.PaletteRowSource;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PaletteContent;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.dev.recorder.DevRecorderSoundSystem;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.layerssource.LayersSourceSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.recorder.panel.RecorderRowSystem;
import editor.runtime.recorder.panel.RecorderTransportSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderContext extends ContextPackage
        implements RecorderTabProvider, RecorderSoundHost, FileMenuProvider, PropertiesProvider, CharacterViewRowSource,
        LayersProvider, PaletteRowSource {

    /*
     * Editor tab for the recorder's transport: record, play, pause and stop
     * in the Dev window, the loop that runs playing round the range marked,
     * with the status saying what they did or why they could not and the
     * clock beside them. A media file with no picture plays its sound here
     * when no Dev window is open. Its other work is in the tabs beside it: the
     * Timeline, where clips are laid and edited; the Keys, where every value
     * of the layer picked in the Layers tab is a row of keys; and the Keys
     * Graph of the value selected. The recordings themselves are made and
     * opened in the Recordings tool. All state lives in RecorderManager; this
     * tab only shows it and hands clicks to RecorderBranch. Its layers in the
     * Layers tab are the channels, each eye open while the next take records
     * it, as the Preview's layers say what it plays, and its Palette is every
     * take of the open media file to pick and lay. With a media file open
     * the Properties tab sets its export, and its tempo, meter and key.
     */

    // Internal
    private RecorderRowSystem recorderRowSystem;
    private RecorderTransportSystem recorderTransportSystem;
    private DevRecorderSoundSystem devRecorderSoundSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private LayersSourceSystem layersSourceSystem;
    private RecorderManager recorderManager;

    // Internal \\

    @Override
    protected void create() {
        this.recorderRowSystem = create(RecorderRowSystem.class);
        this.recorderTransportSystem = create(RecorderTransportSystem.class);
        this.devRecorderSoundSystem = create(DevRecorderSoundSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.layersSourceSystem = create(LayersSourceSystem.class);
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
        recorderManager.buildSettingsRows(rows);
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

    // Layers \\

    @Override
    public int getLayerRevision() {
        return recorderManager.getLayerRevision();
    }

    @Override
    public String getLayerTitle() {
        return EditorSetting.RECORDER_LAYERS_TITLE_ARMED;
    }

    @Override
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {
        recorderManager.buildChannelLayers(roots, false);
    }

    // A channel is armed or not by its eye alone; there is no channel to pick
    @Override
    public void selectLayer(String layerKey) {
    }

    @Override
    public void toggleLayerVisible(String layerKey) {

        RecorderChannel channel = recorderManager.resolveChannelLayer(layerKey);

        if (channel != null)
            recorderManager.toggleArm(channel);
    }

    @Override
    public String getAddLayerKey() {
        return "";
    }

    @Override
    public String getRemoveLayerKey() {
        return "";
    }

    // Palette \\

    @Override
    public PaletteContent getPaletteContent() {
        return PaletteContent.ROWS;
    }

    @Override
    public int getPaletteRevision() {
        return recorderManager.getLayerRevision();
    }

    @Override
    public void buildPaletteRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        recorderManager.buildTakeRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getPaletteTemplates() {
        return EditorSetting.RECORDER_TAKE_ROW_TEMPLATES;
    }

    // A take row has no number to slide
    @Override
    public void stepPaletteRow(String key, int steps) {
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_RECORDER;
    }
}
