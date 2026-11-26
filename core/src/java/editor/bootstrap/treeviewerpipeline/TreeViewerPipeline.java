package editor.bootstrap.treeviewerpipeline;

import editor.bootstrap.treeviewerpipeline.treeviewermanager.TreeViewerManager;
import engine.root.PipelinePackage;

public class TreeViewerPipeline extends PipelinePackage {

    /*
     * Registers the Tree Viewer's state: the species picked in the Hierarchy,
     * the seed and age its preview grows from, and the preview of it at every
     * LOD the world draws trees at, beside the shared tree its LOD seed
     * grows, so a tree is seen near and far while it is made.
     */

    // Internal \\

    @Override
    protected void create() {
        create(TreeViewerManager.class);
    }
}
