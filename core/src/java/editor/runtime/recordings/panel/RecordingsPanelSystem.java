package editor.runtime.recordings.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.animationeditorpipeline.animationeditormanager.AnimationEditorManager;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderClipEdge;
import editor.bootstrap.recorderpipeline.recording.RecordingBoneTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import editor.runtime.EditorSetting;
import editor.runtime.animationeditor.AnimationEditorContext;
import editor.runtime.dev.DevContext;
import editor.runtime.recordings.RecordingsSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingsPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and shows the tracks the
     * recorder opens: the media files, or the animations of the character
     * the Animation Editor has chosen. It turns to the animations as soon as
     * they are worked on — the Animation Editor worked in since a Dev window,
     * or an animation open on the timeline when neither was — and back to the
     * media files when they are, and the toolbar's switch turns it either way
     * in between (RecorderManager keeps which, for every Recordings tab). When the recorder's, the animation
     * editor's or the content's revision moves, or what it lists changes, it
     * refills the list — a row for every media file in the media directory
     * beside the player's saves, video and music alike, most recently changed
     * first, each picked up by its name to be opened or dragged onto the
     * Timeline to lay; or a row for every clip on the character's rig, by
     * name, each clicked to open it on the Timeline and in the Animation
     * Editor — the open one marked, what the open one holds, and the status
     * line, which follows the recorder's.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private RecorderManager recorderManager;
    private AnimationEditorManager animationEditorManager;
    private InfoManager infoManager;
    private ActiveWindowManager activeWindowManager;

    // Menus
    private MenuInstance recordingsMenu;
    private ElementInstance statusLabel;
    private ElementInstance listTitle;

    // State
    private int shownRevision;
    private int shownAnimationRevision;
    private int shownInfoRevision;
    private boolean shownAnimations;
    private int workedOn;

    // Scratch
    private ObjectArrayList<String> clipNames;

    // Base \\

    @Override
    protected void create() {

        // State
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        this.workedOn = EngineSetting.INDEX_NOT_FOUND;

        // Scratch
        this.clipNames = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.recorderManager = get(RecorderManager.class);
        this.animationEditorManager = get(AnimationEditorManager.class);
        this.infoManager = get(InfoManager.class);
        this.activeWindowManager = get(ActiveWindowManager.class);
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

        followWork();

        boolean animations = recorderManager.isListingAnimations();

        if (recorderManager.getRevision() == shownRevision
                && animations == shownAnimations
                && (!animations || animationEditorManager.getRevision() == shownAnimationRevision
                        && infoManager.getRevision() == shownInfoRevision))
            return;

        this.shownRevision = recorderManager.getRevision();
        this.shownAnimationRevision = animationEditorManager.getRevision();
        this.shownInfoRevision = infoManager.getRevision();
        this.shownAnimations = animations;
        statusLabel.setFontText(recorderManager.getStatusText());

        if (animations) {
            refreshAnimations();
            refreshAnimationDetails();
            return;
        }

        refreshRecordings();
        refreshDetails();
    }

    // The list turned to what is worked on whenever that changes, so the switch holds in between
    private void followWork() {

        int working = isWorkingOnAnimations() ? 1 : 0;

        if (working == workedOn)
            return;

        this.workedOn = working;
        recorderManager.showAnimationTracks(working == 1);
    }

    // Animations are worked on while the Animation Editor was worked in since any Dev window, or, when neither was,
    // while one is open on the timeline
    private boolean isWorkingOnAnimations() {

        WindowInstance recent = activeWindowManager.findRecent(AnimationEditorContext.class, DevContext.class);

        if (recent != null)
            return recent.getContext() instanceof AnimationEditorContext;

        RecordingInstance open = recorderManager.getRecording();

        return open != null && open.isAnimation();
    }

    // Recordings \\

    // A row for every media file, most recently changed first, the open one marked
    private void refreshRecordings() {

        menuManager.ejectAll(recordingsMenu, RecordingsSetting.ENTRY_RECORDINGS);

        ObjectArrayList<String> recordingNames = recorderManager.getRecordingNames();
        RecordingInstance open = recorderManager.getRecording();
        String openName = open != null && !open.isAnimation() ? open.getRecordingName() : null;

        listTitle.setFontText(String.format(RecordingsSetting.FORMAT_LIST_TITLE, recordingNames.size()));

        if (recordingNames.isEmpty())
            injectText(RecordingsSetting.ENTRY_RECORDINGS, RecordingsSetting.TEXT_NO_RECORDINGS);

        for (int i = 0; i < recordingNames.size(); i++) {

            String name = recordingNames.get(i);
            boolean opened = name.equals(openName);
            ElementInstance row = menuManager.inject(
                    recordingsMenu,
                    RecordingsSetting.ENTRY_RECORDINGS,
                    opened
                            ? RecordingsSetting.TEMPLATE_RECORDING_ROW_SELECTED
                            : RecordingsSetting.TEMPLATE_RECORDING_ROW);

            row.setOnDragArgOverride(name);
            row.findChildById(RecordingsSetting.ELEMENT_ROW_LABEL).setFontText(name);
            row.findChildById(RecordingsSetting.ELEMENT_ROW_VALUE).setFontText(
                    opened ? RecordingsSetting.TEXT_OPEN : RecordingsSetting.TEXT_EMPTY_VALUE);
        }
    }

    // Animations \\

    // A row for every clip on the rig of the character chosen, by name, the one open on the timeline marked
    private void refreshAnimations() {

        menuManager.ejectAll(recordingsMenu, RecordingsSetting.ENTRY_RECORDINGS);
        animationEditorManager.collectCharacterClips(clipNames);

        RecordingInstance open = recorderManager.getRecording();
        String openName = open != null && open.isAnimation() ? open.getAnimationName() : null;

        listTitle.setFontText(String.format(RecordingsSetting.FORMAT_ANIMATION_LIST_TITLE, clipNames.size()));

        if (animationEditorManager.getCharacterRig() == null)
            injectText(RecordingsSetting.ENTRY_RECORDINGS, RecordingsSetting.TEXT_NO_CHARACTER);
        else if (clipNames.isEmpty())
            injectText(RecordingsSetting.ENTRY_RECORDINGS, RecordingsSetting.TEXT_NO_ANIMATIONS);

        for (int i = 0; i < clipNames.size(); i++) {

            String name = clipNames.get(i);
            boolean opened = name.equals(openName);
            ElementInstance row = menuManager.inject(
                    recordingsMenu,
                    RecordingsSetting.ENTRY_RECORDINGS,
                    opened
                            ? RecordingsSetting.TEMPLATE_ANIMATION_ROW_SELECTED
                            : RecordingsSetting.TEMPLATE_ANIMATION_ROW);

            row.setActionArgOverride(name);
            row.findChildById(RecordingsSetting.ELEMENT_ROW_LABEL).setFontText(name);
            row.findChildById(RecordingsSetting.ELEMENT_ROW_VALUE).setFontText(
                    opened ? RecordingsSetting.TEXT_OPEN : RecordingsSetting.TEXT_EMPTY_VALUE);
        }
    }

    // What the open clip is: the character it plays on, its rig, whether it is saved, how long it runs and how many
    // bones it keys
    private void refreshAnimationDetails() {

        menuManager.ejectAll(recordingsMenu, RecordingsSetting.ENTRY_DETAILS);

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null || !recording.isAnimation()) {
            injectText(RecordingsSetting.ENTRY_DETAILS, RecordingsSetting.TEXT_NO_ANIMATION);
            injectText(RecordingsSetting.ENTRY_DETAILS, RecordingsSetting.TEXT_ANIMATION_OPEN_HINT);
            return;
        }

        String characterName = animationEditorManager.getCharacterName();

        injectHeader(recording.getAnimationName());
        injectDetail(RecordingsSetting.TEXT_CHARACTER,
                characterName != null ? characterName : EditorSetting.CHARACTER_VIEW_NONE);
        injectDetail(RecordingsSetting.TEXT_RIG, ArpgUtility.getString(
                recording.getAnimationArpg(),
                EditorSetting.ANIMATION_EDITOR_FIELD_RIG,
                EditorSetting.CHARACTER_VIEW_NONE));
        injectDetail(RecordingsSetting.TEXT_STATE,
                recorderManager.isUnsaved() ? RecordingsSetting.TEXT_UNSAVED : RecordingsSetting.TEXT_SAVED);
        injectDetail(RecordingsSetting.TEXT_LENGTH, formatTime(recording.getDuration()));
        injectDetail(RecordingsSetting.TEXT_BONES, Integer.toString(countKeyedBones(recording)));
        injectText(RecordingsSetting.ENTRY_DETAILS, RecordingsSetting.TEXT_ANIMATION_OPEN_HINT);
        injectText(RecordingsSetting.ENTRY_DETAILS, RecordingsSetting.TEXT_ANIMATION_EDIT_HINT);
    }

    private int countKeyedBones(RecordingInstance recording) {

        ObjectArrayList<RecordingBoneTrackStruct> bones = recording.getTakes().get(0).getBoneTracks();
        int count = 0;

        for (int i = 0; i < bones.size(); i++)
            if (bones.get(i).isKeyed())
                count++;

        return count;
    }

    // Details \\

    // What the open recording holds: the world it was made in, its takes, clips and transitions, and how long it runs
    private void refreshDetails() {

        menuManager.ejectAll(recordingsMenu, RecordingsSetting.ENTRY_DETAILS);

        if (!recorderManager.hasRecording() || recorderManager.getRecording().isAnimation()) {
            injectText(RecordingsSetting.ENTRY_DETAILS, RecordingsSetting.TEXT_NO_RECORDING);
            injectText(RecordingsSetting.ENTRY_DETAILS, RecordingsSetting.TEXT_OPEN_HINT);
            return;
        }

        RecordingInstance recording = recorderManager.getRecording();

        injectHeader(recording.getRecordingName());
        injectDetail(RecordingsSetting.TEXT_KIND, recording.hasPicture()
                ? RecordingsSetting.TEXT_KIND_VIDEO
                : RecordingsSetting.TEXT_KIND_MUSIC);
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
