package editor.runtime.menueventsmanager.menus;

import editor.bootstrap.entityeditorpipeline.entityeditormanager.EntityEditorManager;
import engine.root.BranchPackage;

public class EntityEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the Entity Editor's rows. Each row hands back
     * the key of what it shows: an entity to pick, its race, or one of the
     * race's genders to preview.
     */

    // Internal
    private EntityEditorManager entityEditorManager;

    // Base \\

    @Override
    protected void get() {
        this.entityEditorManager = get(EntityEditorManager.class);
    }

    // Rows \\

    public void selectRow(String key) {
        entityEditorManager.select(key);
    }
}
