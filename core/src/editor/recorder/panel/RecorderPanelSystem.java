package editor.recorder.panel;

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
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingLayerStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingMarkerStruct;
import editor.bootstrap.recorderpipeline.util.FfmpegUtility;
import editor.recorder.RecorderSetting;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and shows the recorder. When the
     * recorder's revision moves it refills every panel — the recordings in
     * the game's directory with the open one marked, the channels armed for
     * the next take and the export settings, every take with its channels,
     * span, mute and time ramp, every marker, and the timeline's take bars,
     * marker ticks and export range. The playhead follows the playhead every
     * frame from positions built once, so playback never allocates; the clock
     * text, the export's progress and a timeline still growing under a take
     * refresh a few times a second.
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
    private ElementInstance rangeStartMarker;
    private ElementInstance rangeEndMarker;
    private ElementInstance progressLabel;

    // Timeline
    private DimensionVector2Struct[] timelinePositions;
    private DimensionVector2Struct rangeMarkerSize;
    private DimensionVector2Struct hiddenSize;
    private int shownPlayheadStep;
    private float shownSpan;

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
                    DimensionValueStruct.ofPercent(
                            RecorderSetting.TIMELINE_PERCENT * i / RecorderSetting.TIMELINE_STEPS),
                    DimensionValueStruct.ofAbsolute(0f));

        this.rangeMarkerSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(RecorderSetting.RANGE_MARKER_WIDTH_PIXELS),
                DimensionValueStruct.ofAbsolute(RecorderSetting.RANGE_MARKER_HEIGHT_PIXELS));
        this.hiddenSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(0f),
                DimensionValueStruct.ofAbsolute(0f));
        this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;

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
        this.recorderMenu = menuManager.openMenu(RecorderSetting.MENU_RECORDER, window);

        this.statusLabel = requireEntryPoint(RecorderSetting.ENTRY_STATUS);
        this.clockLabel = requireEntryPoint(RecorderSetting.ENTRY_CLOCK);
        this.playhead = requireEntryPoint(RecorderSetting.ENTRY_PLAYHEAD);
        this.rangeStartMarker = requireEntryPoint(RecorderSetting.ENTRY_RANGE_START);
        this.rangeEndMarker = requireEntryPoint(RecorderSetting.ENTRY_RANGE_END);

        requireEntryPoint(RecorderSetting.ENTRY_RECORDINGS);
        requireEntryPoint(RecorderSetting.ENTRY_SETTINGS);
        requireEntryPoint(RecorderSetting.ENTRY_LAYERS);
        requireEntryPoint(RecorderSetting.ENTRY_MARKERS);
        requireEntryPoint(RecorderSetting.ENTRY_LAYER_LANE);
        requireEntryPoint(RecorderSetting.ENTRY_MARKER_TICKS);
    }

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = recorderMenu.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Recorder menu '" + RecorderSetting.MENU_RECORDER + "' has no entry point "
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

            if (resolveSpan() != shownSpan)
                refreshTimeline();
        }

        movePlayhead();
    }

    private void refreshAll() {
        refreshStatus();
        refreshRecordings();
        refreshSettings();
        refreshLayers();
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
                formatTime(resolveSpan())));
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
                    channel.getLabel(),
                    channel.isIn(armedChannels) ? RecorderSetting.TEXT_ARMED : RecorderSetting.TEXT_OFF,
                    resolveArmKey(channel));

        injectHeader(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_HEADER_EXPORT);

        if (!recorderManager.hasRecording()) {
            injectText(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_NO_RECORDING);
            return;
        }

        RecordingExportStruct exportStruct = recorderManager.getRecording().getExportStruct();

        injectSetting(RecorderSetting.TEXT_RESOLUTION,
                String.format(RecorderSetting.FORMAT_RESOLUTION, exportStruct.getWidth(), exportStruct.getHeight()),
                EditorSetting.RECORDER_KEY_RESOLUTION);
        injectSetting(RecorderSetting.TEXT_FRAME_RATE,
                String.format(RecorderSetting.FORMAT_FRAME_RATE, exportStruct.getFrameRate()),
                EditorSetting.RECORDER_KEY_FRAME_RATE);
        injectSetting(RecorderSetting.TEXT_FORMAT, exportStruct.getFormat().getLabel(),
                EditorSetting.RECORDER_KEY_FORMAT);
        injectSetting(RecorderSetting.TEXT_MOTION_BLUR, exportStruct.getMotionBlurSamples() > 1
                ? String.format(RecorderSetting.FORMAT_MOTION_BLUR, exportStruct.getMotionBlurSamples())
                : RecorderSetting.TEXT_OFF,
                EditorSetting.RECORDER_KEY_MOTION_BLUR);
        injectSetting(RecorderSetting.TEXT_SUPERSAMPLE, toOnOff(exportStruct.isSupersampled()),
                EditorSetting.RECORDER_KEY_SUPERSAMPLE);
        injectSetting(RecorderSetting.TEXT_HUD, toOnOff(exportStruct.isHud()), EditorSetting.RECORDER_KEY_HUD);
        injectSetting(RecorderSetting.TEXT_RANGE_START, exportStruct.hasRangeStart()
                ? formatTime(exportStruct.getRangeStart())
                : RecorderSetting.TEXT_RANGE_FROM_START,
                EditorSetting.RECORDER_KEY_RANGE_START);
        injectSetting(RecorderSetting.TEXT_RANGE_END, exportStruct.hasRangeEnd()
                ? formatTime(exportStruct.getRangeEnd())
                : RecorderSetting.TEXT_RANGE_TO_END,
                EditorSetting.RECORDER_KEY_RANGE_END);
        injectAction(RecorderSetting.TEXT_CLEAR_RANGE, EditorSetting.RECORDER_KEY_RANGE_CLEAR);

        if (recorderManager.getState() == RecorderState.EXPORTING) {
            injectAction(RecorderSetting.TEXT_CANCEL_EXPORT, EditorSetting.RECORDER_KEY_CANCEL_EXPORT);
            this.progressLabel = injectText(RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEXT_EMPTY);
            refreshProgress();
            return;
        }

        injectAction(RecorderSetting.TEXT_EXPORT, EditorSetting.RECORDER_KEY_EXPORT);
        injectText(RecorderSetting.ENTRY_SETTINGS, FfmpegUtility.findExecutable(internal.path) != null
                ? RecorderSetting.TEXT_FFMPEG_FOUND
                : RecorderSetting.TEXT_FFMPEG_MISSING);
    }

    private String resolveArmKey(RecorderChannel channel) {
        return switch (channel) {
            case CAMERA -> EditorSetting.RECORDER_KEY_ARM_CAMERA;
            case PLAYER -> EditorSetting.RECORDER_KEY_ARM_PLAYER;
            case WORLD -> EditorSetting.RECORDER_KEY_ARM_WORLD;
            case TIME -> EditorSetting.RECORDER_KEY_ARM_TIME;
        };
    }

    private void injectSetting(String label, String value, String settingKey) {

        ElementInstance row = menuManager.inject(
                recorderMenu, RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEMPLATE_SETTING_ROW);

        row.setActionArgOverride(settingKey);
        row.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(label);
        row.findChildById(RecorderSetting.ELEMENT_ROW_VALUE).setFontText(value);
    }

    private void injectAction(String label, String settingKey) {

        ElementInstance row = menuManager.inject(
                recorderMenu, RecorderSetting.ENTRY_SETTINGS, RecorderSetting.TEMPLATE_ACTION_ROW);

        row.setActionArgOverride(settingKey);
        row.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(label);
    }

    // Takes \\

    private void refreshLayers() {

        menuManager.ejectAll(recorderMenu, RecorderSetting.ENTRY_LAYERS);

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null || recording.getLayers().isEmpty()) {
            injectText(RecorderSetting.ENTRY_LAYERS, RecorderSetting.TEXT_NO_TAKES);
            return;
        }

        ObjectArrayList<RecordingLayerStruct> layers = recording.getLayers();

        for (int i = 0; i < layers.size(); i++) {

            RecordingLayerStruct layer = layers.get(i);
            String layerKey = Integer.toString(i);
            ElementInstance row = menuManager.inject(
                    recorderMenu,
                    RecorderSetting.ENTRY_LAYERS,
                    i == recorderManager.getSelectedLayer()
                            ? RecorderSetting.TEMPLATE_LAYER_ROW_SELECTED
                            : RecorderSetting.TEMPLATE_LAYER_ROW);

            row.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(String.format(
                    RecorderSetting.FORMAT_LAYER,
                    layer.getName(),
                    describeChannels(layer.getChannelMask()),
                    formatTime(layer.getStartTime()),
                    formatTime(layer.getEndTime())));
            row.findChildById(RecorderSetting.ELEMENT_ROW_MUTE_LABEL).setFontText(
                    layer.isMuted() ? RecorderSetting.TEXT_MUTED : RecorderSetting.TEXT_MUTE);
            row.findChildById(RecorderSetting.ELEMENT_ROW_RAMP_LABEL).setFontText(layer.getTimeRampSeconds() > 0f
                    ? String.format(RecorderSetting.FORMAT_RAMP, layer.getTimeRampSeconds())
                    : RecorderSetting.TEXT_RAMP_CUT);

            bindRow(row, layerKey);
            row.findChildById(RecorderSetting.ELEMENT_ROW_MUTE).setActionArgOverride(layerKey);
            row.findChildById(RecorderSetting.ELEMENT_ROW_RAMP).setActionArgOverride(layerKey);
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
            ElementInstance row = menuManager.inject(
                    recorderMenu,
                    RecorderSetting.ENTRY_MARKERS,
                    i == recorderManager.getSelectedMarker()
                            ? RecorderSetting.TEMPLATE_MARKER_ROW_SELECTED
                            : RecorderSetting.TEMPLATE_MARKER_ROW);

            row.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(String.format(
                    RecorderSetting.FORMAT_MARKER, formatTime(marker.getTime()), marker.getName()));
            bindRow(row, Integer.toString(i));
        }
    }

    // The row's select, rename and delete buttons all hand back the row's index
    private void bindRow(ElementInstance row, String rowKey) {
        row.findChildById(RecorderSetting.ELEMENT_ROW_SELECT).setActionArgOverride(rowKey);
        row.findChildById(RecorderSetting.ELEMENT_ROW_RENAME).setActionArgOverride(rowKey);
        row.findChildById(RecorderSetting.ELEMENT_ROW_DELETE).setActionArgOverride(rowKey);
    }

    // Timeline \\

    private void refreshTimeline() {

        menuManager.ejectAll(recorderMenu, RecorderSetting.ENTRY_LAYER_LANE);
        menuManager.ejectAll(recorderMenu, RecorderSetting.ENTRY_MARKER_TICKS);

        RecordingInstance recording = recorderManager.getRecording();
        float span = resolveSpan();

        this.shownSpan = span;
        this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;

        if (recording == null) {
            rangeStartMarker.setSizeOverride(hiddenSize);
            rangeEndMarker.setSizeOverride(hiddenSize);
            return;
        }

        ObjectArrayList<RecordingLayerStruct> layers = recording.getLayers();

        for (int i = 0; i < layers.size(); i++)
            injectLayerBar(layers.get(i), i, span);

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

    // One bar per take across the span it covers, stacked a few lanes deep so overlapping takes stay apart
    private void injectLayerBar(RecordingLayerStruct layer, int layerIndex, float span) {

        float start = RecorderSetting.TIMELINE_PERCENT * layer.getStartTime() / span;
        float width = RecorderSetting.TIMELINE_PERCENT * layer.getDuration() / span;
        DimensionVector2Struct position = new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(start),
                DimensionValueStruct.ofAbsolute(
                        -(layerIndex % RecorderSetting.LANE_ROWS) * RecorderSetting.LANE_ROW_PIXELS));
        DimensionVector2Struct size = new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(width),
                DimensionValueStruct.ofAbsolute(RecorderSetting.LANE_BAR_PIXELS));

        menuManager.inject(
                recorderMenu,
                RecorderSetting.ENTRY_LAYER_LANE,
                layer.isMuted() ? RecorderSetting.TEMPLATE_LAYER_BAR_MUTED : RecorderSetting.TEMPLATE_LAYER_BAR,
                bar -> {
                    bar.setPositionOverride(position);
                    bar.setSizeOverride(size);
                });
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
    }

    // The timeline runs the recording's length, longer while a take records past it
    private float resolveSpan() {

        float span = RecorderSetting.TIMELINE_MIN_SPAN_SECONDS;

        if (recorderManager.hasRecording())
            span = Math.max(span, recorderManager.getRecording().getDuration());

        return Math.max(span, recorderManager.getPlayhead());
    }

    private int toTimelineStep(float time, float span) {

        int step = Math.round(time / span * RecorderSetting.TIMELINE_STEPS);

        return Math.max(0, Math.min(RecorderSetting.TIMELINE_STEPS, step));
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
