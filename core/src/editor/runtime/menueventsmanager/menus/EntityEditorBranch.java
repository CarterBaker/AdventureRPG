package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.entityeditorpipeline.entityeditormanager.EntityEditorManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class EntityEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the Entity Editor's rows. Each row hands back
     * the key of what it shows: an entity to pick, a gender to preview, a
     * choice to step or type, or an action. A new entity and a copied race
     * are named in the name dialog, and an entity or race is deleted only
     * once its name is typed; every other action applies at once.
     */

    // Internal
    private EntityEditorManager entityEditorManager;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.entityEditorManager = get(EntityEditorManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Rows \\

    public void selectRow(String key) {
        entityEditorManager.select(key);
    }

    public void previousRow(String key) {
        entityEditorManager.cycle(key, -1);
    }

    public void nextRow(String key) {
        entityEditorManager.cycle(key, 1);
    }

    public void editRow(String key, WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_EDIT_VALUE,
                entityEditorManager.getValueText(key),
                text -> entityEditorManager.isValueValid(key, text),
                text -> entityEditorManager.setValue(key, text));
    }

    public void actRow(String key, WindowInstance window) {
        switch (key) {
            case EditorSetting.ENTITY_EDITOR_KEY_NEW_ENTITY -> nameDialogBranch.open(
                    window,
                    EditorSetting.ENTITY_EDITOR_DIALOG_NEW_ENTITY,
                    entityEditorManager::isEntityNameAvailable,
                    entityEditorManager::createEntity);
            case EditorSetting.ENTITY_EDITOR_KEY_DELETE_ENTITY -> nameDialogBranch.open(
                    window,
                    toDeleteTitle(entityEditorManager.getActiveEntityName()),
                    entityEditorManager::isActiveEntityName,
                    confirmedName -> entityEditorManager.deleteActiveEntity());
            case EditorSetting.ENTITY_EDITOR_KEY_COPY_RACE -> nameDialogBranch.open(
                    window,
                    EditorSetting.ENTITY_EDITOR_DIALOG_COPY_RACE,
                    entityEditorManager::isRaceNameAvailable,
                    entityEditorManager::copyRace);
            case EditorSetting.ENTITY_EDITOR_KEY_DELETE_RACE -> nameDialogBranch.open(
                    window,
                    toDeleteTitle(entityEditorManager.getActiveRaceName()),
                    entityEditorManager::isActiveRaceName,
                    confirmedName -> entityEditorManager.deleteActiveRace());
            default -> entityEditorManager.act(key);
        }
    }

    private String toDeleteTitle(String name) {
        return EditorSetting.DIALOG_TITLE_DELETE_PREFIX + name + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX;
    }
}
