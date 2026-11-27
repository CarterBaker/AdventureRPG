package editor.runtime.recorderkeygraph;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.propertiespipeline.properties.PaletteRowSource;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PaletteContent;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
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
import editor.runtime.recorder.panel.RecorderKeyGraphSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderKeyGraphContext extends ContextPackage
        implements RecorderTabProvider, ToolCanvasProvider, ToolActionProvider, LayersProvider, PaletteRowSource,
        FileMenuProvider, PropertiesProvider, CharacterViewRowSource {

    /*
     * Editor tab for the graph of the value selected on the Keys tab: its
     * curve across the stretch of the timeline the Timeline shows, between
     * the least and greatest values framed, its keys to retime and move and
     * their handles to bend, a key clicked twice freeing its handles or
     * joining them again. What can be done to the keys is the Tools tab's
     * actions. While it is worked in, the recorder's keys and deletes act on
     * its keys, and the Properties tab lists the keys selected to move, set
     * and give a mode, then keeps the lane and clip selected. All state lives in RecorderManager; clicks, drags and the
     * wheel go to RecorderBranch.
     */

    // Internal
    private RecorderRowSystem recorderRowSystem;
    private RecorderKeyGraphSystem recorderKeyGraphSystem;
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
        this.recorderKeyGraphSystem = create(RecorderKeyGraphSystem.class);
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
        return EditorSetting.RECORDER_KEY_GRAPH_MENU;
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
