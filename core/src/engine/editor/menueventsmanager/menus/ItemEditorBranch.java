package engine.editor.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.bootstrap.itemeditorpipeline.util.ItemEditorTool;
import engine.editor.EditorSetting;
import engine.root.BranchPackage;

public class ItemEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the Item Editor toolbar. Every action works on the
     * shared active item; new items and parts are named through the name dialog.
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

    public void saveItem() {
        itemEditorManager.saveActiveItem();
    }

    public void reloadItem() {
        itemEditorManager.reloadActiveItem();
    }

    // Tools \\

    public void selectPlaceTool() {
        itemEditorManager.setTool(ItemEditorTool.PLACE);
    }

    public void selectEraseTool() {
        itemEditorManager.setTool(ItemEditorTool.ERASE);
    }

    public void selectPaintTool() {
        itemEditorManager.setTool(ItemEditorTool.PAINT);
    }

    // Parts \\

    public void addPart(WindowInstance window) {

        if (!itemEditorManager.hasActiveDocument())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_NEW_PART,
                itemEditorManager::isPartNameAvailable,
                itemEditorManager::addPart);
    }

    public void removePart() {
        itemEditorManager.removeSelectedPart();
    }

    public void previousTexture() {
        itemEditorManager.cycleTexture(-1);
    }

    public void nextTexture() {
        itemEditorManager.cycleTexture(1);
    }
}
