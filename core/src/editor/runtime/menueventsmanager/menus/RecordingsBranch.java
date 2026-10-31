package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;

public class RecordingsBranch extends BranchPackage {

    /*
     * Menu event handlers for the Recordings tab. Every action works on the
     * one recorder all tabs share. A row opens the recording it names. New,
     * renamed and copied recordings are named through the name dialog, and
     * deleting the open recording asks for its name to be typed back. Rows
     * hand back the name of the recording they show.
     */

    // Internal
    private RecorderManager recorderManager;
    private NameDialogBranch nameDialogBranch;

    // Base \\

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    // Recordings \\

    public void newRecording(WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_NEW_RECORDING,
                recorderManager::isRecordingNameAvailable,
                recorderManager::createRecording);
    }

    public void openRecording(String recordingName) {
        recorderManager.openRecording(recordingName);
    }

    public void saveRecording() {
        recorderManager.saveRecording();
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
