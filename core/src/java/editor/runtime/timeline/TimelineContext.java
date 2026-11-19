package editor.runtime.timeline;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.layerspipeline.layers.LayerDragProvider;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayerViewsProvider;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.propertiespipeline.properties.OutlinerProvider;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderLayerView;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.dev.recorder.DevRecorderSoundSystem;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.layerssource.LayersSourceSystem;
import editor.runtime.menueventsmanager.menus.RecorderBranch;
import editor.runtime.recorder.RecorderSoundHost;
import editor.runtime.recorder.RecorderTabProvider;
import editor.runtime.recorder.panel.RecorderRowSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.recorder.panel.RecorderTimelineSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TimelineContext extends ContextPackage
        implements ToolCanvasProvider, ToolActionProvider, RecorderTabProvider, RecorderSoundHost, FileMenuProvider,
        PropertiesProvider, OutlinerProvider, LayersProvider, LayerViewsProvider, LayerDragProvider,
        CharacterViewRowSource {

    /*
     * Editor tab for the recorder's timeline: its lanes of clips to move,
     * split, trim, stamp, mute and strip of channels, zoomed and scrolled,
     * sounds dropped onto them from the sound library, saved recordings from
     * the Recordings tab to nest, and transitions on their edges from the
     * transitions tool, each clip showing the nodes of what the player does
     * along it; its title names the recording open and the ones it was opened
     * inside. A nested clip clicked twice opens its recording. The transport
     * is the Recorder tab beside it, and a media file with no picture plays
     * its sound here when neither a Dev window nor the Recorder tab is open.
     * All state lives in RecorderManager; this tab only shows it and hands
     * clicks, drags and the wheel to RecorderBranch. The tab is a tool
     * canvas, the timeline, for as long as it is open: the Tools tab lays,
     * renames and deletes the take picked and edits the clip, keys, sound,
     * transition and node selected, nests clips and exports MIDI, and File's
     * Export sends the media file out as a video or as music. Working in it
     * makes the clips what the recorder's deletes act on. The Properties tab
     * shows the mix of the lane selected and the clip selected's channels,
     * mute, time ramp and instrument; the Layers tab the layers of its take or
     * every take; the Character Outliner the channels armed and the export.
     */

    // Internal
    private RecorderRowSystem recorderRowSystem;
    private RecorderTimelineSystem recorderTimelineSystem;
    private DevRecorderSoundSystem devRecorderSoundSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private LayersSourceSystem layersSourceSystem;
    private RecorderBranch recorderBranch;
    private RecorderManager recorderManager;

    // Internal \\

    @Override
    protected void create() {
        this.recorderRowSystem = create(RecorderRowSystem.class);
        this.recorderTimelineSystem = create(RecorderTimelineSystem.class);
        this.devRecorderSoundSystem = create(DevRecorderSoundSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.layersSourceSystem = create(LayersSourceSystem.class);
    }

    @Override
    protected void get() {
        this.recorderBranch = get(RecorderBranch.class);
        this.recorderManager = get(RecorderManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tab \\

    @Override
    public String getRecorderMenu() {
        return EditorSetting.RECORDER_TIMELINE_MENU;
    }

    @Override
    public RecorderPane getRecorderPane() {
        return RecorderPane.TIMELINE;
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return recorderManager.getToolCanvas();
    }

    // Every action runs exactly as its key runs it, on this tab's window
    @Override
    public void runToolAction(String actionKey) {
        recorderBranch.actSetting(actionKey, getWindow());
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
        return EditorSetting.RECORDER_TIMELINE_PROPERTIES_TITLE;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        recorderManager.buildRows(rows, true);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.RECORDER_MUSIC_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        recorderManager.stepRow(key, steps);
    }

    // Outliner \\

    @Override
    public int getOutlinerRevision() {
        return recorderManager.getRevision();
    }

    @Override
    public String getOutlinerTitle() {
        return EditorSetting.RECORDER_OUTLINER_TITLE;
    }

    @Override
    public void buildOutlinerRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        recorderManager.buildSettingsRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getOutlinerTemplates() {
        return EditorSetting.RECORDER_MUSIC_ROW_TEMPLATES;
    }

    @Override
    public void stepOutlinerRow(String key, int steps) {
        recorderManager.stepRow(key, steps);
    }

    // Layers \\

    @Override
    public int getLayerRevision() {
        return recorderManager.getLayerRevision();
    }

    @Override
    public String getLayerTitle() {
        return recorderManager.getLayerTitle();
    }

    @Override
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {
        recorderManager.buildLayers(roots);
    }

    @Override
    public void selectLayer(String layerKey) {
        recorderManager.selectLayer(layerKey);
    }

    // A take of the Takes view is carried onto the Timeline's lanes
    @Override
    public void dragLayer(String layerKey, WindowInstance window) {
        recorderBranch.dragLayer(layerKey, window);
    }

    // Every layer of a take is always shown, so none has an eye
    @Override
    public void toggleLayerVisible(String layerKey) {
    }

    @Override
    public String getAddLayerKey() {
        return "";
    }

    @Override
    public String getRemoveLayerKey() {
        return recorderManager.getRemoveLayerKey();
    }

    @Override
    public int getLayerViewCount() {
        return RecorderLayerView.VALUES.length;
    }

    @Override
    public String getLayerViewLabel(int view) {
        return RecorderLayerView.VALUES[view].getLabel();
    }

    @Override
    public int getLayerView() {
        return recorderManager.getLayerView().ordinal();
    }

    @Override
    public void showLayerView(int view) {
        recorderManager.showLayerView(RecorderLayerView.VALUES[view]);
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_RECORDER;
    }
}
