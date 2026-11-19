package editor.runtime.recorderkeys;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayerViewsProvider;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderLayerView;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.layerssource.LayersSourceSystem;
import editor.runtime.menueventsmanager.menus.RecorderBranch;
import editor.runtime.recorder.RecorderTabProvider;
import editor.runtime.recorder.panel.RecorderRowSystem;
import editor.runtime.recorder.panel.RecorderKeysSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderKeysContext extends ContextPackage
        implements RecorderTabProvider, ToolCanvasProvider, ToolActionProvider, LayersProvider, LayerViewsProvider,
        FileMenuProvider {

    /*
     * Editor tab for the keys of the layer picked in the Layers tab, of the
     * take the Timeline's selected clip plays: a row for every value it holds
     * and, for a character, a row of its movement and one of its gestures,
     * each strip showing its keys or nodes across the stretch of the timeline
     * the Timeline shows; the Sounds layer a row for each sound it names. The
     * graph of the value selected is the Keys Graph tab beside it, and what
     * can be done to keys, sounds and nodes the Tools tab's actions. While it
     * is worked in, the recorder's keys and deletes act on its keys. All
     * state lives in RecorderManager; clicks, drags and the wheel go to
     * RecorderBranch.
     */

    // Internal
    private RecorderRowSystem recorderRowSystem;
    private RecorderKeysSystem recorderKeysSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private LayersSourceSystem layersSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;
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
