package editor.bootstrap.treeplannerpipeline;

import editor.bootstrap.treeplannerpipeline.treeplannermanager.TreePlannerManager;
import engine.root.PipelinePackage;

public class TreePlannerPipeline extends PipelinePackage {

    /*
     * Registers the tree planner, which lays a tree species out as the
     * generator it grows by and steps its fields, and whose row of the
     * species drawn at every detail the Tree Viewer shows, so a tree is seen
     * near and far while it is made.
     */

    // Internal \\

    @Override
    protected void create() {
        create(TreePlannerManager.class);
    }
}
