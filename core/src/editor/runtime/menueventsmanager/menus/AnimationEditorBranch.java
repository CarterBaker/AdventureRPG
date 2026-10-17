package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.animationeditorpipeline.animationeditormanager.AnimationEditorManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class AnimationEditorBranch extends BranchPackage {

    /*
     * Menu event handlers for the Animation Editor toolbar, timeline and
     * panels. Every action works on the shared active clip. New clips are
     * named through the name dialog, deleting a clip asks for its name to be
     * typed back as confirmation, and a number's value opens in the text
     * dialog. Dragging along the timeline moves the playhead to the frame
     * under the cursor. Panel rows hand back the key of the value they show.
     */

    // Internal
    private AnimationEditorManager animationEditorManager;
    private NameDialogBranch nameDialogBranch;
    private InputManager inputManager;

    // Base \\

    @Override
    protected void get() {
        this.animationEditorManager = get(AnimationEditorManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.inputManager = get(InputManager.class);
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

    public void scrubTimeline(WindowInstance window, ElementInstance track) {

        if (track.getComputedW() > 0f)
            animationEditorManager.scrub(
                    (inputManager.getHoverMouseX(window) - track.getComputedLeft()) / track.getComputedW());
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
