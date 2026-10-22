package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingDragStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingMarkerStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.bootstrap.recorderpipeline.util.FfmpegUtility;
import editor.runtime.recorder.RecorderSetting;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and shows the recorder. When the
     * recorder's revision moves it refills every panel — the recordings in
     * the game's directory with the open one marked, the channels armed for
     * the next take and the export settings, the bin of takes with their
     * channels, length and how often each is laid, the clip selected with
     * what it plays and every edit it takes, every marker, and the timeline's
     * clips on their lanes, highest on top, its marker ticks and export range.
     * Both playheads follow the playhead every frame from positions built
     * once, and a take or clip being dragged shows where it would land from
     * positions built the first time each is needed, so neither playback nor
     * a drag allocates; the clock text, the export's progress and a timeline
     * still growing under a take refresh a few times a second.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private RecorderManager recorderManager;

    // Menus
    private MenuInstance recorderMenu;
    private ElementInstance statusLabel;
    private ElementInstance clockLabel;
    private ElementInstance playhead;
    private ElementInstance lanePlayhead;
    private ElementInstance rangeStartMarker;
    private ElementInstance rangeEndMarker;
    private ElementInstance dragGhost;
    private ElementInstance progressLabel;

    // Timeline
    private DimensionVector2Struct[] timelinePositions;
    private DimensionVector2Struct[][] lanePositions;
    private DimensionVector2Struct rangeMarkerSize;
    private DimensionVector2Struct hiddenSize;
    private int shownPlayheadStep;
    private float shownSpan;

    // Ghost
    private DimensionVector2Struct ghostSize;
    private float ghostLength;
    private float ghostSpan;
    private int ghostStep;
    private int ghostLane;
    private boolean ghostShown;

    // State
    private int shownRevision;
    private float refreshElapsed;

    // Scratch
    private StringBuilder channelScratch;

    // Base \\

    @Override
    protected void create() {

        // Timeline
        this.timelinePositions = new DimensionVector2Struct[RecorderSetting.TIMELINE_STEPS + 1];

        for (int i = 0; i < timelinePositions.length; i++)
            timelinePositions[i] = new DimensionVector2Struct(
                    DimensionValueStruct.ofPercent(toPercent(i)),
                    DimensionValueStruct.ofAbsolute(0f));

        this.lanePositions = new DimensionVector2Struct[EditorSetting.RECORDER_LANE_COUNT]
                [RecorderSetting.TIMELINE_STEPS + 1];
        this.rangeMarkerSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(RecorderSetting.RANGE_MARKER_WIDTH_PIXELS),
                DimensionValueStruct.ofAbsolute(RecorderSetting.RANGE_MARKER_HEIGHT_PIXELS));
        this.hiddenSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(0f),
                DimensionValueStruct.ofAbsolute(0f));
        this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;

        // Ghost
        this.ghostLength = EditorSetting.RECORDER_TIME_UNSET;
        this.ghostStep = EngineSetting.INDEX_NOT_FOUND;

        // State
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;

        // Scratch
        this.channelScratch = new StringBuilder();
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
        this.recorderMenu = menuManager.openMenu(EditorSetting.RECORDER_MENU, window);

        this.statusLabel = requireEntryPoint(RecorderSetting.ENTRY_STATUS);
        this.clockLabel = requireEntryPoint(RecorderSetting.ENTRY_CLOCK);
        this.playhead = requireEntryPoint(RecorderSetting.ENTRY_PLAYHEAD);
        this.lanePlayhead = requireEntryPoint(RecorderSetting.ENTRY_LANE_PLAYHEAD);
        this.rangeStartMarker = requireEntryPoint(RecorderSetting.ENTRY_RANGE_START);
        this.rangeEndMarker = requireEntryPoint(RecorderSetting.ENTRY_RANGE_END);
        this.dragGhost = requireEntryPoint(RecorderSetting.ENTRY_DRAG_GHOST);

        requireEntryPoint(RecorderSetting.ENTRY_RECORDINGS);
        requireEntryPoint(RecorderSetting.ENTRY_SETTINGS);
        requireEntryPoint(RecorderSetting.ENTRY_TAKES);
        requireEntryPoint(RecorderSetting.ENTRY_MARKERS);
        requireEntryPoint(RecorderSetting.ENTRY_CLIP);
        requireEntryPoint(RecorderSetting.ENTRY_MARKER_TICKS);
        requireEntryPoint(EditorSetting.RECORDER_ENTRY_CLIP_LANES);
    }

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = recorderMenu.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Recorder menu '" + EditorSetting.RECORDER_MENU + "' has no entry point "
                    + entryPoint + ".");

        return element;
    }

    // Update \\

    @Override
    protected void update() {

        if (recorderManager.getRevision() != shownRevision) {
            this.shownRevision = recorderManager.getRevision();
            refreshAll();
        }

        this.refreshElapsed += internal.getDeltaTime();

        if (refreshElapsed >= RecorderSetting.REFRESH_SECONDS) {
            this.refreshElapsed = 0f;
            refreshClock();
            refreshProgress();

            if (recorderManager.resolveTimelineSpan() != shownSpan)
                refreshTimeline();
        }

        movePlayhead();
        moveGhost();
    }

    private void refreshAll() {
        refreshStatus();
        refreshRecordings();
        refreshSettings();
        refreshTakes();
        refreshClip();
        refreshMarkers();
        refreshTimeline();
        refreshClock();
    }

    // Status \\

    private void refreshStatus() {
        statusLabel.setFontText(recorderManager.getStatusText());
    }

    private void refreshClock() {

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null) {
            clockLabel.setFontText(RecorderSetting.TEXT_NO_RECORDING);
            return;
        }

        clockLabel.setFontText(String.format(
                RecorderSetting.FORMAT_CLOCK,
                recording.getRecordingName() + (recorderManager.isUnsaved() ? RecorderSetting.TEXT_UNSAVED_SUFFIX : ""),
                formatTime(recorderManager.getPlayhead()),
                formatTime(recorderManager.resolveTimelineSpan())));
    }

    private void refreshProgress() {
        if (progressLabel != null && recorderManager.getState() == RecorderState.EXPORTING)
            progressLabel.setFontText(String.format(
                    RecorderSetting.FORMAT_PROGRESS,
                    recorderManager.getExportedFrames(),
                    recorderManager.getExportFrameCount()));
    }

    // Recordings \\

    private void refreshRecordings() {

        menuManager.ejectAll(recorderMenu, RecorderSetting.ENTRY_RECORDINGS);

        ObjectArrayList<String> recordingNames = recorderManager.getRecordingNames();
        String openName = recorderManager.hasRecording() ? recorderManager.getRecording().getRecordingName() : null;

        if (recordingNames.isEmpty())
            injectText(RecorderSetting.ENTRY_RECORDINGS, RecorderSetting.TEXT_NO_RECORDINGS);

        for (int i = 0; i < recordingNames.size(); i++) {

            String recordingName = recordingNames.get(i);
            ElementInstance row = menuManager.inject(
                    recorderMenu,
                    RecorderSetting.ENTRY_RECORDINGS,
                    recordingName.equals(openName)
                            ? RecorderSetting.TEMPLATE_RECORDING_ROW_SELECTED
                            : RecorderSetting.TEMPLATE_RECORDING_ROW);

            row.setActionArgOverride(recordingName);
            row.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(recordingName);
        }
    }

    // Settings \\

    private void refreshSettings() {

        menuManager.ejectAll(recorderMenu, RecorderSetting.ENTRY_SETTINGS);
        this.progressLabel = null;

        int armedChannels = recorderManager.getArmedChannels();

        injectHeader(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_HEADER_RECORD);

        for (RecorderChannel channel : RecorderChannel.VALUES)
            injectSetting(
                    RecorderSetting.ENTRY_SETTINGS,
                    channel.getLabel(),
                    channel.isIn(armedChannels) ? RecorderSetting.TEXT_ARMED : RecorderSetting.TEXT_OFF,
                    EditorSetting.RECORDER_KEY_ARM_PREFIX + channel.getKey());

        injectHeader(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_HEADER_EXPORT);

        if (!recorderManager.hasRecording()) {
            injectText(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_NO_RECORDING);
            return;
        }

        RecordingExportStruct exportStruct = recorderManager.getRecording().getExportStruct();

        injectSetting(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_RESOLUTION,
                String.format(RecorderSetting.FORMAT_RESOLUTION, exportStruct.getWidth(), exportStruct.getHeight()),
                EditorSetting.RECORDER_KEY_RESOLUTION);
        injectSetting(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_FRAME_RATE,
                String.format(RecorderSetting.FORMAT_FRAME_RATE, exportStruct.getFrameRate()),
                EditorSetting.RECORDER_KEY_FRAME_RATE);
        injectSetting(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_FORMAT,
                exportStruct.getFormat().getLabel(), EditorSetting.RECORDER_KEY_FORMAT);
        injectSetting(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_MOTION_BLUR,
                exportStruct.getMotionBlurSamples() > 1
                        ? String.format(RecorderSetting.FORMAT_MOTION_BLUR, exportStruct.getMotionBlurSamples())
                        : RecorderSetting.TEXT_OFF,
                EditorSetting.RECORDER_KEY_MOTION_BLUR);
        injectSetting(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_SUPERSAMPLE,
                toOnOff(exportStruct.isSupersampled()), EditorSetting.RECORDER_KEY_SUPERSAMPLE);
        injectSetting(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_HUD,
                toOnOff(exportStruct.isHud()), EditorSetting.RECORDER_KEY_HUD);
        injectSetting(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_RANGE_START,
                exportStruct.hasRangeStart()
                        ? formatTime(exportStruct.getRangeStart())
                        : RecorderSetting.TEXT_RANGE_FROM_START,
                EditorSetting.RECORDER_KEY_RANGE_START);
        injectSetting(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_RANGE_END,
                exportStruct.hasRangeEnd()
                        ? formatTime(exportStruct.getRangeEnd())
                        : RecorderSetting.TEXT_RANGE_TO_END,
                EditorSetting.RECORDER_KEY_RANGE_END);
        injectAction(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_CLEAR_RANGE,
                EditorSetting.RECORDER_KEY_RANGE_CLEAR);

        if (recorderManager.getState() == RecorderState.EXPORTING) {
            injectAction(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_CANCEL_EXPORT,
                    EditorSetting.RECORDER_KEY_CANCEL_EXPORT);
            this.progressLabel = injectText(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_EMPTY);
            refreshProgress();
            return;
        }

        injectAction(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_EXPORT, EditorSetting.RECORDER_KEY_EXPORT);
        injectText(RecorderSetting.ENTRY_SETTINGS, FfmpegUtility.findExecutable(internal.path) != null
                ? RecorderSetting.TEXT_FFMPEG_FOUND
                : RecorderSetting.TEXT_FFMPEG_MISSING);
    }

    private void injectSetting(int entryPoint, String label, String value, String settingKey) {

        ElementInstance row = menuManager.inject(recorderMenu, entryPoint, RecorderSetting.TEMPLATE_SETTING_ROW);

        row.setActionArgOverride(settingKey);
        row.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(label);
        row.findChildById(RecorderSetting.ELEMENT_ROW_VALUE).setFontText(value);
    }

    private void injectAction(int entryPoint, String label, String settingKey) {

        ElementInstance row = menuManager.inject(recorderMenu, entryPoint, RecorderSetting.TEMPLATE_ACTION_ROW);

        row.setActionArgOverride(settingKey);
        row.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(label);
    }

    // Takes \\

    // Every take in the bin, picked up by its name to be dragged onto the timeline
    private void refreshTakes() {

        menuManager.ejectAll(recorderMenu, RecorderSetting.ENTRY_TAKES);

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null || recording.getTakes().isEmpty()) {
            injectText(RecorderSetting.ENTRY_TAKES, RecorderSetting.TEXT_NO_TAKES);
            return;
        }

        ObjectArrayList<RecordingTakeStruct> takes = recording.getTakes();

        for (int i = 0; i < takes.size(); i++) {

            RecordingTakeStruct take = takes.get(i);
            String takeKey = Integer.toString(i);
            int placed = recording.countClips(take);
            ElementInstance row = menuManager.inject(
                    recorderMenu,
                    RecorderSetting.ENTRY_TAKES,
                    i == recorderManager.getSelectedTake()
                            ? RecorderSetting.TEMPLATE_TAKE_ROW_SELECTED
                            : RecorderSetting.TEMPLATE_TAKE_ROW);

            row.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(placed > 0
                    ? String.format(RecorderSetting.FORMAT_TAKE_PLACED, take.getName(),
                            describeChannels(take.getChannelMask()), formatTime(take.getDuration()), placed)
                    : String.format(RecorderSetting.FORMAT_TAKE, take.getName(),
                            describeChannels(take.getChannelMask()), formatTime(take.getDuration())));

            row.findChildById(RecorderSetting.ELEMENT_ROW_SELECT).setOnDragArgOverride(takeKey);
            row.findChildById(RecorderSetting.ELEMENT_ROW_PLACE).setActionArgOverride(takeKey);
            row.findChildById(RecorderSetting.ELEMENT_ROW_RENAME).setActionArgOverride(takeKey);
            row.findChildById(RecorderSetting.ELEMENT_ROW_DELETE).setActionArgOverride(takeKey);
        }
    }

    private String describeChannels(int channelMask) {

        channelScratch.setLength(0);

        for (RecorderChannel channel : RecorderChannel.VALUES) {

            if (!channel.isIn(channelMask))
                continue;

            if (!channelScratch.isEmpty())
                channelScratch.append(RecorderSetting.TEXT_CHANNEL_SEPARATOR);

            channelScratch.append(channel.getLabel());
        }

        return channelScratch.toString();
    }

    // Clip \\

    // The selected clip: where it lies, what of its take it plays, a row for each channel the take holds, and
    // every edit made at the playhead
    private void refreshClip() {

        menuManager.ejectAll(recorderMenu, RecorderSetting.ENTRY_CLIP);

        RecordingInstance recording = recorderManager.getRecording();
        int selected = recorderManager.getSelectedClip();

        if (recording == null || selected < 0 || selected >= recording.getClips().size()) {
            injectText(RecorderSetting.ENTRY_CLIP, RecorderSetting.TEXT_NO_CLIP);
            return;
        }

        RecordingClipStruct clip = recording.getClips().get(selected);
        RecordingTakeStruct take = clip.getTake();

        injectHeader(RecorderSetting.ENTRY_CLIP, take.getName());
        injectText(RecorderSetting.ENTRY_CLIP, String.format(RecorderSetting.FORMAT_CLIP_PLACE,
                clip.getLane() + 1, formatTime(clip.getStart()), formatTime(clip.getEnd())));
        injectText(RecorderSetting.ENTRY_CLIP, String.format(RecorderSetting.FORMAT_CLIP_SOURCE,
                formatTime(clip.getInPoint()), formatTime(clip.getOutPoint())));
        injectHeader(RecorderSetting.ENTRY_CLIP, RecorderSetting.TEXT_HEADER_PLAYS);

        for (RecorderChannel channel : RecorderChannel.VALUES)
            if (take.holds(channel))
                injectSetting(
                        RecorderSetting.ENTRY_CLIP,
                        channel.getLabel(),
                        toOnOff(clip.holds(channel)),
                        EditorSetting.RECORDER_KEY_CLIP_CHANNEL_PREFIX + channel.getKey());

        injectHeader(RecorderSetting.ENTRY_CLIP, RecorderSetting.TEXT_HEADER_EDIT);
        injectSetting(RecorderSetting.ENTRY_CLIP, RecorderSetting.TEXT_MUTE, toOnOff(clip.isMuted()),
                EditorSetting.RECORDER_KEY_CLIP_MUTE);
        injectSetting(RecorderSetting.ENTRY_CLIP, RecorderSetting.TEXT_TIME_RAMP, clip.getTimeRampSeconds() > 0f
                ? String.format(RecorderSetting.FORMAT_RAMP, clip.getTimeRampSeconds())
                : RecorderSetting.TEXT_RAMP_CUT,
                EditorSetting.RECORDER_KEY_CLIP_RAMP);
        injectAction(RecorderSetting.ENTRY_CLIP, RecorderSetting.TEXT_TRIM_START,
                EditorSetting.RECORDER_KEY_CLIP_TRIM_START);
        injectAction(RecorderSetting.ENTRY_CLIP, RecorderSetting.TEXT_TRIM_END,
                EditorSetting.RECORDER_KEY_CLIP_TRIM_END);
        injectAction(RecorderSetting.ENTRY_CLIP, RecorderSetting.TEXT_SPLIT, EditorSetting.RECORDER_KEY_CLIP_SPLIT);
        injectAction(RecorderSetting.ENTRY_CLIP, RecorderSetting.TEXT_STAMP, EditorSetting.RECORDER_KEY_CLIP_STAMP);
        injectAction(RecorderSetting.ENTRY_CLIP, RecorderSetting.TEXT_REMOVE_CLIP,
                EditorSetting.RECORDER_KEY_CLIP_REMOVE);
    }

    // Markers \\

    private void refreshMarkers() {

        menuManager.ejectAll(recorderMenu, RecorderSetting.ENTRY_MARKERS);

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null || recording.getMarkers().isEmpty()) {
            injectText(RecorderSetting.ENTRY_MARKERS, RecorderSetting.TEXT_NO_MARKERS);
            return;
        }

        ObjectArrayList<RecordingMarkerStruct> markers = recording.getMarkers();

        for (int i = 0; i < markers.size(); i++) {

            RecordingMarkerStruct marker = markers.get(i);
            String markerKey = Integer.toString(i);
            ElementInstance row = menuManager.inject(
                    recorderMenu,
                    RecorderSetting.ENTRY_MARKERS,
                    i == recorderManager.getSelectedMarker()
                            ? RecorderSetting.TEMPLATE_MARKER_ROW_SELECTED
                            : RecorderSetting.TEMPLATE_MARKER_ROW);

            row.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(String.format(
                    RecorderSetting.FORMAT_MARKER, formatTime(marker.getTime()), marker.getName()));
            row.findChildById(RecorderSetting.ELEMENT_ROW_SELECT).setActionArgOverride(markerKey);
            row.findChildById(RecorderSetting.ELEMENT_ROW_RENAME).setActionArgOverride(markerKey);
            row.findChildById(RecorderSetting.ELEMENT_ROW_DELETE).setActionArgOverride(markerKey);
        }
    }

    // Timeline \\

    private void refreshTimeline() {

        menuManager.ejectAll(recorderMenu, EditorSetting.RECORDER_ENTRY_CLIP_LANES);
        menuManager.ejectAll(recorderMenu, RecorderSetting.ENTRY_MARKER_TICKS);

        RecordingInstance recording = recorderManager.getRecording();
        float span = recorderManager.resolveTimelineSpan();

        this.shownSpan = span;
        this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;
        this.ghostStep = EngineSetting.INDEX_NOT_FOUND;

        if (recording == null) {
            rangeStartMarker.setSizeOverride(hiddenSize);
            rangeEndMarker.setSizeOverride(hiddenSize);
            return;
        }

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = 0; i < clips.size(); i++)
            injectClipBar(clips.get(i), i, span);

        ObjectArrayList<RecordingMarkerStruct> markers = recording.getMarkers();

        for (int i = 0; i < markers.size(); i++) {

            DimensionVector2Struct position = timelinePositions[toTimelineStep(markers.get(i).getTime(), span)];

            menuManager.inject(
                    recorderMenu,
                    RecorderSetting.ENTRY_MARKER_TICKS,
                    RecorderSetting.TEMPLATE_MARKER_TICK,
                    tick -> tick.setPositionOverride(position));
        }

        RecordingExportStruct exportStruct = recording.getExportStruct();

        placeRangeMarker(rangeStartMarker, exportStruct.hasRangeStart(), exportStruct.getRangeStart(), span);
        placeRangeMarker(rangeEndMarker, exportStruct.hasRangeEnd(), exportStruct.getRangeEnd(), span);
    }

    // One node per clip across the span it plays, on its own lane, named after its take
    private void injectClipBar(RecordingClipStruct clip, int clipIndex, float span) {

        DimensionVector2Struct position = resolveLanePosition(clip.getLane(), toTimelineStep(clip.getStart(), span));
        DimensionVector2Struct size = resolveBarSize(clip.getLength(), span);
        String template = clipIndex == recorderManager.getSelectedClip()
                ? RecorderSetting.TEMPLATE_CLIP_BAR_SELECTED
                : clip.isMuted() ? RecorderSetting.TEMPLATE_CLIP_BAR_MUTED : RecorderSetting.TEMPLATE_CLIP_BAR;

        ElementInstance bar = menuManager.inject(recorderMenu, EditorSetting.RECORDER_ENTRY_CLIP_LANES, template,
                injected -> {
                    injected.setPositionOverride(position);
                    injected.setSizeOverride(size);
                });

        bar.findChildById(RecorderSetting.ELEMENT_CLIP_LABEL).setFontText(clip.getTake().getName());
    }

    private void placeRangeMarker(ElementInstance marker, boolean set, float time, float span) {

        if (!set) {
            marker.setSizeOverride(hiddenSize);
            return;
        }

        marker.setSizeOverride(rangeMarkerSize);
        marker.setPositionOverride(timelinePositions[toTimelineStep(time, span)]);
    }

    private void movePlayhead() {

        int step = toTimelineStep(recorderManager.getPlayhead(), shownSpan);

        if (step == shownPlayheadStep)
            return;

        this.shownPlayheadStep = step;
        playhead.setPositionOverride(timelinePositions[step]);
        lanePlayhead.setPositionOverride(timelinePositions[step]);
    }

    // Where the take or clip being dragged would land, shown only once the drag has travelled over the lanes
    private void moveGhost() {

        RecordingDragStruct drag = recorderManager.getDrag();
        boolean shown = drag.isActive() && drag.isMoved() && drag.isOverLanes();

        if (!shown) {

            if (ghostShown) {
                dragGhost.setSizeOverride(hiddenSize);
                this.ghostShown = false;
                this.ghostStep = EngineSetting.INDEX_NOT_FOUND;
            }

            return;
        }

        float length = recorderManager.resolveDragLength();

        if (!ghostShown || length != ghostLength || shownSpan != ghostSpan) {
            this.ghostLength = length;
            this.ghostSpan = shownSpan;
            this.ghostSize = resolveBarSize(length, shownSpan);
            dragGhost.setSizeOverride(ghostSize);
            this.ghostShown = true;
        }

        int step = toTimelineStep(drag.getLandingStart(), shownSpan);
        int lane = Math.clamp(drag.getLandingLane(), 0, EditorSetting.RECORDER_LANE_COUNT - 1);

        if (step == ghostStep && lane == ghostLane)
            return;

        this.ghostStep = step;
        this.ghostLane = lane;
        dragGhost.setPositionOverride(resolveLanePosition(lane, step));
    }

    // The place a bar starts on a lane, the highest lane drawn at the top, built the first time it is needed
    private DimensionVector2Struct resolveLanePosition(int lane, int step) {

        int clampedLane = Math.clamp(lane, 0, EditorSetting.RECORDER_LANE_COUNT - 1);
        DimensionVector2Struct position = lanePositions[clampedLane][step];

        if (position != null)
            return position;

        int row = EditorSetting.RECORDER_LANE_COUNT - 1 - clampedLane;

        position = new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(toPercent(step)),
                DimensionValueStruct.ofAbsolute(
                        -(row * RecorderSetting.LANE_PIXELS + RecorderSetting.CLIP_BAR_INSET_PIXELS)));
        lanePositions[clampedLane][step] = position;
        return position;
    }

    private DimensionVector2Struct resolveBarSize(float length, float span) {
        return new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(RecorderSetting.TIMELINE_PERCENT * length / span),
                DimensionValueStruct.ofAbsolute(RecorderSetting.CLIP_BAR_PIXELS));
    }

    private int toTimelineStep(float time, float span) {

        int step = Math.round(time / span * RecorderSetting.TIMELINE_STEPS);

        return Math.max(0, Math.min(RecorderSetting.TIMELINE_STEPS, step));
    }

    private float toPercent(int step) {
        return RecorderSetting.TIMELINE_PERCENT * step / RecorderSetting.TIMELINE_STEPS;
    }

    // Utility \\

    private void injectHeader(int entryPoint, String text) {
        menuManager.inject(recorderMenu, entryPoint, RecorderSetting.TEMPLATE_HEADER_ROW)
                .findChildById(RecorderSetting.ELEMENT_ROW_LABEL)
                .setFontText(text);
    }

    private ElementInstance injectText(int entryPoint, String text) {

        ElementInstance label = menuManager.inject(recorderMenu, entryPoint, RecorderSetting.TEMPLATE_TEXT_ROW)
                .findChildById(RecorderSetting.ELEMENT_ROW_LABEL);

        label.setFontText(text);
        return label;
    }

    private String toOnOff(boolean on) {
        return on ? RecorderSetting.TEXT_ON : RecorderSetting.TEXT_OFF;
    }

    private String formatTime(float seconds) {

        int minutes = (int) (seconds / RecorderSetting.SECONDS_PER_MINUTE);

        return String.format(
                RecorderSetting.FORMAT_TIME, minutes, seconds - minutes * RecorderSetting.SECONDS_PER_MINUTE);
    }
}
