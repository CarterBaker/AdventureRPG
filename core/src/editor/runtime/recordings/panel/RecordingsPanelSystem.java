package editor.runtime.recordings.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderClipEdge;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.runtime.EditorSetting;
import editor.runtime.recordings.RecordingsSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingsPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and shows the recordings. When
     * the recorder's revision moves it refills the list — a row for every
     * recording saved in the game's directory, most recently changed first,
     * the open one marked — what the open recording holds, and the status
     * line, which follows the recorder's.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private RecorderManager recorderManager;

    // Menus
    private MenuInstance recordingsMenu;
    private ElementInstance statusLabel;
    private ElementInstance listTitle;

    // State
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // State
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.recorderManager = get(RecorderManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.recordingsMenu = menuManager.openMenu(EditorSetting.RECORDINGS_MENU, window);

        this.statusLabel = requireEntryPoint(RecordingsSetting.ENTRY_STATUS);
        this.listTitle = requireEntryPoint(RecordingsSetting.ENTRY_LIST_TITLE);

        requireEntryPoint(RecordingsSetting.ENTRY_RECORDINGS);
        requireEntryPoint(RecordingsSetting.ENTRY_DETAILS);
    }

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = recordingsMenu.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Recordings menu '" + EditorSetting.RECORDINGS_MENU + "' has no entry point "
                    + entryPoint + ".");

        return element;
    }

    // Update \\

    @Override
    protected void update() {

        if (recorderManager.getRevision() == shownRevision)
            return;

        this.shownRevision = recorderManager.getRevision();
        statusLabel.setFontText(recorderManager.getStatusText());
        refreshRecordings();
        refreshDetails();
    }

    // Recordings \\

    // A row for every recording saved, most recently changed first, the open one marked
    private void refreshRecordings() {

        menuManager.ejectAll(recordingsMenu, RecordingsSetting.ENTRY_RECORDINGS);

        ObjectArrayList<String> recordingNames = recorderManager.getRecordingNames();
        String openName = recorderManager.hasRecording() ? recorderManager.getRecording().getRecordingName() : null;

        listTitle.setFontText(String.format(RecordingsSetting.FORMAT_LIST_TITLE, recordingNames.size()));

        if (recordingNames.isEmpty())
            injectText(RecordingsSetting.ENTRY_RECORDINGS, RecordingsSetting.TEXT_NO_RECORDINGS);

        for (int i = 0; i < recordingNames.size(); i++) {

            String recordingName = recordingNames.get(i);
            boolean open = recordingName.equals(openName);
            ElementInstance row = menuManager.inject(
                    recordingsMenu,
                    RecordingsSetting.ENTRY_RECORDINGS,
                    open
                            ? RecordingsSetting.TEMPLATE_RECORDING_ROW_SELECTED
                            : RecordingsSetting.TEMPLATE_RECORDING_ROW);

            row.setActionArgOverride(recordingName);
            row.findChildById(RecordingsSetting.ELEMENT_ROW_LABEL).setFontText(recordingName);
            row.findChildById(RecordingsSetting.ELEMENT_ROW_VALUE).setFontText(
                    open ? RecordingsSetting.TEXT_OPEN : RecordingsSetting.TEXT_EMPTY_VALUE);
        }
    }

    // Details \\

    // What the open recording holds: the world it was made in, its takes, clips and transitions, and how long it runs
    private void refreshDetails() {

        menuManager.ejectAll(recordingsMenu, RecordingsSetting.ENTRY_DETAILS);

        if (!recorderManager.hasRecording()) {
            injectText(RecordingsSetting.ENTRY_DETAILS, RecordingsSetting.TEXT_NO_RECORDING);
            injectText(RecordingsSetting.ENTRY_DETAILS, RecordingsSetting.TEXT_OPEN_HINT);
            return;
        }

        RecordingInstance recording = recorderManager.getRecording();

        injectHeader(recording.getRecordingName());
        injectDetail(RecordingsSetting.TEXT_STATE,
                recorderManager.isUnsaved() ? RecordingsSetting.TEXT_UNSAVED : RecordingsSetting.TEXT_SAVED);
        injectDetail(RecordingsSetting.TEXT_WORLD, recording.getWorldName());
        injectDetail(RecordingsSetting.TEXT_TAKES, Integer.toString(recording.getTakes().size()));
        injectDetail(RecordingsSetting.TEXT_CLIPS, Integer.toString(recording.getClips().size()));
        injectDetail(RecordingsSetting.TEXT_TRANSITIONS, Integer.toString(countTransitions(recording)));
        injectDetail(RecordingsSetting.TEXT_LENGTH, formatTime(recording.getDuration()));
        injectText(RecordingsSetting.ENTRY_DETAILS, RecordingsSetting.TEXT_OPEN_HINT);
        injectText(RecordingsSetting.ENTRY_DETAILS, RecordingsSetting.TEXT_EDIT_HINT);
    }

    private int countTransitions(RecordingInstance recording) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();
        int count = 0;

        for (int i = 0; i < clips.size(); i++)
            for (int e = 0; e < RecorderClipEdge.VALUES.length; e++)
                if (clips.get(i).hasTransition(RecorderClipEdge.VALUES[e]))
                    count++;

        return count;
    }

    private void injectDetail(String label, String value) {

        ElementInstance row = menuManager.inject(
                recordingsMenu, RecordingsSetting.ENTRY_DETAILS, RecordingsSetting.TEMPLATE_DETAIL_ROW);

        row.findChildById(RecordingsSetting.ELEMENT_ROW_LABEL).setFontText(label);
        row.findChildById(RecordingsSetting.ELEMENT_ROW_VALUE).setFontText(value);
    }

    // Utility \\

    private void injectHeader(String text) {
        menuManager.inject(recordingsMenu, RecordingsSetting.ENTRY_DETAILS, RecordingsSetting.TEMPLATE_HEADER_ROW)
                .findChildById(RecordingsSetting.ELEMENT_ROW_LABEL).setFontText(text);
    }

    private void injectText(int entryPoint, String text) {
        menuManager.inject(recordingsMenu, entryPoint, RecordingsSetting.TEMPLATE_TEXT_ROW)
                .findChildById(RecordingsSetting.ELEMENT_ROW_LABEL).setFontText(text);
    }

    private String formatTime(float seconds) {

        int minutes = (int) (seconds / RecordingsSetting.SECONDS_PER_MINUTE);

        return String.format(
                RecordingsSetting.FORMAT_TIME, minutes, seconds - minutes * RecordingsSetting.SECONDS_PER_MINUTE);
    }
}
