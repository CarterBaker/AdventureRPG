package editor.runtime.structureeditor;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.propertiespipeline.properties.PaletteRowSource;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PaletteContent;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.structureeditorpipeline.structureeditormanager.StructureEditorManager;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.menueventsmanager.menus.StructureEditorBranch;
import editor.runtime.layerssource.LayersSourceSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.structureeditor.menu.StructureEditorMenuSystem;
import editor.runtime.structureeditor.render.StructureEditorRenderSystem;
import editor.runtime.structureeditor.tool.StructureEditorToolSystem;
import editor.runtime.structureeditor.view.StructureEditorViewSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import editor.runtime.viewcamera.ViewGizmoSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureEditorContext extends ContextPackage
        implements ViewCameraProvider, ToolCanvasProvider, ToolActionProvider, LayersProvider, PaletteRowSource,
        PropertiesProvider, FileMenuProvider {

    /*
     * Editor tab for building structures as a voxel modeller builds models,
     * in a stack of layers (a floor, a roof, any alteration) the Layers tab
     * lists, each added, deleted, moved, renamed and shown or hidden through
     * the Tools tab's Layers actions and its eye, and each holding five fixed
     * jobs: Blocks lays and takes away blocks, Coverage paints the coverings
     * grown over them, Contents the contents whole blocks hold, Furniture
     * stands furnishings and Entities places the NPCs who live there, each
     * with the Tools tab's Hand, Brush, Erase, Fill and Pick. The Palette tab
     * lists every structure, then what the job picked paints with, each laid
     * only where the game takes it. The Properties tab holds the structure's
     * whole entry (where and how it places, its terrain, rules, biomes,
     * furnishings and spawns), and the File and Edit menus make, save, revert,
     * undo and redo. All state lives in StructureEditorManager; this tab only
     * shows it.
     */

    // Internal
    private StructureEditorManager structureEditorManager;
    private StructureEditorBranch structureEditorBranch;
    private EditorInputSystem editorInputSystem;
    private StructureEditorViewSystem structureEditorViewSystem;
    private StructureEditorToolSystem structureEditorToolSystem;
    private StructureEditorRenderSystem structureEditorRenderSystem;
    private StructureEditorMenuSystem structureEditorMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private LayersSourceSystem layersSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.structureEditorViewSystem = create(StructureEditorViewSystem.class);
        this.structureEditorToolSystem = create(StructureEditorToolSystem.class);
        create(ViewCameraSystem.class);
        this.structureEditorRenderSystem = create(StructureEditorRenderSystem.class);
        this.structureEditorMenuSystem = create(StructureEditorMenuSystem.class);
        create(ViewGizmoSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.layersSourceSystem = create(LayersSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void get() {
        this.structureEditorManager = get(StructureEditorManager.class);
        this.structureEditorBranch = get(StructureEditorBranch.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return structureEditorViewSystem;
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return ToolCanvas.STRUCTURE;
    }

    // The Layers actions, the same keys the Layers tab's New Layer and Delete Layer icons run
    @Override
    public void runToolAction(String actionKey) {
        structureEditorBranch.runLayerAction(actionKey, getWindow());
    }

    // Layers \\

    @Override
    public int getLayerRevision() {
        return structureEditorManager.getListRevision();
    }

    @Override
    public String getLayerTitle() {
        return StructureEditorSetting.LAYER_TITLE;
    }

    @Override
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {
        structureEditorManager.buildLayers(roots);
    }

    @Override
    public void selectLayer(String layerKey) {
        structureEditorManager.selectLayer(layerKey);
    }

    // A layer's eye shows or hides all it lays, in the editor only
    @Override
    public void toggleLayerVisible(String layerKey) {
        structureEditorManager.toggleLayerShown(layerKey);
    }

    @Override
    public String getAddLayerKey() {
        return EditorSetting.STRUCTURE_EDITOR_KEY_LAYER_ADD;
    }

    @Override
    public String getRemoveLayerKey() {
        return EditorSetting.STRUCTURE_EDITOR_KEY_LAYER_REMOVE;
    }

    // Palette \\

    // Every structure, then what the layer picked paints with
    @Override
    public PaletteContent getPaletteContent() {
        return PaletteContent.ROWS;
    }

    @Override
    public int getPaletteRevision() {
        return structureEditorManager.getListRevision();
    }

    @Override
    public void buildPaletteRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        structureEditorManager.buildPaletteRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getPaletteTemplates() {
        return StructureEditorSetting.ROW_TEMPLATES;
    }

    // The palette holds no numbers to step
    @Override
    public void stepPaletteRow(String key, int steps) {
    }

    // Properties \\

    // The structure's whole entry, selected in the Hierarchy, shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_STRUCTURES;
    }
}
