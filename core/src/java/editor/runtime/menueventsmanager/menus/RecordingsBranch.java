package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.animationeditorpipeline.animationeditormanager.AnimationEditorManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class RecordingsBranch extends BranchPackage {

    /*
     * Menu event handlers for the Recordings tab. Every action works on the
     * one recorder all tabs share. A row is picked up and dragged through
     * RecorderBranch, which opens the recording it names when it is let go
     * where it was pressed and nests it when it is let go on the Timeline.
     * New asks for the media file's name; renamed and copied ones are named
     * through the name dialog too, and
     * deleting the open recording asks for its name to be typed back. Rows
     * hand back the name of the recording they show; while the tab lists
     * animations, a row clicked opens its clip on the Timeline and in the
     * Animation Editor.
     */

    // Internal
    private RecorderManager recorderManager;
    private AnimationEditorManager animationEditorManager;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.animationEditorManager = get(AnimationEditorManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Recordings \\

    // A new media file of the name typed: video or music, as what is laid on it makes it
    public void newRecording(WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_NEW_RECORDING,
                recorderManager::isRecordingNameAvailable,
                recorderManager::createRecording);
    }

    public void openAnimation(String clipName) {
        animationEditorManager.openClip(clipName);
    }

    public void saveRecording() {
        recorderManager.saveRecording();
    }

    public void undo() {
        recorderManager.undo();
    }

    public void redo() {
        recorderManager.redo();
    }

    public void renameRecording(WindowInstance window) {

        if (!recorderManager.hasRecording())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_RENAME_RECORDING,
                recorderManager::isRecordingNameAvailable,
                recorderManager::renameRecording);
    }

    public void duplicateRecording(WindowInstance window) {

        if (!recorderManager.hasRecording())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_DUPLICATE_RECORDING,
                recorderManager::isRecordingNameAvailable,
                recorderManager::duplicateRecording);
    }

    public void deleteRecording(WindowInstance window) {

        if (!recorderManager.hasRecording())
            return;

        String recordingName = recorderManager.getRecording().getRecordingName();

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_DELETE_PREFIX + recordingName + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX,
                recordingName::equals,
                confirmedName -> recorderManager.deleteRecording());
    }
}
