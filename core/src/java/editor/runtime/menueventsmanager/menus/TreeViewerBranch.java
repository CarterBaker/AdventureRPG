package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.treeviewerpipeline.treeviewermanager.TreeViewerManager;
import engine.root.BranchPackage;

public class TreeViewerBranch extends BranchPackage {

    /*
     * The Tree Viewer's File menu methods. New and Duplicate first open the
     * selected tree in the Hierarchy, so the Info Panel's own New and
     * Duplicate make the tree, named through the same dialogs; Save and
     * Revert act on the selected species' file.
     */

    // Internal
    private TreeViewerManager treeViewerManager;
    private InfoPanelBranch infoPanelBranch;

    // Base \\

    @Override
    protected void get() {
        this.treeViewerManager = get(TreeViewerManager.class);
        this.infoPanelBranch = get(InfoPanelBranch.class);
    }

    // File \\

    public void save() {
        treeViewerManager.save();
    }

    public void revert() {
        treeViewerManager.revert();
    }

    // Trees \\

    public void newTree(WindowInstance window) {
        if (treeViewerManager.focusTree())
            infoPanelBranch.newEntry(window);
    }

    public void duplicateTree(WindowInstance window) {
        if (treeViewerManager.focusTree())
            infoPanelBranch.copySelection(window);
    }
}
