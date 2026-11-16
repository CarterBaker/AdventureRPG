package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterModelBranch;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterStampBranch;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class CharacterEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the Character Editor's keys and panels, and its
     * File and Edit menu actions. Every
     * action works on the shared active character. A copy is named through
     * the name dialog, deleting a character asks for its name to be typed
     * back as confirmation, and a number's or a name's value opens in the
     * text dialog. Undo and redo step the history of what the mode edits,
     * the rig in Rig mode and the mesh being edited otherwise. Panel rows
     * hand back the key of the value they show.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private NameDialogBranch nameDialogBranch;
    private GenerateMapsDialogBranch generateMapsDialogBranch;
    private CharacterModelBranch characterModelBranch;
    private CharacterStampBranch characterStampBranch;

    // Base \\

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.generateMapsDialogBranch = get(GenerateMapsDialogBranch.class);
        this.characterModelBranch = get(CharacterModelBranch.class);
        this.characterStampBranch = get(CharacterStampBranch.class);
    }

    // Characters \\

    public void copyCharacter(WindowInstance window) {

        if (!characterEditorManager.hasActiveDocument())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_COPY_PREFIX + characterEditorManager.getActiveLocalName()
                        + EditorSetting.DIALOG_TITLE_COPY_SUFFIX,
                characterEditorManager::isTemplateNameAvailable,
                characterEditorManager::copyActiveTemplate);
    }

    public void saveCharacter() {
        characterEditorManager.saveActiveTemplate();
    }

    public void revertCharacter() {
        characterEditorManager.revertActiveTemplate();
    }

    public void deleteCharacter(WindowInstance window) {

        if (!characterEditorManager.hasActiveDocument())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_DELETE_PREFIX + characterEditorManager.getActiveLocalName()
                        + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX,
                characterEditorManager::isActiveTemplateName,
                confirmedName -> characterEditorManager.deleteActiveTemplate());
    }

    // History \\

    public void undo() {
        characterEditorManager.undo();
    }

    public void redo() {
        characterEditorManager.redo();
    }

    // Models \\

    public void exportModel() {
        characterModelBranch.exportModel();
    }

    // Opens the Generate Maps dialog on the albedo of the part painted
    public void generateMaps(WindowInstance window) {
        generateMapsDialogBranch.open(window);
    }

    // Asks for the new stamp's name, then cuts the selected faces of the open part into it
    public void saveStamp(WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_SAVE_STAMP,
                characterStampBranch::isStampNameAvailable,
                characterStampBranch::saveSelection);
    }

    // Asks for the .glb's name in the Models folder, then for whatever the model leaves open, one dialog at a time
    public void importModel(WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.MODEL_DIALOG_FILE + characterModelBranch.getModelDirectory(),
                "",
                characterModelBranch::isModelFile,
                name -> {
                    characterModelBranch.beginImport(name);
                    continueImport(window);
                });
    }

    private void continueImport(WindowInstance window) {

        if (!characterModelBranch.isImporting())
            return;

        if (characterModelBranch.hasMissingBones()) {
            nameDialogBranch.openText(
                    window,
                    EditorSetting.MODEL_DIALOG_ADD_BONES_PREFIX + characterModelBranch.describeMissingBones()
                            + EditorSetting.MODEL_DIALOG_ADD_BONES_SUFFIX,
                    "",
                    EditorSetting.MODEL_CONFIRM_ADD_BONES::equalsIgnoreCase,
                    confirmed -> characterModelBranch.addMissingBones());
            return;
        }

        if (characterModelBranch.needsHangBone()) {
            nameDialogBranch.openText(
                    window,
                    EditorSetting.MODEL_DIALOG_HANG_BONE,
                    "",
                    characterModelBranch::isBone,
                    boneName -> {
                        characterModelBranch.setHangBone(boneName);
                        continueImport(window);
                    });
            return;
        }

        if (characterModelBranch.needsTexture()) {
            nameDialogBranch.openText(
                    window,
                    EditorSetting.MODEL_DIALOG_TEXTURE_PREFIX + characterModelBranch.getOpenMaterialName(),
                    "",
                    characterModelBranch::isTexture,
                    textureName -> {
                        characterModelBranch.setTexture(textureName);
                        continueImport(window);
                    });
            return;
        }

        if (characterModelBranch.needsZone()) {
            nameDialogBranch.openText(
                    window,
                    EditorSetting.MODEL_DIALOG_ZONE_PREFIX + characterModelBranch.getOpenMaterialName(),
                    "",
                    characterModelBranch::isZone,
                    zoneName -> {
                        characterModelBranch.setZone(zoneName);
                        continueImport(window);
                    });
            return;
        }

        characterModelBranch.finishImport();
    }

    // Rows \\

    public void selectRow(String key) {
        characterEditorManager.select(key);
    }

    public void decreaseRow(String key) {
        characterEditorManager.step(key, -1);
    }

    public void increaseRow(String key) {
        characterEditorManager.step(key, 1);
    }

    public void editRow(String key, WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_EDIT_VALUE,
                characterEditorManager.getValueText(key),
                text -> characterEditorManager.isValueValid(key, text),
                text -> characterEditorManager.setValue(key, text));
    }

    public void previousRow(String key) {
        characterEditorManager.cycle(key, -1);
    }

    public void nextRow(String key) {
        characterEditorManager.cycle(key, 1);
    }

    public void actRow(String key) {
        characterEditorManager.act(key);
    }
}
