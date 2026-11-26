package editor.runtime.treeviewer;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.treeviewerpipeline.treeviewermanager.TreeViewerManager;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.layerssource.LayersSourceSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.treeviewer.frame.TreeViewerFrameSystem;
import editor.runtime.treeviewer.layer.TreeViewerLayerSystem;
import editor.runtime.treeviewer.menu.TreeViewerMenuSystem;
import editor.runtime.treeviewer.render.TreeViewerRenderSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import editor.runtime.viewcamera.ViewGizmoSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreeViewerContext extends ContextPackage
        implements ViewCameraProvider, ToolCanvasProvider, ToolActionProvider, PropertiesProvider, LayersProvider,
        FileMenuProvider {

    /*
     * Editor tab for making trees: the species picked in the Hierarchy as the
     * world draws it, every field of it, LOD seed included, edited live in
     * the Properties tab. Each LOD level the world draws a tree at is a layer
     * in the Layers tab, and last the shared tree the species' LOD seed
     * grows, all standing on one root, the topmost layer whose eye is open
     * the one drawn. The Tools tab's Preview actions grow the preview from a
     * new seed or to another age, and the File menu makes, saves and reverts
     * species. The orbit button turns the view, the pan button slides it and
     * scrolling zooms. All state lives in TreeViewerManager; this tab only
     * shows it.
     */

    // Internal
    private TreeViewerManager treeViewerManager;
    private EditorInputSystem editorInputSystem;
    private TreeViewerFrameSystem treeViewerFrameSystem;
    private ViewCameraSystem viewCameraSystem;
    private TreeViewerLayerSystem treeViewerLayerSystem;
    private TreeViewerRenderSystem treeViewerRenderSystem;
    private TreeViewerMenuSystem treeViewerMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private LayersSourceSystem layersSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.treeViewerFrameSystem = create(TreeViewerFrameSystem.class);
        this.viewCameraSystem = create(ViewCameraSystem.class);
        this.treeViewerLayerSystem = create(TreeViewerLayerSystem.class);
        this.treeViewerRenderSystem = create(TreeViewerRenderSystem.class);
        this.treeViewerMenuSystem = create(TreeViewerMenuSystem.class);
        create(ViewGizmoSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.layersSourceSystem = create(LayersSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void get() {
        this.treeViewerManager = get(TreeViewerManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return treeViewerFrameSystem;
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return ToolCanvas.TREE_VIEWER;
    }

    @Override
    public void runToolAction(String actionKey) {
        treeViewerManager.act(actionKey);
    }

    // Layers \\

    @Override
    public int getLayerRevision() {
        return treeViewerLayerSystem.getRevision();
    }

    @Override
    public String getLayerTitle() {
        return EditorSetting.TAB_TITLE_TREE_VIEWER;
    }

    @Override
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {
        treeViewerLayerSystem.buildLayers(roots);
    }

    @Override
    public void selectLayer(String layerKey) {
        treeViewerLayerSystem.select(layerKey);
    }

    @Override
    public void toggleLayerVisible(String layerKey) {
        treeViewerLayerSystem.toggleShown(layerKey);
    }

    // The tree's layers are fixed: none is added or deleted
    @Override
    public String getAddLayerKey() {
        return "";
    }

    @Override
    public String getRemoveLayerKey() {
        return "";
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
        return EditorSetting.FILE_MENU_TREE_VIEWER;
    }
}
