package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.treeviewerpipeline.treeviewer.TreePlannerNode;
import editor.bootstrap.treeviewerpipeline.treeviewermanager.TreeViewerManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class TreeViewerBranch extends BranchPackage {

    /*
     * Menu event handlers for the Tree Planner and Tree Viewer. Palette rows
     * carry the species' name, graph nodes the node's and Properties rows the
     * field's; a value is typed through the name dialog into the same setter
     * the slider and step buttons use. New and Duplicate first open the
     * selected tree in the Hierarchy, so the Info Panel's own New and
     * Duplicate make the tree, named through the same dialogs; Save and
     * Revert act on the selected species' file.
     */

    // Internal
    private TreeViewerManager treeViewerManager;
    private InfoPanelBranch infoPanelBranch;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.treeViewerManager = get(TreeViewerManager.class);
        this.infoPanelBranch = get(InfoPanelBranch.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Selection \\

    public void selectRow(String key) {
        treeViewerManager.selectTree(key);
    }

    public void selectNode(String node) {
        treeViewerManager.selectNode(TreePlannerNode.valueOf(node));
    }

    // Fields \\

    public void decreaseRow(String key) {
        treeViewerManager.stepField(key, EditorSetting.TREE_VIEWER_STEP_DOWN);
    }

    public void increaseRow(String key) {
        treeViewerManager.stepField(key, EditorSetting.TREE_VIEWER_STEP_UP);
    }

    public void editRow(String key, WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_EDIT_VALUE,
                treeViewerManager.getFieldText(key),
                text -> treeViewerManager.isFieldValid(key, text),
                text -> treeViewerManager.setField(key, text));
    }

    public void previousRow(String key) {
        treeViewerManager.stepField(key, EditorSetting.TREE_VIEWER_STEP_DOWN);
    }

    public void nextRow(String key) {
        treeViewerManager.stepField(key, EditorSetting.TREE_VIEWER_STEP_UP);
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
