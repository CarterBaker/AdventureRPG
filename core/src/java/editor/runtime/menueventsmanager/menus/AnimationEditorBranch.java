package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.animationeditorpipeline.animationeditormanager.AnimationEditorManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class AnimationEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the Animation Editor's keys and panels, and its
     * File and Edit menu actions. Every action works on the shared active
     * clip. New clips are named through the name dialog, deleting a clip
     * asks for its name to be typed back as confirmation, and a number's
     * value opens in the text dialog. Undo and redo step the history of the
     * recorder's timeline, which holds the clip, and the transport keys play
     * and step its playhead. Panel rows hand back the key of the value they
     * show.
     */

    // Internal
    private AnimationEditorManager animationEditorManager;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.animationEditorManager = get(AnimationEditorManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Clips \\

    public void newClip(WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_NEW_CLIP,
                animationEditorManager::isClipNameAvailable,
                animationEditorManager::createClip);
    }

    public void saveClip() {
        animationEditorManager.saveActiveClip();
    }

    public void revertClip() {
        animationEditorManager.revertActiveClip();
    }

    public void deleteClip(WindowInstance window) {

        if (!animationEditorManager.hasActiveDocument())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_DELETE_PREFIX + animationEditorManager.getActiveLocalName()
                        + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX,
                animationEditorManager::isActiveClipName,
                confirmedName -> animationEditorManager.deleteActiveClip());
    }

    // History \\

    public void undo() {
        animationEditorManager.undo();
    }

    public void redo() {
        animationEditorManager.redo();
    }

    // Playback \\

    public void togglePlayback() {
        animationEditorManager.togglePlayback();
    }

    public void toStart() {
        animationEditorManager.toStart();
    }

    public void previousFrame() {
        animationEditorManager.stepFrame(-1);
    }

    public void nextFrame() {
        animationEditorManager.stepFrame(1);
    }

    public void toEnd() {
        animationEditorManager.toEnd();
    }

    // Rows \\

    public void selectRow(String key) {
        animationEditorManager.select(key);
    }

    public void decreaseRow(String key) {
        animationEditorManager.step(key, -1);
    }

    public void increaseRow(String key) {
        animationEditorManager.step(key, 1);
    }

    public void editRow(String key, WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_EDIT_VALUE,
                animationEditorManager.getValueText(key),
                text -> animationEditorManager.isValueValid(key, text),
                text -> animationEditorManager.setValue(key, text));
    }

    public void previousRow(String key) {
        animationEditorManager.cycle(key, -1);
    }

    public void nextRow(String key) {
        animationEditorManager.cycle(key, 1);
    }

    public void actRow(String key) {
        animationEditorManager.act(key);
    }
}
