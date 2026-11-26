package editor.runtime.treeviewer;

import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.EditorSetting;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.layerssource.LayersSourceSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
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

public class TreeViewerContext extends ContextPackage implements ViewCameraProvider, PropertiesProvider, LayersProvider {

    /*
     * Editor tab showing the Tree Planner's species as the world draws it, at
     * every detail side by side: in full, as a model, a sketch and an
     * outline, grown from the planner's seed to its age and lit by one fixed
     * light. Each detail is a layer in the Layers tab, with an eye showing or
     * hiding it; the layer picked is framed, every layer shown while none is.
     * The orbit button turns the view, the pan button slides it and scrolling
     * zooms. Every change in the planner or the Info Panel shows here as soon
     * as the row is built again.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private TreeViewerFrameSystem treeViewerFrameSystem;
    private ViewCameraSystem viewCameraSystem;
    private TreeViewerRenderSystem treeViewerRenderSystem;
    private TreeViewerMenuSystem treeViewerMenuSystem;
    private TreeViewerLayerSystem treeViewerLayerSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private LayersSourceSystem layersSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.treeViewerFrameSystem = create(TreeViewerFrameSystem.class);
        this.viewCameraSystem = create(ViewCameraSystem.class);
        this.treeViewerRenderSystem = create(TreeViewerRenderSystem.class);
        this.treeViewerMenuSystem = create(TreeViewerMenuSystem.class);
        create(ViewGizmoSystem.class);
        this.treeViewerLayerSystem = create(TreeViewerLayerSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.layersSourceSystem = create(LayersSourceSystem.class);
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

    // The tree's details are fixed: none is added or deleted
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
}
