package editor.bootstrap.worldeditorpipeline.worldeditormanager;

import application.bootstrap.worldpipeline.treemanager.TreeManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.UtilityPackage.InternalException;

class WorldTreeBranch extends BranchPackage {

    /*
     * Carries tree species edits from the Info Panel into the engine as they
     * are made, saved or not: an edited or reverted species is rebuilt live,
     * and every tree of it standing in the world grows again from its seed,
     * so a change shows in every window at once. An edit the engine refuses
     * leaves the species as it was and reports why, in the world editor's
     * status and the log.
     */

    // Internal
    private InfoManager infoManager;
    private TreeManager treeManager;
    private WorldEditorManager worldEditorManager;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.treeManager = get(TreeManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_TREES, this::rebuildTree);
    }

    // Live Edit \\

    private void rebuildTree(InfoEntryStruct entry) {

        try {
            treeManager.rebuildSpecies(entry.getEntryName(), entry.getArpg());
            worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_TREE_LIVE + entry.getEntryName());
        } catch (InternalException e) {
            errorLog(e.getMessage());
            worldEditorManager.setStatusMessage(EditorSetting.WORLD_EDITOR_MESSAGE_TREE_REFUSED + e.getMessage());
        }
    }
}
