package editor.runtime.treeviewer;

import editor.runtime.input.EditorInputSystem;
import editor.runtime.treeviewer.camera.TreeViewerCameraSystem;
import editor.runtime.treeviewer.menu.TreeViewerMenuSystem;
import editor.runtime.treeviewer.render.TreeViewerRenderSystem;
import engine.root.ContextPackage;

public class TreeViewerContext extends ContextPackage {

    /*
     * Editor tab showing the Tree Planner's species as the world draws it, at
     * every detail side by side: in full, as a model, a sketch and an
     * outline, grown from the planner's seed to its age and lit by one fixed
     * light. The orbit button turns the view and scrolling zooms; the
     * toolbar frames the whole row or one tier. Every change in the planner
     * or the Info Panel shows here as soon as the row is built again.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private TreeViewerCameraSystem treeViewerCameraSystem;
    private TreeViewerRenderSystem treeViewerRenderSystem;
    private TreeViewerMenuSystem treeViewerMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.treeViewerCameraSystem = create(TreeViewerCameraSystem.class);
        this.treeViewerRenderSystem = create(TreeViewerRenderSystem.class);
        this.treeViewerMenuSystem = create(TreeViewerMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Management \\

    // The camera framed on one tier of the row, or on the whole row
    public void frame(int tier) {
        treeViewerCameraSystem.frame(tier);
    }
}
