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
import editor.bootstrap.recorderpipeline.recording.RecordingKind;
import editor.runtime.EditorSetting;
import editor.runtime.recordings.RecordingsSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingsPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and shows the recordings. When
     * the recorder's revision moves it refills the list — under Videos a row
     * for every recording saved in the game's directory, most recently
     * changed first, and under Songs a row for every project song, a Music
     * file, by name; the open one marked, each picked up by its name to be
     * opened or dragged onto the Timeline to lay — what the open recording
     * holds, and the status line, which follows the recorder's.
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

    // A row for every Video saved, most recently changed first, then every song, the open one marked
    private void refreshRecordings() {

        menuManager.ejectAll(recordingsMenu, RecordingsSetting.ENTRY_RECORDINGS);

        ObjectArrayList<String> recordingNames = recorderManager.getRecordingNames();
        ObjectArrayList<String> songNames = recorderManager.getSongNames();
        RecordingInstance open = recorderManager.getRecording();

        listTitle.setFontText(String.format(RecordingsSetting.FORMAT_LIST_TITLE,
                recordingNames.size() + songNames.size()));

        injectText(RecordingsSetting.ENTRY_RECORDINGS, String.format(RecordingsSetting.FORMAT_VIDEOS_HEADER,
                recordingNames.size()));

        if (recordingNames.isEmpty())
            injectText(RecordingsSetting.ENTRY_RECORDINGS, RecordingsSetting.TEXT_NO_RECORDINGS);

        injectRows(recordingNames, open, RecordingKind.VIDEO, RecordingsSetting.TEMPLATE_RECORDING_ROW,
                RecordingsSetting.TEMPLATE_RECORDING_ROW_SELECTED);
        injectText(RecordingsSetting.ENTRY_RECORDINGS, String.format(RecordingsSetting.FORMAT_SONGS_HEADER,
                songNames.size()));

        if (songNames.isEmpty())
            injectText(RecordingsSetting.ENTRY_RECORDINGS, RecordingsSetting.TEXT_NO_SONGS);

        injectRows(songNames, open, RecordingKind.MUSIC, RecordingsSetting.TEMPLATE_SONG_ROW,
                RecordingsSetting.TEMPLATE_SONG_ROW_SELECTED);
    }

    // A row for every name, picked up by its name, the open one of the kind marked
    private void injectRows(
            ObjectArrayList<String> names,
            RecordingInstance open,
            RecordingKind kind,
            String template,
            String selectedTemplate) {

        String openName = open != null && open.getKind() == kind ? open.getRecordingName() : null;

        for (int i = 0; i < names.size(); i++) {

            String name = names.get(i);
            boolean opened = name.equals(openName);
            ElementInstance row = menuManager.inject(
                    recordingsMenu,
                    RecordingsSetting.ENTRY_RECORDINGS,
                    opened ? selectedTemplate : template);

            row.setOnDragArgOverride(name);
            row.findChildById(RecordingsSetting.ELEMENT_ROW_LABEL).setFontText(name);
            row.findChildById(RecordingsSetting.ELEMENT_ROW_VALUE).setFontText(
                    opened ? RecordingsSetting.TEXT_OPEN : RecordingsSetting.TEXT_EMPTY_VALUE);
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
        injectDetail(RecordingsSetting.TEXT_KIND, recording.getKind().getLabel());
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
