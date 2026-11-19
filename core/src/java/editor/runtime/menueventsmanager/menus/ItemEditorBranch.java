package editor.runtime.menueventsmanager.menus;

import java.io.File;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.filemenupipeline.util.WorkspaceUtility;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class ItemEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the Item Editor's keys and its File menu
     * actions. Every action works on the shared active item; new workspace
     * items and part renames are named through the name dialog, Open picks a
     * workspace item in the system's open dialog, Import asks the definition
     * file the item joins, Export bakes it for the mesh tools, and deleting an
     * item asks for its name to be typed
     * back as confirmation. Parts are added, picked and deleted through the
     * Layers tab, and the tools are chosen through ToolBranch, as every
     * editor's are.
     */

    // Internal
    private ItemEditorManager itemEditorManager;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.itemEditorManager = get(ItemEditorManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Items \\

    public void newItem(WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_NEW_ITEM,
                itemEditorManager::isItemNameAvailable,
                itemEditorManager::createItem);
    }

    // A workspace item picked in the system's open dialog, which starts in the workspace's Items folder
    public void openItem() {

        File file = WorkspaceUtility.chooseFile(
                EditorSetting.WORKSPACE_DIALOG_OPEN_ITEM,
                itemEditorManager.getItemFolder(),
                EditorSetting.WORKSPACE_FILE_PATTERNS,
                EditorSetting.WORKSPACE_FILE_DESCRIPTION);

        if (file != null)
            itemEditorManager.openItemFile(file);
    }

    public void saveItem() {
        itemEditorManager.saveActiveItem();
    }

    public void reloadItem() {
        itemEditorManager.reloadActiveItem();
    }

    // The open workspace item brought into the game, joining the definition file asked for
    public void importItem(WindowInstance window) {

        if (!itemEditorManager.canImport())
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.ITEM_IMPORT_DIALOG_DEFINITION,
                EditorSetting.ITEM_EDITOR_DEFINITION_FILE,
                itemEditorManager::isImportDefinition,
                itemEditorManager::importActiveItem);
    }

    // The item baked for the mesh tools and opened in the character tabs
    public void exportItem() {
        itemEditorManager.exportActiveItem();
    }

    public void deleteItem(WindowInstance window) {

        if (!itemEditorManager.hasActiveDocument())
            return;

        String localName = itemEditorManager.getActiveDocument().getLocalName();

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_DELETE_PREFIX + localName + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX,
                itemEditorManager::isActiveItemName,
                confirmedName -> itemEditorManager.deleteActiveItem());
    }

    // Parts \\

    public void removePart() {
        itemEditorManager.removeSelectedPart();
    }

    public void renamePart(WindowInstance window) {

        if (!itemEditorManager.hasActiveDocument())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_RENAME_PART,
                itemEditorManager::isPartNameAvailable,
                itemEditorManager::renameSelectedPart);
    }
}
