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
import editor.bootstrap.recorderpipeline.recording.RecorderDragKind;
import editor.bootstrap.recorderpipeline.recording.RecorderNodeKind;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingDragStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingMarkerStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingNodeStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingViewStruct;
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
     * what it plays, the node selected on it and every edit it takes, every
     * marker, and the stretch of the timeline in view: its ruler and marker
     * ticks, its export range, and its clips on their lanes, highest on top,
     * each with what the player does along it — the movement it is in along
     * its lower row and every gesture of its hands along its upper row. Only
     * what lies in view is drawn, cut at the view's edges, and the view's
     * zooming and scrolling redraws the timeline alone. Both playheads follow
     * the playhead every frame from positions built once, hidden while it is
     * out of view, and what is being dragged shows where it would land — a
     * span on the lane it would really take, or a line at the moment an edge,
     * a cut or a node would move to — from positions built the first time
     * each is needed, so neither playback nor a drag allocates; the cut tool
     * shows the same line where it would cut under the pointer, and its
     * button says whether it is on. The clock text, the export's progress, a
     * timeline still growing under a take and a resized timeline refresh a
     * few times a second.
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
    private ElementInstance viewLabel;
    private ElementInstance cutLabel;
    private ElementInstance progressLabel;

    // Timeline
    private DimensionVector2Struct[] timelinePositions;
    private DimensionVector2Struct[][] lanePositions;
    private DimensionVector2Struct[] barSizes;
    private DimensionVector2Struct rangeMarkerSize;
    private DimensionVector2Struct playheadSize;
    private DimensionVector2Struct lanePlayheadSize;
    private DimensionVector2Struct hiddenSize;
    private int shownPlayheadStep;
    private boolean playheadShown;

    // View
    private float shownViewStart;
    private float shownViewSpan;
    private float shownLanesWidth;
    private int shownViewRevision;

    // Ghost
    private DimensionVector2Struct ghostSize;
    private DimensionVector2Struct edgeGhostSize;
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
                [RecorderSetting.TIMELINE_STEPS + 2 * RecorderSetting.GHOST_OVERHANG_STEPS + 1];
        this.barSizes = new DimensionVector2Struct[RecorderSetting.TIMELINE_STEPS + 1];
        this.rangeMarkerSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(RecorderSetting.RANGE_MARKER_WIDTH_PIXELS),
                DimensionValueStruct.ofAbsolute(RecorderSetting.RANGE_MARKER_HEIGHT_PIXELS));
        this.playheadSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(RecorderSetting.PLAYHEAD_WIDTH_PIXELS),
                DimensionValueStruct.ofAbsolute(RecorderSetting.PLAYHEAD_HEIGHT_PIXELS));
        this.lanePlayheadSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(RecorderSetting.PLAYHEAD_WIDTH_PIXELS),
                DimensionValueStruct.ofPercent(RecorderSetting.TIMELINE_PERCENT));
        this.hiddenSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(0f),
                DimensionValueStruct.ofAbsolute(0f));
        this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;
        this.playheadShown = true;

        // View
        this.shownViewRevision = EngineSetting.INDEX_NOT_FOUND;

        // Ghost
        this.edgeGhostSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(RecorderSetting.EDGE_GHOST_WIDTH_PIXELS),
                DimensionValueStruct.ofAbsolute(RecorderSetting.CLIP_BAR_PIXELS));
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
        this.viewLabel = requireEntryPoint(RecorderSetting.ENTRY_VIEW);
        this.cutLabel = requireEntryPoint(RecorderSetting.ENTRY_CUT_LABEL);

        requireEntryPoint(RecorderSetting.ENTRY_RECORDINGS);
        requireEntryPoint(RecorderSetting.ENTRY_SETTINGS);
        requireEntryPoint(RecorderSetting.ENTRY_TAKES);
        requireEntryPoint(RecorderSetting.ENTRY_MARKERS);
        requireEntryPoint(RecorderSetting.ENTRY_CLIP);
        requireEntryPoint(EditorSetting.RECORDER_ENTRY_RULER);
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
        } else if (recorderManager.getViewRevision() != shownViewRevision)
            refreshTimeline();

        this.refreshElapsed += internal.getDeltaTime();

        if (refreshElapsed >= RecorderSetting.REFRESH_SECONDS) {
            this.refreshElapsed = 0f;
            refreshClock();
            refreshProgress();

            if (isTimelineStale())
                refreshTimeline();
        }

        movePlayhead();
        moveGhost();
    }

    private void refreshAll() {
        refreshStatus();
        refreshTools();
        refreshRecordings();
        refreshSettings();
        refreshTakes();
        refreshClip();
        refreshMarkers();
        refreshTimeline();
        refreshClock();
    }

    // Accessible \\

    MenuInstance getRecorderMenu() {
        return recorderMenu;
    }

    // Status \\

    private void refreshStatus() {
        statusLabel.setFontText(recorderManager.getStatusText());
    }

    private void refreshTools() {
        cutLabel.setFontText(recorderManager.isCutting() ? RecorderSetting.TEXT_CUT_ON : RecorderSetting.TEXT_CUT_OFF);
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
                formatTime(recorderManager.resolveLength())));
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

    void injectSetting(int entryPoint, String label, String value, String settingKey) {

        ElementInstance row = menuManager.inject(recorderMenu, entryPoint, RecorderSetting.TEMPLATE_SETTING_ROW);

        row.setActionArgOverride(settingKey);
        row.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(label);
        row.findChildById(RecorderSetting.ELEMENT_ROW_VALUE).setFontText(value);
    }

    void injectAction(int entryPoint, String label, String settingKey) {

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
        refreshNode(RecorderSetting.ENTRY_CLIP);
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

    // The node selected on the clip: what its character does across it, and how it is changed; the Clip panel and the
    // Keys tab's inspector both show it
    void refreshNode(int entryPoint) {

        int selected = recorderManager.getSelectedNode();

        if (selected < 0 || selected >= recorderManager.getNodes().size())
            return;

        RecordingNodeStruct node = recorderManager.getNodes().get(selected);

        injectHeader(entryPoint, String.format(RecorderSetting.FORMAT_NODE_HEADER,
                RecorderSetting.TEXT_HEADER_NODE, node.getTrack().getTrackLabel()));
        injectText(entryPoint, String.format(RecorderSetting.FORMAT_NODE,
                node.getLabel(), formatTime(node.getStart()), formatTime(node.getEnd())));

        if (node.getKind() == RecorderNodeKind.MOVEMENT) {
            injectSetting(entryPoint, RecorderSetting.TEXT_NODE_DOING, node.getLabel(),
                    EditorSetting.RECORDER_KEY_NODE_NEXT);
            injectAction(entryPoint, RecorderSetting.TEXT_NODE_PREVIOUS,
                    EditorSetting.RECORDER_KEY_NODE_PREVIOUS);
            injectAction(entryPoint, RecorderSetting.TEXT_REMOVE_MOVEMENT,
                    EditorSetting.RECORDER_KEY_NODE_REMOVE);
        } else
            injectAction(entryPoint, RecorderSetting.TEXT_REMOVE_GESTURE,
                    EditorSetting.RECORDER_KEY_NODE_REMOVE);

        if (node.isMovable())
            injectText(entryPoint, node.getKind() == RecorderNodeKind.MOVEMENT
                    ? RecorderSetting.TEXT_NODE_MOVE_HINT
                    : RecorderSetting.TEXT_GESTURE_MOVE_HINT);

        injectText(entryPoint, RecorderSetting.TEXT_NODE_SHARED);
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

    // The timeline is drawn again when the view has moved since, or the lanes were resized
    private boolean isTimelineStale() {

        RecordingViewStruct view = recorderManager.getView();

        return view.getStart() != shownViewStart
                || view.getSpan() != shownViewSpan
                || resolveLanesWidth() != shownLanesWidth;
    }

    private void refreshTimeline() {

        menuManager.ejectAll(recorderMenu, EditorSetting.RECORDER_ENTRY_CLIP_LANES);
        menuManager.ejectAll(recorderMenu, EditorSetting.RECORDER_ENTRY_RULER);

        RecordingInstance recording = recorderManager.getRecording();
        RecordingViewStruct view = recorderManager.getView();
        float lanesWidth = resolveLanesWidth();

        this.shownViewStart = view.getStart();
        this.shownViewSpan = view.getSpan();
        this.shownLanesWidth = lanesWidth;
        this.shownViewRevision = recorderManager.getViewRevision();
        this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;
        this.ghostStep = EngineSetting.INDEX_NOT_FOUND;

        viewLabel.setFontText(view.isFitted()
                ? RecorderSetting.TEXT_VIEW_FIT
                : String.format(RecorderSetting.FORMAT_VIEW, formatTime(view.getStart()), formatTime(view.getEnd())));
        injectRulerTicks(view, lanesWidth);

        if (recording == null) {
            rangeStartMarker.setSizeOverride(hiddenSize);
            rangeEndMarker.setSizeOverride(hiddenSize);
            return;
        }

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = 0; i < clips.size(); i++)
            injectClipBar(clips.get(i), i, view);

        ObjectArrayList<RecordingNodeStruct> nodes = recorderManager.getNodes();

        for (int i = 0; i < nodes.size(); i++)
            injectNode(nodes.get(i), i, recording, view, lanesWidth);

        ObjectArrayList<RecordingMarkerStruct> markers = recording.getMarkers();

        for (int i = 0; i < markers.size(); i++) {

            if (!view.contains(markers.get(i).getTime()))
                continue;

            DimensionVector2Struct position = timelinePositions[toViewStep(markers.get(i).getTime())];

            menuManager.inject(
                    recorderMenu,
                    EditorSetting.RECORDER_ENTRY_RULER,
                    RecorderSetting.TEMPLATE_MARKER_TICK,
                    tick -> tick.setPositionOverride(position));
        }

        RecordingExportStruct exportStruct = recording.getExportStruct();

        placeRangeMarker(rangeStartMarker, exportStruct.hasRangeStart(), exportStruct.getRangeStart());
        placeRangeMarker(rangeEndMarker, exportStruct.hasRangeEnd(), exportStruct.getRangeEnd());
    }

    // A tick with its time at every whole interval in view, the interval the closest that keeps the labels apart
    private void injectRulerTicks(RecordingViewStruct view, float lanesWidth) {

        float interval = resolveRulerInterval(view.getSpan(), lanesWidth);

        for (int i = (int) Math.ceil(view.getStart() / interval); i * interval <= view.getEnd(); i++) {

            float time = i * interval;
            DimensionVector2Struct position = timelinePositions[toViewStep(time)];
            ElementInstance tick = menuManager.inject(
                    recorderMenu,
                    EditorSetting.RECORDER_ENTRY_RULER,
                    RecorderSetting.TEMPLATE_RULER_TICK,
                    injected -> injected.setPositionOverride(position));

            tick.findChildById(RecorderSetting.ELEMENT_RULER_LABEL).setFontText(formatTime(time));
        }
    }

    private float resolveRulerInterval(float span, float lanesWidth) {

        float[] intervals = RecorderSetting.RULER_INTERVAL_SECONDS;
        float wanted = lanesWidth > 0f
                ? span * RecorderSetting.RULER_MIN_TICK_PIXELS / lanesWidth
                : span / RecorderSetting.RULER_FALLBACK_TICKS;

        for (int i = 0; i < intervals.length; i++)
            if (intervals[i] >= wanted)
                return intervals[i];

        float longest = intervals[intervals.length - 1];

        return (float) Math.ceil(wanted / longest) * longest;
    }

    // One bar per clip across the part of it in view, on its own lane, named after its take
    private void injectClipBar(RecordingClipStruct clip, int clipIndex, RecordingViewStruct view) {

        if (!clip.overlaps(view.getStart(), view.getEnd()))
            return;

        int fromStep = toViewStep(clip.getStart());
        DimensionVector2Struct position = resolveLanePosition(clip.getLane(), fromStep);
        DimensionVector2Struct size = resolveBarSize(toViewStep(clip.getEnd()) - fromStep);
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

    // One of the player's nodes across the part of its run in view, on its clip's lane over the clip's bar; too
    // narrow to see, it is left out until the view is zoomed in on it
    private void injectNode(
            RecordingNodeStruct node,
            int nodeIndex,
            RecordingInstance recording,
            RecordingViewStruct view,
            float lanesWidth) {

        RecordingClipStruct clip = recording.getClips().get(node.getClipIndex());

        if (node.getTrack() != clip.getTake().getPlayerTrack()
                || node.getEnd() <= view.getStart() || node.getStart() >= view.getEnd())
            return;

        float shownSeconds = Math.min(node.getEnd(), view.getEnd()) - Math.max(node.getStart(), view.getStart());

        if (lanesWidth > 0f && shownSeconds / view.getSpan() * lanesWidth < RecorderSetting.NODE_MIN_PIXELS)
            return;

        int fromStep = toViewStep(node.getStart());
        int steps = toViewStep(node.getEnd()) - fromStep;

        if (steps <= 0)
            return;

        DimensionVector2Struct position = resolveLanePosition(clip.getLane(), fromStep);
        DimensionVector2Struct size = resolveBarSize(steps);
        boolean selected = nodeIndex == recorderManager.getSelectedNode();
        String template = node.getKind() == RecorderNodeKind.MOVEMENT
                ? selected ? RecorderSetting.TEMPLATE_NODE_MOVEMENT_SELECTED : RecorderSetting.TEMPLATE_NODE_MOVEMENT
                : selected ? RecorderSetting.TEMPLATE_NODE_GESTURE_SELECTED : RecorderSetting.TEMPLATE_NODE_GESTURE;

        ElementInstance element = menuManager.inject(recorderMenu, EditorSetting.RECORDER_ENTRY_CLIP_LANES, template,
                injected -> {
                    injected.setPositionOverride(position);
                    injected.setSizeOverride(size);
                });

        element.findChildById(RecorderSetting.ELEMENT_NODE_LABEL).setFontText(node.getLabel());
    }

    private void placeRangeMarker(ElementInstance marker, boolean set, float time) {

        if (!set || !recorderManager.getView().contains(time)) {
            marker.setSizeOverride(hiddenSize);
            return;
        }

        marker.setSizeOverride(rangeMarkerSize);
        marker.setPositionOverride(timelinePositions[toViewStep(time)]);
    }

    // Both playheads at the playhead, hidden while it lies out of view
    private void movePlayhead() {

        float time = recorderManager.getPlayhead();
        boolean shown = recorderManager.getView().contains(time);

        if (shown != playheadShown) {
            this.playheadShown = shown;
            this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;
            playhead.setSizeOverride(shown ? playheadSize : hiddenSize);
            lanePlayhead.setSizeOverride(shown ? lanePlayheadSize : hiddenSize);
        }

        if (!shown)
            return;

        int step = toViewStep(time);

        if (step == shownPlayheadStep)
            return;

        this.shownPlayheadStep = step;
        playhead.setPositionOverride(timelinePositions[step]);
        lanePlayhead.setPositionOverride(timelinePositions[step]);
    }

    // Where what is dragged would land, shown once the drag has travelled, or at once for a cut: a span over the
    // lanes on the lane it would really take, or a line at the moment an edge, a cut or a node would move to; with
    // nothing dragged, the cut tool's line where it would cut under the pointer
    private void moveGhost() {

        RecordingDragStruct drag = recorderManager.getDrag();
        RecorderDragKind kind = drag.getKind();
        boolean dragShown = drag.isActive() && (drag.isMoved() || kind == RecorderDragKind.CUT)
                && (kind.isSpan() && drag.isOverLanes() || kind.isMoment());
        int cutClip = recorderManager.getCutHoverClip();
        RecordingInstance recording = recorderManager.getRecording();
        boolean cutShown = !drag.isActive() && recording != null
                && cutClip >= 0 && cutClip < recording.getClips().size();
        boolean shown = dragShown || cutShown;

        if (!shown) {

            if (ghostShown) {
                dragGhost.setSizeOverride(hiddenSize);
                this.ghostShown = false;
                this.ghostStep = EngineSetting.INDEX_NOT_FOUND;
            }

            return;
        }

        boolean spanShown = dragShown && kind.isSpan();
        float length = spanShown ? recorderManager.resolveDragLength() : EditorSetting.RECORDER_TIME_UNSET;
        float span = recorderManager.getView().getSpan();

        if (!ghostShown || length != ghostLength || span != ghostSpan) {
            this.ghostLength = length;
            this.ghostSpan = span;
            this.ghostSize = spanShown ? resolveGhostSize(length, span) : edgeGhostSize;
            dragGhost.setSizeOverride(ghostSize);
            this.ghostShown = true;
        }

        int step = toGhostStep(dragShown ? drag.getLandingStart() : recorderManager.getCutHoverTime());
        int lane = Math.clamp(dragShown ? drag.getLandingLane() : recording.getClips().get(cutClip).getLane(),
                0, EditorSetting.RECORDER_LANE_COUNT - 1);

        if (step == ghostStep && lane == ghostLane)
            return;

        this.ghostStep = step;
        this.ghostLane = lane;
        dragGhost.setPositionOverride(resolveLanePosition(lane, step));
    }

    // The place a bar starts on a lane, the highest lane drawn at the top, built the first time it is needed; a step
    // may lie a view's width to either side, where only a ghost is drawn
    private DimensionVector2Struct resolveLanePosition(int lane, int step) {

        int clampedLane = Math.clamp(lane, 0, EditorSetting.RECORDER_LANE_COUNT - 1);
        int slot = step + RecorderSetting.GHOST_OVERHANG_STEPS;
        DimensionVector2Struct position = lanePositions[clampedLane][slot];

        if (position != null)
            return position;

        int row = EditorSetting.RECORDER_LANE_COUNT - 1 - clampedLane;

        position = new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(toPercent(step)),
                DimensionValueStruct.ofAbsolute(-(row * EditorSetting.RECORDER_LANE_PIXELS
                        + EditorSetting.RECORDER_CLIP_BAR_INSET_PIXELS)));
        lanePositions[clampedLane][slot] = position;
        return position;
    }

    // A bar across a number of steps of the view, built the first time it is needed
    private DimensionVector2Struct resolveBarSize(int steps) {

        DimensionVector2Struct size = barSizes[steps];

        if (size != null)
            return size;

        size = new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(toPercent(steps)),
                DimensionValueStruct.ofAbsolute(RecorderSetting.CLIP_BAR_PIXELS));
        barSizes[steps] = size;
        return size;
    }

    // A dragged span may run past the view, so it is sized as it is, once each drag
    private DimensionVector2Struct resolveGhostSize(float length, float span) {
        return new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(RecorderSetting.TIMELINE_PERCENT * length / span),
                DimensionValueStruct.ofAbsolute(RecorderSetting.CLIP_BAR_PIXELS));
    }

    private float resolveLanesWidth() {
        return recorderMenu.getEntryPoint(EditorSetting.RECORDER_ENTRY_CLIP_LANES).getComputedW();
    }

    // How far across the view a moment lies, in steps kept within it
    private int toViewStep(float time) {

        int step = Math.round(recorderManager.getView().toFraction(time) * RecorderSetting.TIMELINE_STEPS);

        return Math.clamp(step, 0, RecorderSetting.TIMELINE_STEPS);
    }

    // How far across the view a ghost's start lies, in steps that may reach a view's width past either side
    private int toGhostStep(float time) {

        int step = Math.round(recorderManager.getView().toFraction(time) * RecorderSetting.TIMELINE_STEPS);

        return Math.clamp(step, -RecorderSetting.GHOST_OVERHANG_STEPS,
                RecorderSetting.TIMELINE_STEPS + RecorderSetting.GHOST_OVERHANG_STEPS);
    }

    private float toPercent(int step) {
        return RecorderSetting.TIMELINE_PERCENT * step / RecorderSetting.TIMELINE_STEPS;
    }

    // Utility \\

    void injectHeader(int entryPoint, String text) {
        menuManager.inject(recorderMenu, entryPoint, RecorderSetting.TEMPLATE_HEADER_ROW)
                .findChildById(RecorderSetting.ELEMENT_ROW_LABEL)
                .setFontText(text);
    }

    ElementInstance injectText(int entryPoint, String text) {

        ElementInstance label = menuManager.inject(recorderMenu, entryPoint, RecorderSetting.TEMPLATE_TEXT_ROW)
                .findChildById(RecorderSetting.ELEMENT_ROW_LABEL);

        label.setFontText(text);
        return label;
    }

    private String toOnOff(boolean on) {
        return on ? RecorderSetting.TEXT_ON : RecorderSetting.TEXT_OFF;
    }

    String formatTime(float seconds) {

        int minutes = (int) (seconds / RecorderSetting.SECONDS_PER_MINUTE);

        return String.format(
                RecorderSetting.FORMAT_TIME, minutes, seconds - minutes * RecorderSetting.SECONDS_PER_MINUTE);
    }
}
