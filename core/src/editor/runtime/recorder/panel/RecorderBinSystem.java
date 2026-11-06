package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderClipEdge;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.runtime.EditorSetting;
import editor.runtime.recorder.RecorderSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderBinSystem extends SystemPackage {

    /*
     * Fills the Recorder Bin tab: beside each other, the bin of takes, each
     * with its channels, its length and how often it is laid, picked up by
     * its name to be dragged onto the timeline in the Recorder tab, laid at
     * the playhead, renamed or deleted; and the clip selected on the
     * timeline with where it lies, what of its take it plays, the node,
     * sound or transition selected on it, the channels it plays and every
     * edit it takes. Both are drawn again when the recorder's revision moves.
     */

    // Internal
    private MenuManager menuManager;
    private RecorderManager recorderManager;
    private RecorderRowSystem recorderRowSystem;

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
        this.recorderManager = get(RecorderManager.class);
        this.recorderRowSystem = get(RecorderRowSystem.class);
    }

    @Override
    protected void awake() {
        recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_TAKES);
        recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_CLIP);
    }

    // Update \\

    @Override
    protected void update() {

        if (recorderManager.getRevision() == shownRevision)
            return;

        this.shownRevision = recorderManager.getRevision();
        refreshTakes();
        refreshClip();
    }

    // Takes \\

    // Every take in the bin, picked up by its name to be dragged onto the timeline
    private void refreshTakes() {

        int entryPoint = RecorderSetting.ENTRY_TAKES;

        menuManager.ejectAll(recorderRowSystem.getMenu(), entryPoint);

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null || recording.getTakes().isEmpty()) {
            recorderRowSystem.injectText(entryPoint, RecorderSetting.TEXT_NO_TAKES);
            return;
        }

        ObjectArrayList<RecordingTakeStruct> takes = recording.getTakes();

        for (int i = 0; i < takes.size(); i++) {

            RecordingTakeStruct take = takes.get(i);
            String takeKey = Integer.toString(i);
            String channels = recorderRowSystem.describeChannels(take.getChannelMask());
            String duration = recorderRowSystem.formatTime(take.getDuration());
            int placed = recording.countClips(take);
            ElementInstance row = menuManager.inject(
                    recorderRowSystem.getMenu(),
                    entryPoint,
                    i == recorderManager.getSelectedTake()
                            ? RecorderSetting.TEMPLATE_TAKE_ROW_SELECTED
                            : RecorderSetting.TEMPLATE_TAKE_ROW);

            row.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(placed > 0
                    ? String.format(RecorderSetting.FORMAT_TAKE_PLACED, take.getName(), channels, duration, placed)
                    : String.format(RecorderSetting.FORMAT_TAKE, take.getName(), channels, duration));

            row.findChildById(RecorderSetting.ELEMENT_ROW_SELECT).setOnDragArgOverride(takeKey);
            row.findChildById(RecorderSetting.ELEMENT_ROW_PLACE).setActionArgOverride(takeKey);
            row.findChildById(RecorderSetting.ELEMENT_ROW_RENAME).setActionArgOverride(takeKey);
            row.findChildById(RecorderSetting.ELEMENT_ROW_DELETE).setActionArgOverride(takeKey);
        }
    }

    // Clip \\

    // The selected clip: where it lies, what of its take it plays, a row for each channel the take holds, and
    // every edit made at the playhead
    private void refreshClip() {

        int entryPoint = RecorderSetting.ENTRY_CLIP;

        menuManager.ejectAll(recorderRowSystem.getMenu(), entryPoint);

        RecordingInstance recording = recorderManager.getRecording();
        int selected = recorderManager.getSelectedClip();

        if (recording == null) {
            recorderRowSystem.injectText(entryPoint, RecorderSetting.TEXT_OPEN_RECORDING_HINT);
            return;
        }

        if (selected < 0 || selected >= recording.getClips().size()) {
            recorderRowSystem.injectText(entryPoint, RecorderSetting.TEXT_NO_CLIP);
            return;
        }

        RecordingClipStruct clip = recording.getClips().get(selected);
        RecordingTakeStruct take = clip.getTake();

        recorderRowSystem.injectHeader(entryPoint, take.getName());
        recorderRowSystem.injectText(entryPoint, String.format(RecorderSetting.FORMAT_CLIP_PLACE,
                clip.getLane() + 1,
                recorderRowSystem.formatTime(clip.getStart()),
                recorderRowSystem.formatTime(clip.getEnd())));
        recorderRowSystem.injectText(entryPoint, String.format(RecorderSetting.FORMAT_CLIP_SOURCE,
                recorderRowSystem.formatTime(clip.getInPoint()),
                recorderRowSystem.formatTime(clip.getOutPoint())));
        recorderRowSystem.injectNode(entryPoint);
        recorderRowSystem.injectSound(entryPoint);
        recorderRowSystem.injectTransition(entryPoint);
        recorderRowSystem.injectHeader(entryPoint, RecorderSetting.TEXT_HEADER_PLAYS);

        for (RecorderChannel channel : RecorderChannel.VALUES)
            if (take.holds(channel))
                recorderRowSystem.injectSetting(
                        entryPoint,
                        channel.getLabel(),
                        recorderRowSystem.toOnOff(clip.holds(channel)),
                        EditorSetting.RECORDER_KEY_CLIP_CHANNEL_PREFIX + channel.getKey());

        recorderRowSystem.injectHeader(entryPoint, RecorderSetting.TEXT_HEADER_EDIT);
        recorderRowSystem.injectSetting(entryPoint, RecorderSetting.TEXT_MUTE,
                recorderRowSystem.toOnOff(clip.isMuted()), EditorSetting.RECORDER_KEY_CLIP_MUTE);
        recorderRowSystem.injectSetting(entryPoint, RecorderSetting.TEXT_TIME_RAMP,
                clip.getTimeRampSeconds() > 0f
                        ? String.format(RecorderSetting.FORMAT_RAMP, clip.getTimeRampSeconds())
                        : RecorderSetting.TEXT_RAMP_CUT,
                EditorSetting.RECORDER_KEY_CLIP_RAMP);
        recorderRowSystem.injectAction(entryPoint, RecorderSetting.TEXT_TRIM_START,
                EditorSetting.RECORDER_KEY_CLIP_TRIM_START);
        recorderRowSystem.injectAction(entryPoint, RecorderSetting.TEXT_TRIM_END,
                EditorSetting.RECORDER_KEY_CLIP_TRIM_END);
        recorderRowSystem.injectAction(entryPoint, RecorderSetting.TEXT_SPLIT, EditorSetting.RECORDER_KEY_CLIP_SPLIT);
        recorderRowSystem.injectAction(entryPoint, RecorderSetting.TEXT_STAMP, EditorSetting.RECORDER_KEY_CLIP_STAMP);

        if (take.holds(RecorderChannel.SOUND))
            recorderRowSystem.injectAction(entryPoint, RecorderSetting.TEXT_SOUNDS_ADD,
                    EditorSetting.RECORDER_KEY_SOUND_ADD);

        recorderRowSystem.injectAction(entryPoint, RecorderSetting.TEXT_TRANSITION_IN,
                EditorSetting.RECORDER_KEY_TRANSITION_PREFIX + RecorderClipEdge.START.getKey());
        recorderRowSystem.injectAction(entryPoint, RecorderSetting.TEXT_TRANSITION_OUT,
                EditorSetting.RECORDER_KEY_TRANSITION_PREFIX + RecorderClipEdge.END.getKey());
        recorderRowSystem.injectAction(entryPoint, RecorderSetting.TEXT_REMOVE_CLIP,
                EditorSetting.RECORDER_KEY_CLIP_REMOVE);
    }
}
