package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.structureeditorpipeline.structureeditormanager.StructureEditorManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class StructureEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the Structure Editor. Palette rows carry a
     * structure's or an architecture's name, a slot, an entry of the
     * structure's own, a block, a covering or a content; New and Duplicate
     * first open the structure painted in the Hierarchy, so the Info Panel's
     * own New and Duplicate make the structure, named through the same
     * dialogs; Save and Revert act on its file, Undo and Redo on the editor's
     * own history of strokes.
     */

    // Internal
    private StructureEditorManager structureEditorManager;
    private InfoPanelBranch infoPanelBranch;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.structureEditorManager = get(StructureEditorManager.class);
        this.infoPanelBranch = get(InfoPanelBranch.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Selection \\

    public void selectRow(String key) {
        structureEditorManager.choosePaletteRow(key);
    }

    // Layers \\

    // A Layers action run from the Tools tab or the Layers tab's icons, renaming through the name dialog
    public void runLayerAction(String actionKey, WindowInstance window) {

        switch (actionKey) {
            case EditorSetting.STRUCTURE_EDITOR_KEY_LAYER_ADD -> structureEditorManager.addLayer();
            case EditorSetting.STRUCTURE_EDITOR_KEY_LAYER_REMOVE -> structureEditorManager.removeLayer();
            case EditorSetting.STRUCTURE_EDITOR_KEY_LAYER_UP -> structureEditorManager.moveLayer(1);
            case EditorSetting.STRUCTURE_EDITOR_KEY_LAYER_DOWN -> structureEditorManager.moveLayer(-1);
            case EditorSetting.STRUCTURE_EDITOR_KEY_LAYER_RENAME -> nameDialogBranch.openText(
                    window,
                    EditorSetting.DIALOG_TITLE_RENAME_LAYER,
                    structureEditorManager.getLayerName(),
                    structureEditorManager::isLayerNameAvailable,
                    structureEditorManager::renameLayer);
            default -> {
            }
        }
    }

    // File \\

    public void save() {
        structureEditorManager.save();
    }

    public void revert() {
        structureEditorManager.revert();
    }

    public void undo() {
        structureEditorManager.undo();
    }

    public void redo() {
        structureEditorManager.redo();
    }

    public void newStructure(WindowInstance window) {
        if (structureEditorManager.focusStructure())
            infoPanelBranch.newEntry(window);
    }

    public void duplicateStructure(WindowInstance window) {
        if (structureEditorManager.focusStructure())
            infoPanelBranch.copySelection(window);
    }
}
