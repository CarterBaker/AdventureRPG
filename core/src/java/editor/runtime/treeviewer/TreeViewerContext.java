package editor.runtime.treeviewer;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.treeviewer.frame.TreeViewerFrameSystem;
import editor.runtime.treeviewer.menu.TreeViewerMenuSystem;
import editor.runtime.treeviewer.render.TreeViewerRenderSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.ContextPackage;

public class TreeViewerContext extends ContextPackage implements ViewCameraProvider, PropertiesProvider {

    /*
     * Editor tab showing the Tree Planner's species as the world draws it, at
     * every detail side by side: in full, as a model, a sketch and an
     * outline, grown from the planner's seed to its age and lit by one fixed
     * light. The orbit button turns the view, the pan button slides it and
     * scrolling zooms; the toolbar frames the whole row or one tier. Every change in the planner
     * or the Info Panel shows here as soon as the row is built again.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private TreeViewerFrameSystem treeViewerFrameSystem;
    private ViewCameraSystem viewCameraSystem;
    private TreeViewerRenderSystem treeViewerRenderSystem;
    private TreeViewerMenuSystem treeViewerMenuSystem;
    private PropertiesSourceSystem propertiesSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.treeViewerFrameSystem = create(TreeViewerFrameSystem.class);
        this.viewCameraSystem = create(ViewCameraSystem.class);
        this.treeViewerRenderSystem = create(TreeViewerRenderSystem.class);
        this.treeViewerMenuSystem = create(TreeViewerMenuSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
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

    // Management \\

    // The camera framed on one tier of the row, or on the whole row, centred on it
    public void frame(int tier) {
        treeViewerFrameSystem.frame(tier);
        viewCameraSystem.centre();
    }

    // Properties \\

    // What this tool works on is content selected in the Hierarchy, shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }
}
