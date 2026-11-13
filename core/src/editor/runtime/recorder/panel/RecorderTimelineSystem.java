package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderClipEdge;
import editor.bootstrap.recorderpipeline.recording.RecorderDragKind;
import editor.bootstrap.recorderpipeline.recording.RecorderNodeKind;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingDragStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingNodeStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingViewStruct;
import editor.runtime.recorder.RecorderSetting;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderTimelineSystem extends SystemPackage {

    /*
     * Shows the Timeline tab: when the recorder's revision moves it redraws
     * the status line, the title naming the recording open and the ones it
     * was opened inside, and the stretch of the timeline in view —
     * its ruler's ticks, its export range, and its clips on their lanes,
     * highest on top, each with what the player does along it — the
     * movement it is in along its lower row and every gesture of its hands
     * along its upper row — and a mark across each transition laid on it,
     * named after the transition; every clip selected is drawn selected, a
     * clip that is only heard — a sound laid from the sound library, a song,
     * a notes clip — is drawn in a shade of its own, and each is named after
     * its source, a nested recording's name in the form its kind gives it.
     * The transport, the Keys, the Bin, the Assets and the settings are
     * their own tabs beside it, and the recordings themselves are listed in
     * the Recordings tool. Only what lies in view is drawn,
     * cut at the view's edges, and the view's zooming and scrolling redraws
     * the timeline alone. Both playheads follow the playhead every frame from
     * positions built once, hidden while it is out of view, and what is being
     * dragged shows where it would land — a span on the lane it would really
     * take, or a line at the moment an edge, a cut, a node or a transition's
     * far side would move to, or at the clip edge a transition would be laid
     * on — from positions built the first time each is needed, so neither
     * playback nor a drag allocates; the Cut tool shows the same line where
     * it would cut under the pointer. A timeline still growing under a take
     * and a resized timeline refresh a few times a second.
     */

    // Internal
    private MenuManager menuManager;
    private RecorderManager recorderManager;
    private RecorderRowSystem recorderRowSystem;

    // Menus
    private MenuInstance recorderMenu;
    private ElementInstance statusLabel;
    private ElementInstance titleLabel;
    private ElementInstance playhead;
    private ElementInstance lanePlayhead;
    private ElementInstance rangeStartMarker;
    private ElementInstance rangeEndMarker;
    private ElementInstance dragGhost;
    private ElementInstance viewLabel;

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
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.recorderManager = get(RecorderManager.class);
        this.recorderRowSystem = get(RecorderRowSystem.class);
    }

    @Override
    protected void awake() {

        this.recorderMenu = recorderRowSystem.getMenu();
        this.statusLabel = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_STATUS);
        this.titleLabel = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_TIMELINE_TITLE);
        this.playhead = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_PLAYHEAD);
        this.lanePlayhead = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_LANE_PLAYHEAD);
        this.rangeStartMarker = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_RANGE_START);
        this.rangeEndMarker = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_RANGE_END);
        this.dragGhost = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_DRAG_GHOST);
        this.viewLabel = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_VIEW);

        recorderRowSystem.requireEntryPoint(EditorSetting.RECORDER_ENTRY_RULER);
        recorderRowSystem.requireEntryPoint(EditorSetting.RECORDER_ENTRY_CLIP_LANES);
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

            if (isTimelineStale())
                refreshTimeline();
        }

        movePlayhead();
        moveGhost();
    }

    private void refreshAll() {
        refreshStatus();
        refreshTitle();
        refreshTimeline();
    }

    // Status \\

    private void refreshStatus() {
        statusLabel.setFontText(recorderManager.getStatusText());
    }

    // The recording open, and every recording it was opened inside
    private void refreshTitle() {
        titleLabel.setFontText(recorderManager.hasRecording()
                ? String.format(RecorderSetting.FORMAT_TIMELINE_TITLE, recorderRowSystem.describeOpen())
                : RecorderSetting.TEXT_TIMELINE_TITLE);
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
                : String.format(
                        RecorderSetting.FORMAT_VIEW,
                        recorderRowSystem.formatTime(view.getStart()),
                        recorderRowSystem.formatTime(view.getEnd())));
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

        for (int i = 0; i < clips.size(); i++)
            injectTransitionMarks(clips.get(i), view);

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

            tick.findChildById(RecorderSetting.ELEMENT_RULER_LABEL).setFontText(recorderRowSystem.formatTime(time));
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

    // One bar per clip across the part of it in view, on its own lane, named after its source; a clip only heard is
    // drawn in its own shade
    private void injectClipBar(RecordingClipStruct clip, int clipIndex, RecordingViewStruct view) {

        if (!clip.overlaps(view.getStart(), view.getEnd()))
            return;

        int fromStep = toViewStep(clip.getStart());
        DimensionVector2Struct position = resolveLanePosition(clip.getLane(), fromStep);
        DimensionVector2Struct size = resolveBarSize(toViewStep(clip.getEnd()) - fromStep);
        String template = recorderManager.isClipSelected(clipIndex)
                ? RecorderSetting.TEMPLATE_CLIP_BAR_SELECTED
                : clip.isMuted() ? RecorderSetting.TEMPLATE_CLIP_BAR_MUTED
                        : clip.resolveSourceChannelMask() == RecorderChannel.SOUND.getBit()
                                ? RecorderSetting.TEMPLATE_CLIP_BAR_SOUND
                                : RecorderSetting.TEMPLATE_CLIP_BAR;

        ElementInstance bar = menuManager.inject(recorderMenu, EditorSetting.RECORDER_ENTRY_CLIP_LANES, template,
                injected -> {
                    injected.setPositionOverride(position);
                    injected.setSizeOverride(size);
                });

        bar.findChildById(RecorderSetting.ELEMENT_CLIP_LABEL).setFontText(String.format(
                RecorderSetting.FORMAT_CLIP_LABELS[clip.getSourceKind().ordinal()], clip.getSourceName()));
    }

    // A mark across each transition laid on the clip, over the part of it in view, named after the transition; too
    // narrow to see, it is left out until the view is zoomed in on it
    private void injectTransitionMarks(RecordingClipStruct clip, RecordingViewStruct view) {

        for (int e = 0; e < RecorderClipEdge.VALUES.length; e++) {

            RecorderClipEdge edge = RecorderClipEdge.VALUES[e];

            if (!clip.hasTransition(edge))
                continue;

            float from = clip.resolveTransitionStart(edge);
            float to = from + clip.resolveTransitionSeconds(edge);

            if (to <= view.getStart() || from >= view.getEnd())
                continue;

            int fromStep = toViewStep(from);
            int steps = toViewStep(to) - fromStep;

            if (steps <= 0)
                continue;

            DimensionVector2Struct position = resolveLanePosition(clip.getLane(), fromStep);
            DimensionVector2Struct size = resolveBarSize(steps);
            String template = clip == recorderManager.getSelectedTransitionClip()
                    && edge == recorderManager.getSelectedTransitionEdge()
                            ? RecorderSetting.TEMPLATE_TRANSITION_MARK_SELECTED
                            : RecorderSetting.TEMPLATE_TRANSITION_MARK;

            ElementInstance mark = menuManager.inject(recorderMenu, EditorSetting.RECORDER_ENTRY_CLIP_LANES, template,
                    injected -> {
                        injected.setPositionOverride(position);
                        injected.setSizeOverride(size);
                    });

            mark.findChildById(RecorderSetting.ELEMENT_TRANSITION_LABEL).setFontText(
                    recorderManager.resolveTransitionHandle(clip, edge).getDisplayName());
        }
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

        if (clip.getTake() == null || node.getTrack() != clip.getTake().getPlayerTrack()
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
    // lanes on the lane it would really take, or a line at the moment an edge, a cut, a node or a transition's far
    // side would move to, or at the clip edge a transition would be laid on; with nothing dragged, the cut tool's
    // line where it would cut under the pointer
    private void moveGhost() {

        RecordingDragStruct drag = recorderManager.getDrag();
        RecorderDragKind kind = drag.getKind();
        boolean dragShown = drag.isActive() && (drag.isMoved() || kind == RecorderDragKind.CUT)
                && ((kind.isSpan() || kind.isAimed()) && drag.isOverLanes() || kind.isMoment());
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
}
