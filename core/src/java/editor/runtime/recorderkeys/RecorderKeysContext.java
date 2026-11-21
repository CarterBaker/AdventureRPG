package editor.runtime.recorderkeys;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.layerspipeline.layers.LayerDragProvider;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayerViewsProvider;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
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
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.layerssource.LayersSourceSystem;
import editor.runtime.menueventsmanager.menus.RecorderBranch;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.recorder.RecorderTabProvider;
import editor.runtime.recorder.panel.RecorderRowSystem;
import editor.runtime.recorder.panel.RecorderKeysSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderKeysContext extends ContextPackage
        implements RecorderTabProvider, ToolCanvasProvider, ToolActionProvider, LayersProvider, LayerViewsProvider,
        LayerDragProvider, FileMenuProvider, PropertiesProvider, CharacterViewRowSource {

    /*
     * Editor tab for the keys of the layer picked in the Layers tab, of the
     * take the Timeline's selected clip plays: a row for every value it holds
     * and, for a character, a row of its movement and one of its gestures,
     * each strip showing its keys or nodes across the stretch of the timeline
     * the Timeline shows; the Sounds layer a row for each sound it names. The
     * graph of the value selected is the Keys Graph tab beside it, and what
     * can be done to keys, sounds and nodes the Tools tab's actions. While it
     * is worked in, the recorder's keys and deletes act on its keys, and the
     * Properties tab lists the keys selected to move, set and give a mode,
     * then keeps the lane and clip selected. All state lives
     * in RecorderManager; clicks, drags and the wheel go to RecorderBranch.
     */

    // Internal
    private RecorderRowSystem recorderRowSystem;
    private RecorderKeysSystem recorderKeysSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private LayersSourceSystem layersSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private RecorderBranch recorderBranch;
    private RecorderManager recorderManager;

    // Internal \\

    @Override
    protected void create() {
        this.recorderRowSystem = create(RecorderRowSystem.class);
        this.recorderKeysSystem = create(RecorderKeysSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.layersSourceSystem = create(LayersSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
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
        return EditorSetting.RECORDER_KEYS_MENU;
    }

    @Override
    public RecorderPane getRecorderPane() {
        return RecorderPane.KEYS;
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

    // The Properties tab keeps the mix of the lane and the clip selected, as the Timeline shows them
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.ROWS;
    }

    // Moves with the keys selected as well, which are listed first, to move, set and give a mode
    @Override
    public int getRowRevision() {
        return recorderManager.getKeyRowRevision();
    }

    @Override
    public String getRowTitle() {
        return recorderManager.getRowsTitle(true);
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        recorderManager.buildKeyRows(rows);
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

    // Layers \\

    @Override
    public int getLayerRevision() {
        return recorderManager.getLayerRevision();
    }

    @Override
    public String getLayerTitle() {
        return recorderManager.getLayerTitle(getRecorderPane());
    }

    @Override
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {
        recorderManager.buildLayers(roots, getRecorderPane());
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
        return recorderManager.getRemoveLayerKey(getRecorderPane());
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
        return recorderManager.getLayerView(getRecorderPane()).ordinal();
    }

    @Override
    public void showLayerView(int view) {
        recorderManager.showLayerView(getRecorderPane(), RecorderLayerView.VALUES[view]);
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_RECORDER;
    }
}
