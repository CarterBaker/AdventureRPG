package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.faceeditorpipeline.faceeditormanager.FaceEditorManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class FaceEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the Face Editor toolbar and the Face Outliner
     * and Face Properties panels. Every action works on the face of the
     * character the Character Editor has open. Save and Revert keep the
     * template's face, meshes and rig together; undo and redo step the face's
     * history. Panel rows hand back the key of the value they show, and a
     * number's or a name's value opens in the text dialog.
     */

    // Internal
    private FaceEditorManager faceEditorManager;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.faceEditorManager = get(FaceEditorManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Template \\

    public void saveCharacter() {
        faceEditorManager.saveActiveTemplate();
    }

    public void revertCharacter() {
        faceEditorManager.revertActiveTemplate();
    }

    // History \\

    public void undo() {
        faceEditorManager.undo();
    }

    public void redo() {
        faceEditorManager.redo();
    }

    // Rows \\

    public void selectRow(String key) {
        faceEditorManager.select(key);
    }

    public void decreaseRow(String key) {
        faceEditorManager.step(key, -1);
    }

    public void increaseRow(String key) {
        faceEditorManager.step(key, 1);
    }

    public void editRow(String key, WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_EDIT_VALUE,
                faceEditorManager.getValueText(key),
                text -> faceEditorManager.isValueValid(key, text),
                text -> faceEditorManager.setValue(key, text));
    }

    public void previousRow(String key) {
        faceEditorManager.cycle(key, -1);
    }

    public void nextRow(String key) {
        faceEditorManager.cycle(key, 1);
    }

    public void actRow(String key) {
        faceEditorManager.act(key);
    }
}
