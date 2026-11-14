package application.bootstrap.menupipeline.hierarchymanager;

import application.bootstrap.menupipeline.menu.MenuInstance;
import engine.root.BranchPackage;

public class HierarchyBranch extends BranchPackage {

    /*
     * Menu event handlers for hierarchy panels. The clicked menu identifies the
     * panel and the argument is the tab name or node key.
     */

    // Internal
    private HierarchyManager hierarchyManager;

    // Base \\

    @Override
    protected void get() {
        this.hierarchyManager = get(HierarchyManager.class);
    }

    // Events \\

    public void selectTab(String tabName, MenuInstance menu) {
        hierarchyManager.selectTab(menu, tabName);
    }

    public void selectNode(String nodeKey, MenuInstance menu) {
        hierarchyManager.selectNode(menu, nodeKey);
    }

    public void toggleNode(String nodeKey, MenuInstance menu) {
        hierarchyManager.toggleNode(menu, nodeKey);
    }
}
