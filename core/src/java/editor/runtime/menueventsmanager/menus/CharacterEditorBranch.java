package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class CharacterEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the character tabs' keys and panels, and their
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

    // Base \\

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
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

    // Modes \\

    public void showAppearance() {
        characterEditorManager.setMode(CharacterEditorMode.APPEARANCE);
    }

    public void showBuild() {
        characterEditorManager.setMode(CharacterEditorMode.BUILD);
    }

    public void showEquipment() {
        characterEditorManager.setMode(CharacterEditorMode.EQUIPMENT);
    }

    public void showMesh() {
        characterEditorManager.setMode(CharacterEditorMode.MESH);
    }

    public void showSculpt() {
        characterEditorManager.setMode(CharacterEditorMode.SCULPT);
    }

    public void showWeights() {
        characterEditorManager.setMode(CharacterEditorMode.WEIGHTS);
    }

    public void showPaint() {
        characterEditorManager.setMode(CharacterEditorMode.PAINT);
    }

    public void showRig() {
        characterEditorManager.setMode(CharacterEditorMode.RIG);
    }

    // History \\

    public void undo() {
        characterEditorManager.undo();
    }

    public void redo() {
        characterEditorManager.redo();
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
