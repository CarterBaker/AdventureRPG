package editor.runtime.menueventsmanager.menus;

import java.io.File;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.filemenupipeline.util.WorkspaceUtility;
import editor.bootstrap.imageeditorpipeline.imageeditormanager.ImageEditorManager;
import editor.bootstrap.materialspipeline.materialsmanager.MaterialsManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class MaterialsBranch extends BranchPackage {

    /*
     * Menu event handlers for the Materials and Material Viewer tabs.
     * Pressing a texture's row chooses it for every window that lays one and
     * picks it up, to be dropped on whichever window takes materials it is
     * let go over; pressing the open material's row chooses it. The File
     * menu makes, opens, saves, reverts, deletes, exports and imports the
     * material of the workspace, the Edit menu undoes and redoes its edits,
     * and the Properties tab's rows step, cycle, take typed values and choose a
     * map's image in the system's file dialog. Rows hand back the name or key
     * they show.
     */

    // Internal
    private MaterialsManager materialsManager;
    private ImageEditorManager imageEditorManager;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.materialsManager = get(MaterialsManager.class);
        this.imageEditorManager = get(ImageEditorManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Materials \\

    // The material is chosen on the press and carried over the windows until let go
    public void dragMaterial(String materialName) {
        materialsManager.dragMaterial(materialName);
    }

    public void chooseDocument() {
        materialsManager.chooseDocument();
    }

    // Files \\

    public void newMaterial(WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_NEW_MATERIAL,
                materialsManager::isMaterialNameAvailable,
                materialsManager::newMaterial);
    }

    public void openMaterial() {

        File file = WorkspaceUtility.chooseFile(
                EditorSetting.WORKSPACE_DIALOG_OPEN_MATERIAL,
                materialsManager.getMaterialFolder(),
                EditorSetting.WORKSPACE_FILE_PATTERNS,
                EditorSetting.WORKSPACE_FILE_DESCRIPTION);

        if (file != null)
            materialsManager.openMaterialFile(file);
    }

    public void saveMaterial() {
        materialsManager.saveMaterial();
    }

    public void revertMaterial() {
        materialsManager.revertMaterial();
    }

    public void deleteMaterial(WindowInstance window) {

        if (materialsManager.getDocument() == null)
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_DELETE_PREFIX + materialsManager.getDocument().getMaterialName()
                        + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX,
                materialsManager::isOpenMaterialName,
                confirmedName -> materialsManager.deleteMaterial());
    }

    // History \\

    public void undoMaterial() {
        materialsManager.undoMaterial();
    }

    public void redoMaterial() {
        materialsManager.redoMaterial();
    }

    // Exchange \\

    // Every map of the material written as a PNG in the workspace's Exports folder
    public void exportMaterial() {
        materialsManager.exportMaterial();
    }

    // The material's exported maps brought into the game: the pack asked for, then the texture's name, then baked
    public void importMaterial(WindowInstance window) {

        if (!materialsManager.canImport())
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.IMAGE_IMPORT_DIALOG_PACK,
                imageEditorManager.getImportPack(),
                imageEditorManager::isImportPack,
                packName -> nameDialogBranch.openText(
                        window,
                        EditorSetting.IMAGE_IMPORT_DIALOG_TILE,
                        materialsManager.getDocument().getMaterialName(),
                        imageEditorManager::isImportTileName,
                        tileName -> materialsManager.importMaterial(packName, tileName)));
    }

    // Rows \\

    public void decreaseRow(String key) {
        materialsManager.stepRow(key, -1);
    }

    public void increaseRow(String key) {
        materialsManager.stepRow(key, 1);
    }

    public void previousRow(String key) {
        materialsManager.cycleRow(key, -1);
    }

    public void nextRow(String key) {
        materialsManager.cycleRow(key, 1);
    }

    // A map's image chosen in the system's file dialog, which starts among the workspace's exported maps
    public void actRow(String key) {

        File file = WorkspaceUtility.chooseFile(
                EditorSetting.MATERIALS_DIALOG_CHOOSE_IMAGE,
                materialsManager.getImageFolder(),
                EditorSetting.MATERIALS_IMAGE_PATTERNS,
                EditorSetting.MATERIALS_IMAGE_DESCRIPTION);

        if (file != null)
            materialsManager.setRowImage(key, file);
    }

    public void editRow(String key, WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_EDIT_VALUE,
                materialsManager.getRowValueText(key),
                text -> materialsManager.isRowValueValid(key, text),
                text -> materialsManager.setRowValue(key, text));
    }
}
