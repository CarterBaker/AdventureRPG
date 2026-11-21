package editor.runtime.menueventsmanager.menus;

import java.io.File;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterModelBranch;
import editor.bootstrap.filemenupipeline.util.WorkspaceUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.util.io.FileUtility;

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
    private CharacterModelBranch characterModelBranch;

    // Base \\

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.characterModelBranch = get(CharacterModelBranch.class);
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

    // A blank model made in the workspace: its name asked, then the game rig it is built on
    public void newModel(WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.CHARACTER_MODEL_DIALOG_NEW,
                characterEditorManager::isModelNameAvailable,
                modelName -> nameDialogBranch.openText(
                        window,
                        EditorSetting.CHARACTER_MODEL_DIALOG_RIG,
                        EditorSetting.CHARACTER_MODEL_DEFAULT_RIG,
                        characterEditorManager::isModelRig,
                        rigName -> characterEditorManager.newModel(modelName, rigName)));
    }

    // A file picked in the system's open dialog: a workspace model opens in every character tab, a .glb is brought
    // into the mesh being edited, whatever it leaves open asked one dialog at a time
    public void openModel(WindowInstance window) {

        File file = WorkspaceUtility.chooseFile(
                EditorSetting.CHARACTER_MODEL_DIALOG_OPEN,
                characterEditorManager.getModelFolder(),
                EditorSetting.WORKSPACE_MODEL_PATTERNS,
                EditorSetting.WORKSPACE_MODEL_DESCRIPTION);

        if (file == null)
            return;

        if (!FileUtility.hasExtension(file, EditorSetting.GLTF_FILE_EXTENSION)) {
            characterEditorManager.openModelFile(file);
            return;
        }

        characterModelBranch.beginImport(file);
        continueImport(window);
    }

    // The open model brought into the game: the race it joins, the gender it is the model of, then an entity to make;
    // or the item it is worn as, then the gender whose body it fits and the side it is worn on
    public void importModel(WindowInstance window) {

        if (!characterEditorManager.canImportModel())
            return;

        if (characterEditorManager.isItemModel()) {
            nameDialogBranch.openText(
                    window,
                    EditorSetting.CHARACTER_MODEL_DIALOG_ITEM,
                    characterEditorManager.getImportItemName(),
                    characterEditorManager::isImportItem,
                    characterEditorManager::importItemModel);
            return;
        }

        nameDialogBranch.openText(
                window,
                EditorSetting.CHARACTER_MODEL_DIALOG_RACE,
                characterEditorManager.getActiveModelStem(),
                name -> characterEditorManager.isImportWorn(name) || characterEditorManager.isImportRace(name),
                name -> {
                    if (characterEditorManager.isImportWorn(name))
                        importWornModel(window, name);
                    else
                        importRaceModel(window, name);
                });
    }

    private void importRaceModel(WindowInstance window, String raceName) {
        nameDialogBranch.openText(
                window,
                EditorSetting.CHARACTER_MODEL_DIALOG_GENDER,
                "",
                characterEditorManager::isImportGender,
                genderName -> nameDialogBranch.openText(
                        window,
                        EditorSetting.CHARACTER_MODEL_DIALOG_ENTITY,
                        "",
                        characterEditorManager::isImportEntity,
                        entityName -> characterEditorManager.importModel(
                                raceName,
                                genderName,
                                entityName)));
    }

    private void importWornModel(WindowInstance window, String itemName) {
        nameDialogBranch.openText(
                window,
                EditorSetting.CHARACTER_MODEL_DIALOG_WORN_GENDER,
                "",
                characterEditorManager::isImportWornGender,
                genderName -> nameDialogBranch.openText(
                        window,
                        EditorSetting.CHARACTER_MODEL_DIALOG_WORN_SIDE,
                        "",
                        characterEditorManager::isImportWornSide,
                        sideName -> characterEditorManager.importWornModel(itemName, genderName, sideName)));
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

    // Hair \\

    // A groom made in the workspace's Hair folder: its name asked, then the game rig it is worn on
    public void newHair(WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.CHARACTER_HAIR_DIALOG_NEW,
                characterEditorManager::isHairNameAvailable,
                hairName -> nameDialogBranch.openText(
                        window,
                        EditorSetting.CHARACTER_HAIR_DIALOG_RIG,
                        EditorSetting.CHARACTER_MODEL_DEFAULT_RIG,
                        characterEditorManager::isModelRig,
                        rigName -> characterEditorManager.newHair(hairName, rigName)));
    }

    // A groom picked in the system's open dialog from the Hair folder
    public void openHair() {

        File file = WorkspaceUtility.chooseFile(
                EditorSetting.CHARACTER_HAIR_DIALOG_OPEN,
                characterEditorManager.getHairFolder(),
                EditorSetting.WORKSPACE_FILE_PATTERNS,
                EditorSetting.WORKSPACE_FILE_DESCRIPTION);

        if (file != null)
            characterEditorManager.openHairFile(file);
    }

    public void saveHair() {
        characterEditorManager.saveHair();
    }

    public void revertHair() {
        characterEditorManager.revertHair();
    }

    public void undoHair() {
        characterEditorManager.undoHair();
    }

    public void redoHair() {
        characterEditorManager.redoHair();
    }

    // The groom's cards written to the workspace's Exports, the files Import brings into the game
    public void exportHair() {
        characterEditorManager.exportHair();
    }

    // An export picked from Exports/Hair brought into the game: the hair feature it becomes, then whether its long
    // strands get spring chains and weights
    public void importHair(WindowInstance window) {

        File file = WorkspaceUtility.chooseFile(
                EditorSetting.CHARACTER_HAIR_DIALOG_IMPORT,
                characterEditorManager.getHairExportFolder(),
                EditorSetting.WORKSPACE_FILE_PATTERNS,
                EditorSetting.WORKSPACE_FILE_DESCRIPTION);

        if (file == null)
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.CHARACTER_HAIR_DIALOG_FEATURE,
                characterEditorManager.getHairFeatureDefault(),
                characterEditorManager::isHairFeatureName,
                featureName -> nameDialogBranch.openText(
                        window,
                        EditorSetting.CHARACTER_HAIR_DIALOG_SPRINGS,
                        EditorSetting.CHARACTER_HAIR_ANSWER_YES,
                        this::isAnswer,
                        answer -> characterEditorManager.importHair(
                                file,
                                featureName,
                                answer.equalsIgnoreCase(EditorSetting.CHARACTER_HAIR_ANSWER_YES))));
    }

    private boolean isAnswer(String text) {
        return text.equalsIgnoreCase(EditorSetting.CHARACTER_HAIR_ANSWER_YES)
                || text.equalsIgnoreCase(EditorSetting.CHARACTER_HAIR_ANSWER_NO);
    }

    // Rows \\

    public void selectRow(String key) {
        characterEditorManager.select(key);
    }

    public void toggleRowEye(String key) {
        characterEditorManager.toggleRowEye(key);
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
