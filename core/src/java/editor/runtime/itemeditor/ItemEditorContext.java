package editor.runtime.itemeditor;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.materialspipeline.materials.MaterialDropProvider;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.itemeditor.frame.ItemEditorFrameSystem;
import editor.runtime.itemeditor.menu.ItemEditorMenuSystem;
import editor.runtime.itemeditor.render.ItemEditorRenderSystem;
import editor.runtime.itemeditor.tool.ItemEditorToolSystem;
import editor.runtime.layerssource.LayersSourceSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraShareProvider;
import editor.runtime.viewcamera.ViewCameraStateStruct;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import editor.runtime.viewcamera.ViewGizmoSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ItemEditorContext extends ContextPackage
        implements ToolCanvasProvider, ToolActionProvider, ViewCameraProvider, ViewCameraShareProvider, LayersProvider,
        PropertiesProvider, FileMenuProvider, MaterialDropProvider {

    /*
     * Editor tab for building items from sub-voxel cubes. Reads input, orbits
     * the camera, casts the pointer, draws the active item and the stamp the
     * tools would lay, and shows the status bar — all state and cube logic
     * live in the editor and engine. The item's parts are its layers in the
     * Layers tab, each one material, added and deleted by the tab's icons or
     * the Tools tab's actions, the one picked laid by the tools; a material
     * dropped here from the Materials tab colours the part under the pointer.
     * The tab is a tool canvas, the item, for as long as it is open.
     */

    // Internal
    private ItemEditorManager itemEditorManager;
    private EditorInputSystem editorInputSystem;
    private ItemEditorFrameSystem itemEditorFrameSystem;
    private ViewCameraSystem viewCameraSystem;
    private ItemEditorToolSystem itemEditorToolSystem;
    private ItemEditorRenderSystem itemEditorRenderSystem;
    private ItemEditorMenuSystem itemEditorMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;
    private LayersSourceSystem layersSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.itemEditorFrameSystem = create(ItemEditorFrameSystem.class);
        this.viewCameraSystem = create(ViewCameraSystem.class);
        this.itemEditorToolSystem = create(ItemEditorToolSystem.class);
        this.itemEditorRenderSystem = create(ItemEditorRenderSystem.class);
        this.itemEditorMenuSystem = create(ItemEditorMenuSystem.class);
        create(ViewGizmoSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
        this.layersSourceSystem = create(LayersSourceSystem.class);
    }

    @Override
    protected void get() {
        this.itemEditorManager = get(ItemEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return ToolCanvas.ITEM;
    }

    // The tab's actions run exactly as the Layers tab's icons run them
    @Override
    public void runToolAction(String actionKey) {
        itemEditorManager.act(actionKey, getWindow().getGLWindow());
    }

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return itemEditorFrameSystem;
    }

    // Stacked with the modelling tabs, it shares their view, so its item and its baked mesh are seen alike
    @Override
    public String getViewCameraShare() {
        return EditorSetting.VIEW_CAMERA_SHARE_CHARACTER;
    }

    @Override
    public ViewCameraStateStruct getViewCameraState() {
        return viewCameraSystem.getViewCameraState();
    }

    // Layers \\

    @Override
    public int getLayerRevision() {
        return itemEditorManager.getRevision();
    }

    @Override
    public String getLayerTitle() {
        return EditorSetting.TAB_TITLE_ITEM_EDITOR;
    }

    @Override
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {
        itemEditorManager.buildLayers(roots);
    }

    @Override
    public void selectLayer(String layerKey) {
        itemEditorManager.selectLayer(layerKey);
    }

    // A part is never hidden: every part is the item
    @Override
    public void toggleLayerVisible(String layerKey) {
    }

    @Override
    public String getAddLayerKey() {
        return EditorSetting.ITEM_EDITOR_KEY_PART_ADD;
    }

    @Override
    public String getRemoveLayerKey() {
        return EditorSetting.ITEM_EDITOR_KEY_PART_REMOVE;
    }

    // Materials \\

    @Override
    public boolean dropMaterial(String materialName) {
        return itemEditorToolSystem.dropMaterial(materialName);
    }

    // Properties \\

    // What this tool works on is content selected in the Hierarchy, shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_ITEM;
    }
}
