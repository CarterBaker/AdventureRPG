package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.bootstrap.treeplannerpipeline.treeplanner.TreePlannerNode;
import editor.bootstrap.treeplannerpipeline.treeplanner.TreePlannerParameter;
import editor.bootstrap.treeplannerpipeline.treeplannermanager.TreePlannerManager;
import editor.runtime.treeplanner.TreePlannerSetting;
import engine.root.BranchPackage;

public class TreePlannerBranch extends BranchPackage {

    /*
     * Menu event handlers for the Tree Planner. Tree rows carry the species'
     * name, nodes the node's and step buttons the field's. New and Duplicate
     * first open the selected tree in the Hierarchy, so the Info Panel's own
     * New and Duplicate make the tree, named through the same dialogs; Open
     * Viewer opens a Tree Viewer beside the planner.
     */

    // Internal
    private TreePlannerManager treePlannerManager;
    private InfoPanelBranch infoPanelBranch;
    private TabManager tabManager;

    // Base \\

    @Override
    protected void get() {
        this.treePlannerManager = get(TreePlannerManager.class);
        this.infoPanelBranch = get(InfoPanelBranch.class);
        this.tabManager = get(TabManager.class);
    }

    // Selection \\

    public void selectTree(String treeName) {
        treePlannerManager.selectTree(treeName);
    }

    public void selectNode(String node) {
        treePlannerManager.selectNode(TreePlannerNode.valueOf(node));
    }

    // Fields \\

    public void lowerValue(String parameter) {
        treePlannerManager.step(TreePlannerParameter.valueOf(parameter), TreePlannerSetting.STEP_DOWN);
    }

    public void raiseValue(String parameter) {
        treePlannerManager.step(TreePlannerParameter.valueOf(parameter), TreePlannerSetting.STEP_UP);
    }

    // Preview \\

    public void reseed() {
        treePlannerManager.reseed();
    }

    public void younger() {
        treePlannerManager.stepAge(TreePlannerSetting.STEP_DOWN);
    }

    public void older() {
        treePlannerManager.stepAge(TreePlannerSetting.STEP_UP);
    }

    // File \\

    public void save() {
        treePlannerManager.save();
    }

    public void revert() {
        treePlannerManager.revert();
    }

    // Trees \\

    public void newTree(WindowInstance window) {
        if (treePlannerManager.focusTree())
            infoPanelBranch.newEntry(window);
    }

    public void duplicateTree(WindowInstance window) {
        if (treePlannerManager.focusTree())
            infoPanelBranch.copySelection(window);
    }

    // Viewer \\

    public void openViewer(WindowInstance window) {
        tabManager.openTreeViewer(window.getGLWindow());
    }
}
