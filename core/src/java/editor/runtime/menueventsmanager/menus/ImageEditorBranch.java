package editor.runtime.menueventsmanager.menus;

import java.io.File;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.filemenupipeline.util.WorkspaceUtility;
import editor.bootstrap.imageeditorpipeline.imageeditormanager.ImageEditorManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class ImageEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the Image Editor's File and Edit menu actions
     * and the rows of its Properties and Maps tabs. New asks for the image's
     * name, then for its size and background; a copy is named through the
     * name dialog; deleting asks for the image's name to be typed back as
     * confirmation; a number's or a name's value opens in the text dialog.
     * Panel rows hand back the key of the value they show.
     */

    // Internal
    private ImageEditorManager imageEditorManager;
    private NameDialogBranch nameDialogBranch;
    private NewImageDialogBranch newImageDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.imageEditorManager = get(ImageEditorManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.newImageDialogBranch = get(NewImageDialogBranch.class);
    }

    // Images \\

    public void newImage(WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_NEW_IMAGE,
                imageEditorManager::isImageNameAvailable,
                imageName -> newImageDialogBranch.open(window, imageName));
    }

    // An image picked in the system's open dialog, which starts in the workspace's Images folder
    public void openImage() {

        File file = WorkspaceUtility.chooseFile(
                EditorSetting.WORKSPACE_DIALOG_OPEN_IMAGE,
                imageEditorManager.getImageFolder());

        if (file != null)
            imageEditorManager.openImageFile(file);
    }

    // Every map of the image written as a PNG in the workspace's Exports folder
    public void exportImage() {
        imageEditorManager.exportImage();
    }

    // The image's exported maps brought into the game: the pack asked for, then the texture's name, then baked
    public void importImage(WindowInstance window) {

        if (!imageEditorManager.canImport())
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.IMAGE_IMPORT_DIALOG_PACK,
                imageEditorManager.getImportPack(),
                imageEditorManager::isImportPack,
                packName -> nameDialogBranch.openText(
                        window,
                        EditorSetting.IMAGE_IMPORT_DIALOG_TILE,
                        imageEditorManager.getActiveImageStem(),
                        imageEditorManager::isImportTileName,
                        tileName -> imageEditorManager.importImage(packName, tileName)));
    }

    public void copyImage(WindowInstance window) {

        if (!imageEditorManager.hasActiveImage())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_COPY_PREFIX + imageEditorManager.getActiveProject().getImageName()
                        + EditorSetting.DIALOG_TITLE_COPY_SUFFIX,
                imageEditorManager::isImageNameAvailable,
                imageEditorManager::copyImage);
    }

    public void saveImage() {
        imageEditorManager.saveImage();
    }

    public void saveAllImages() {
        imageEditorManager.saveAllImages();
    }

    public void revertImage() {
        imageEditorManager.revertImage();
    }

    public void deleteImage(WindowInstance window) {

        if (!imageEditorManager.hasActiveImage())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_DELETE_PREFIX + imageEditorManager.getActiveProject().getImageName()
                        + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX,
                imageEditorManager::isActiveImageName,
                confirmedName -> imageEditorManager.deleteImage());
    }

    // History \\

    public void undo() {
        imageEditorManager.undo();
    }

    public void redo() {
        imageEditorManager.redo();
    }

    // Rows \\

    public void selectRow(String key) {
        imageEditorManager.select(key);
    }

    public void decreaseRow(String key) {
        imageEditorManager.step(key, -1);
    }

    public void increaseRow(String key) {
        imageEditorManager.step(key, 1);
    }

    public void editRow(String key, WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_EDIT_VALUE,
                imageEditorManager.getValueText(key),
                text -> imageEditorManager.isValueValid(key, text),
                text -> imageEditorManager.setValue(key, text));
    }

    public void previousRow(String key) {
        imageEditorManager.cycle(key, -1);
    }

    public void nextRow(String key) {
        imageEditorManager.cycle(key, 1);
    }

    public void actRow(String key) {
        imageEditorManager.act(key);
    }
}
