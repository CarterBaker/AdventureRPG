package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderKeyMode;
import editor.bootstrap.recorderpipeline.recording.RecorderNodeKind;
import editor.bootstrap.recorderpipeline.recording.RecordingBodyTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingNodeStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingViewStruct;
import editor.runtime.EditorSetting;
import editor.runtime.recorder.RecorderSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderKeysSystem extends SystemPackage {

    /*
     * Shows the Keys tab in place of the bin, the selected clip and the
     * markers whenever the recorder asks for it. Its outliner lists the
     * selected clip's take track by track, each opened to a row for every
     * value it holds and, for a character, a row of its movement and one of
     * its gestures; every row's strip shows its keys or nodes across the
     * stretch of the timeline in view, a closed track's strip every key of
     * every curve it holds. As a graph it draws the selected curve across
     * the view between the least and greatest values framed, its keys, and
     * the handles of the selected key. Its inspector shows the selected
     * curve, key or node and what can be done to it. The whole tab is drawn
     * again when the recorder's revision, its keys or its view move; a key
     * or handle being dragged redraws only its own strip, the graph and the
     * inspector, and the playhead follows every frame from positions built
     * once.
     */

    // Internal
    private MenuManager menuManager;
    private RecorderManager recorderManager;
    private RecorderPanelSystem recorderPanelSystem;

    // Menus
    private MenuInstance recorderMenu;
    private ElementInstance keysPanel;
    private ElementInstance keysTitle;
    private ElementInstance keyRows;
    private ElementInstance keyGraph;
    private ElementInstance graphMarks;
    private ElementInstance keysPlayhead;
    private ElementInstance graphTop;
    private ElementInstance graphBottom;
    private ElementInstance takesPanel;
    private ElementInstance clipPanel;
    private ElementInstance markersPanel;
    private ElementInstance selectedStrip;
    private ElementInstance nowLabel;

    // Strips
    private DimensionVector2Struct[] stripPositions;
    private boolean[] stepTaken;
    private DimensionVector2Struct hiddenPosition;
    private int shownPlayheadStep;
    private boolean playheadShown;
    private DimensionVector2Struct playheadSize;
    private DimensionVector2Struct hiddenSize;

    // State
    private boolean shownKeys;
    private boolean shownGraph;
    private int shownRevision;
    private int shownKeysRevision;
    private int shownViewRevision;
    private int shownCurveRevision;
    private float shownViewStart;
    private float shownViewSpan;
    private float shownGraphWidth;
    private float shownGraphHeight;
    private float refreshElapsed;

    // Base \\

    @Override
    protected void create() {

        // Strips
        this.stripPositions = new DimensionVector2Struct[RecorderSetting.TIMELINE_STEPS + 1];

        for (int i = 0; i < stripPositions.length; i++)
            stripPositions[i] = new DimensionVector2Struct(
                    DimensionValueStruct.ofPercent(toPercent(i)),
                    DimensionValueStruct.ofAbsolute(0f));

        this.stepTaken = new boolean[RecorderSetting.TIMELINE_STEPS + 1];
        this.hiddenPosition = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(RecorderSetting.HIDDEN_PANEL_PIXELS),
                DimensionValueStruct.ofAbsolute(0f));
        this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;
        this.playheadShown = true;
        this.playheadSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(RecorderSetting.PLAYHEAD_WIDTH_PIXELS),
                DimensionValueStruct.ofPercent(RecorderSetting.TIMELINE_PERCENT));
        this.hiddenSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(0f),
                DimensionValueStruct.ofAbsolute(0f));

        // State
        this.shownKeys = true;
        this.shownGraph = true;
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.recorderManager = get(RecorderManager.class);
        this.recorderPanelSystem = get(RecorderPanelSystem.class);
    }

    @Override
    protected void awake() {

        this.recorderMenu = recorderPanelSystem.getRecorderMenu();
        this.keysPanel = requireEntryPoint(RecorderSetting.ENTRY_KEYS_PANEL);
        this.keysTitle = requireEntryPoint(RecorderSetting.ENTRY_KEYS_TITLE);
        this.keyRows = requireEntryPoint(RecorderSetting.ENTRY_KEY_ROWS);
        this.keyGraph = requireEntryPoint(EditorSetting.RECORDER_ENTRY_KEY_GRAPH);
        this.graphMarks = requireEntryPoint(RecorderSetting.ENTRY_GRAPH_MARKS);
        this.keysPlayhead = requireEntryPoint(RecorderSetting.ENTRY_KEYS_PLAYHEAD);
        this.graphTop = requireEntryPoint(RecorderSetting.ENTRY_GRAPH_TOP);
        this.graphBottom = requireEntryPoint(RecorderSetting.ENTRY_GRAPH_BOTTOM);
        this.takesPanel = requireEntryPoint(RecorderSetting.ENTRY_TAKES_PANEL);
        this.clipPanel = requireEntryPoint(RecorderSetting.ENTRY_CLIP_PANEL);
        this.markersPanel = requireEntryPoint(RecorderSetting.ENTRY_MARKERS_PANEL);

        requireEntryPoint(RecorderSetting.ENTRY_KEY_INSPECTOR);
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

        showPanels();

        if (!recorderManager.isKeysShown())
            return;

        if (isStale())
            refreshKeys();
        else if (recorderManager.getCurveRevision() != shownCurveRevision)
            refreshCurve();

        this.refreshElapsed += internal.getDeltaTime();

        if (refreshElapsed >= RecorderSetting.REFRESH_SECONDS) {
            this.refreshElapsed = 0f;
            refreshNow();
        }

        movePlayhead();
    }

    // The bin, the clip and the markers give way to the Keys tab and back, and the graph to the strips
    private void showPanels() {

        boolean keys = recorderManager.isKeysShown();
        boolean graph = keys && recorderManager.isGraphShown();

        if (keys != shownKeys) {
            this.shownKeys = keys;
            place(keysPanel, keys);
            place(takesPanel, !keys);
            place(clipPanel, !keys);
            place(markersPanel, !keys);
            this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        }

        if (graph != shownGraph) {
            this.shownGraph = graph;
            place(keyGraph, graph);
            this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        }
    }

    private void place(ElementInstance element, boolean shown) {
        if (shown)
            element.clearPositionOverride();
        else
            element.setPositionOverride(hiddenPosition);
    }

    private boolean isStale() {

        RecordingViewStruct view = recorderManager.getView();

        return recorderManager.getRevision() != shownRevision
                || recorderManager.getKeysRevision() != shownKeysRevision
                || recorderManager.getViewRevision() != shownViewRevision
                || view.getStart() != shownViewStart
                || view.getSpan() != shownViewSpan
                || keyGraph.getComputedW() != shownGraphWidth
                || keyGraph.getComputedH() != shownGraphHeight;
    }

    // Refresh \\

    private void refreshKeys() {

        RecordingViewStruct view = recorderManager.getView();

        this.shownRevision = recorderManager.getRevision();
        this.shownKeysRevision = recorderManager.getKeysRevision();
        this.shownViewRevision = recorderManager.getViewRevision();
        this.shownCurveRevision = recorderManager.getCurveRevision();
        this.shownViewStart = view.getStart();
        this.shownViewSpan = view.getSpan();
        this.shownGraphWidth = keyGraph.getComputedW();
        this.shownGraphHeight = keyGraph.getComputedH();
        this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;

        RecordingClipStruct clip = recorderManager.getKeyClip();

        keysTitle.setFontText(clip != null
                ? String.format(RecorderSetting.FORMAT_KEYS_TITLE, clip.getTake().getName())
                : RecorderSetting.TEXT_KEYS_TITLE);

        refreshRows(clip);
        refreshGraph(clip);
        refreshInspector(clip);
    }

    // A key or handle being dragged: only its own strip, the graph and the inspector change
    private void refreshCurve() {

        RecordingClipStruct clip = recorderManager.getKeyClip();
        RecordingCurveStruct curve = recorderManager.getSelectedCurve();

        this.shownCurveRevision = recorderManager.getCurveRevision();

        if (selectedStrip != null && clip != null && curve != null) {
            ejectChildren(selectedStrip);
            injectKeys(selectedStrip, clip, curve, true);
        }

        refreshGraph(clip);
        refreshInspector(clip);
    }

    // Rows \\

    private void refreshRows(RecordingClipStruct clip) {

        menuManager.ejectAll(recorderMenu, RecorderSetting.ENTRY_KEY_ROWS);
        this.selectedStrip = null;

        if (clip == null) {
            recorderPanelSystem.injectText(RecorderSetting.ENTRY_KEY_ROWS, RecorderSetting.TEXT_KEYS_NO_CLIP);
            return;
        }

        ObjectArrayList<RecordingCurveTrackStruct> tracks = recorderManager.getKeyTracks();

        if (tracks.isEmpty())
            recorderPanelSystem.injectText(RecorderSetting.ENTRY_KEY_ROWS, RecorderSetting.TEXT_KEYS_NO_TRACKS);

        for (int i = 0; i < tracks.size(); i++)
            injectTrack(tracks.get(i), i, clip);
    }

    // A track's own row with every key it holds, and, opened, its node rows and a row for every value it holds; the
    // graph covers the strips, so while it shows they are left empty
    private void injectTrack(RecordingCurveTrackStruct track, int trackIndex, RecordingClipStruct clip) {

        boolean opened = recorderManager.isTrackOpened(track);
        boolean strips = !recorderManager.isGraphShown();
        ElementInstance trackStrip = injectRow(
                RecorderSetting.TEMPLATE_KEY_TRACK_ROW,
                (opened ? RecorderSetting.TEXT_TRACK_OPENED : RecorderSetting.TEXT_TRACK_CLOSED)
                        + track.getTrackLabel(),
                toRowKey(trackIndex, EditorSetting.RECORDER_KEY_ROW_TRACK));

        clearSteps();

        for (int slot = 0; strips && slot < track.getCurveCount(); slot++)
            if (track.hasCurve(slot))
                injectSummary(trackStrip, clip, track.getCurve(slot));

        if (!opened)
            return;

        if (track instanceof RecordingBodyTrackStruct body) {

            ElementInstance movementStrip = injectRow(RecorderSetting.TEMPLATE_KEY_NODE_ROW,
                    RecorderSetting.TEXT_ROW_MOVEMENT, toRowKey(trackIndex, EditorSetting.RECORDER_KEY_ROW_MOVEMENT));
            ElementInstance gestureStrip = injectRow(RecorderSetting.TEMPLATE_KEY_NODE_ROW,
                    RecorderSetting.TEXT_ROW_GESTURES, toRowKey(trackIndex, EditorSetting.RECORDER_KEY_ROW_GESTURES));

            if (strips) {
                injectNodes(movementStrip, body, RecorderNodeKind.MOVEMENT);
                injectNodes(gestureStrip, body, RecorderNodeKind.GESTURE);
            }
        }

        for (int slot = 0; slot < track.getCurveCount(); slot++) {

            boolean selected = track == recorderManager.getSelectedTrack() && slot == recorderManager.getSelectedSlot();
            ElementInstance strip = injectRow(
                    selected ? RecorderSetting.TEMPLATE_KEY_CURVE_ROW_SELECTED : RecorderSetting.TEMPLATE_KEY_CURVE_ROW,
                    track.getCurveLabel(slot),
                    toRowKey(trackIndex, slot));

            if (strips && track.hasCurve(slot))
                injectKeys(strip, clip, track.getCurve(slot), selected);

            if (strips && selected)
                this.selectedStrip = strip;
        }
    }

    // One row of the outliner, its name and its strip both handing back the row's key; its strip comes back
    private ElementInstance injectRow(String template, String label, String rowKey) {

        ElementInstance row = menuManager.inject(recorderMenu, RecorderSetting.ENTRY_KEY_ROWS, template);
        ElementInstance name = row.findChildById(RecorderSetting.ELEMENT_KEY_NAME);
        ElementInstance strip = row.findChildById(RecorderSetting.ELEMENT_KEY_STRIP);

        name.setActionArgOverride(rowKey);
        name.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(label);
        strip.setOnDragArgOverride(rowKey);
        return strip;
    }

    private String toRowKey(int trackIndex, int row) {
        return trackIndex + EditorSetting.RECORDER_KEY_ROW_SEPARATOR + row;
    }

    // Every key of the curve within its clip and the view, one mark however many share a step, the selected one
    // always marked
    private void injectKeys(
            ElementInstance strip,
            RecordingClipStruct clip,
            RecordingCurveStruct curve,
            boolean marked) {

        int selectedKey = marked ? recorderManager.getSelectedKey() : EngineSetting.INDEX_NOT_FOUND;

        clearSteps();

        for (int i = 0; i < curve.size(); i++) {

            int step = resolveKeyStep(clip, curve.getTime(i));

            if (step == EngineSetting.INDEX_NOT_FOUND || i != selectedKey && stepTaken[step])
                continue;

            stepTaken[step] = true;
            injectDiamond(strip, i == selectedKey
                    ? RecorderSetting.TEMPLATE_KEY_DIAMOND_SELECTED
                    : RecorderSetting.TEMPLATE_KEY_DIAMOND, step);
        }
    }

    // Every key of the curve on a closed track's own row, one mark however many keys share a step
    private void injectSummary(ElementInstance strip, RecordingClipStruct clip, RecordingCurveStruct curve) {

        for (int i = 0; i < curve.size(); i++) {

            int step = resolveKeyStep(clip, curve.getTime(i));

            if (step == EngineSetting.INDEX_NOT_FOUND || stepTaken[step])
                continue;

            stepTaken[step] = true;
            injectDiamond(strip, RecorderSetting.TEMPLATE_KEY_SUMMARY, step);
        }
    }

    private void injectDiamond(ElementInstance strip, String template, int step) {

        DimensionVector2Struct position = stripPositions[step];

        menuManager.inject(strip, template, diamond -> {
            diamond.setPositionOverride(position);
            diamond.setRotation(RecorderSetting.KEY_DIAMOND_DEGREES);
        });
    }

    // The step across the view a key of the take falls on, -1 outside its clip or the view
    private int resolveKeyStep(RecordingClipStruct clip, float takeTime) {

        float time = clip.toTimelineTime(takeTime);
        RecordingViewStruct view = recorderManager.getView();

        if (time < clip.getStart() || time > clip.getEnd() || !view.contains(time))
            return EngineSetting.INDEX_NOT_FOUND;

        return toViewStep(time);
    }

    private void clearSteps() {
        for (int i = 0; i < stepTaken.length; i++)
            stepTaken[i] = false;
    }

    // The character's movements or gestures on the selected clip across the view, each named
    private void injectNodes(ElementInstance strip, RecordingBodyTrackStruct track, RecorderNodeKind kind) {

        ObjectArrayList<RecordingNodeStruct> nodes = recorderManager.getNodes();
        RecordingViewStruct view = recorderManager.getView();
        int clipIndex = recorderManager.getSelectedClip();

        for (int i = 0; i < nodes.size(); i++) {

            RecordingNodeStruct node = nodes.get(i);

            if (node.getClipIndex() != clipIndex || node.getTrack() != track || node.getKind() != kind
                    || node.getEnd() <= view.getStart() || node.getStart() >= view.getEnd())
                continue;

            int fromStep = toViewStep(node.getStart());
            int steps = toViewStep(node.getEnd()) - fromStep;

            if (steps <= 0)
                continue;

            boolean selected = i == recorderManager.getSelectedNode();
            String template = kind == RecorderNodeKind.MOVEMENT
                    ? selected ? RecorderSetting.TEMPLATE_KEY_MOVEMENT_SELECTED : RecorderSetting.TEMPLATE_KEY_MOVEMENT
                    : selected ? RecorderSetting.TEMPLATE_KEY_GESTURE_SELECTED : RecorderSetting.TEMPLATE_KEY_GESTURE;
            DimensionVector2Struct position = stripPositions[fromStep];
            DimensionVector2Struct size = new DimensionVector2Struct(
                    DimensionValueStruct.ofPercent(toPercent(steps)),
                    DimensionValueStruct.ofAbsolute(RecorderSetting.KEY_NODE_PIXELS));

            menuManager.inject(strip, template, injected -> {
                injected.setPositionOverride(position);
                injected.setSizeOverride(size);
            }).findChildById(RecorderSetting.ELEMENT_NODE_LABEL).setFontText(node.getLabel());
        }
    }

    // Graph \\

    // The selected curve across the view as a line through its keys, a key too close to the last drawn left out
    // unless selected, and the handles of the selected key drawn out
    private void refreshGraph(RecordingClipStruct clip) {

        ejectChildren(graphMarks);

        RecordingCurveStruct curve = recorderManager.getSelectedCurve();
        float width = keyGraph.getComputedW();
        float height = keyGraph.getComputedH();

        if (!recorderManager.isGraphShown() || clip == null || curve == null || width <= 0f || height <= 0f) {
            graphTop.setFontText(RecorderSetting.TEXT_GRAPH_EMPTY);
            graphBottom.setFontText(RecorderSetting.TEXT_EMPTY);
            return;
        }

        graphTop.setFontText(recorderManager.formatValue(
                curve.getProperty().getKind(), recorderManager.getGraphMaximum()));
        graphBottom.setFontText(recorderManager.formatValue(
                curve.getProperty().getKind(), recorderManager.getGraphMinimum()));

        injectCurveLine(clip, curve, width, height);

        int selectedKey = recorderManager.getSelectedKey();
        float lastX = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < curve.size(); i++) {

            float time = clip.toTimelineTime(curve.getTime(i));

            if (time < clip.getStart() || time > clip.getEnd() || !recorderManager.getView().contains(time))
                continue;

            float x = toGraphX(time, width);
            float y = toGraphY(curve.getValue(i), height);

            if (i != selectedKey && x - lastX < RecorderSetting.GRAPH_KEY_SPACING_PIXELS)
                continue;

            lastX = x;

            if (i == selectedKey && curve.getMode(i) != RecorderKeyMode.CONSTANT) {

                if (i > 0)
                    injectHandle(x, y, toGraphX(clip.toTimelineTime(curve.getTime(i) + curve.getInTime(i)), width),
                            toGraphY(curve.getValue(i) + curve.getInValue(i), height));

                if (i < curve.size() - 1)
                    injectHandle(x, y, toGraphX(clip.toTimelineTime(curve.getTime(i) + curve.getOutTime(i)), width),
                            toGraphY(curve.getValue(i) + curve.getOutValue(i), height));
            }

            injectPoint(i == selectedKey
                    ? RecorderSetting.TEMPLATE_GRAPH_KEY_SELECTED
                    : RecorderSetting.TEMPLATE_GRAPH_KEY, x, y);
        }
    }

    // The curve sampled every few pixels across the part of its clip in view, each stretch a turned line
    private void injectCurveLine(RecordingClipStruct clip, RecordingCurveStruct curve, float width, float height) {

        RecordingViewStruct view = recorderManager.getView();
        float from = Math.max(view.getStart(), clip.getStart());
        float to = Math.min(view.getEnd(), clip.getEnd());

        if (to <= from)
            return;

        int segments = Math.max(1, (int) Math.ceil(
                (to - from) / view.getSpan() * width / RecorderSetting.GRAPH_SEGMENT_PIXELS));
        float lastX = toGraphX(from, width);
        float lastY = toGraphY(curve.evaluate(clip.toTakeTime(from)), height);

        for (int i = 1; i <= segments; i++) {

            float time = from + (to - from) * i / segments;
            float x = toGraphX(time, width);
            float y = toGraphY(curve.evaluate(clip.toTakeTime(time)), height);

            injectLine(RecorderSetting.TEMPLATE_GRAPH_SEGMENT, lastX, lastY, x, y, RecorderSetting.GRAPH_LINE_PIXELS);
            lastX = x;
            lastY = y;
        }
    }

    private void injectHandle(float keyX, float keyY, float handleX, float handleY) {
        injectLine(RecorderSetting.TEMPLATE_GRAPH_HANDLE_LINE, keyX, keyY, handleX, handleY,
                RecorderSetting.GRAPH_HANDLE_LINE_PIXELS);
        injectPoint(RecorderSetting.TEMPLATE_GRAPH_HANDLE, handleX, handleY);
    }

    // A line from one point to another, laid from its start and turned toward its end
    private void injectLine(String template, float fromX, float fromY, float toX, float toY, float thickness) {

        float length = (float) Math.hypot(toX - fromX, toY - fromY);

        if (length <= 0f)
            return;

        DimensionVector2Struct position = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(fromX),
                DimensionValueStruct.ofAbsolute(fromY));
        DimensionVector2Struct size = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(length),
                DimensionValueStruct.ofAbsolute(thickness));
        float degrees = (float) Math.toDegrees(Math.atan2(toY - fromY, toX - fromX));

        menuManager.inject(graphMarks, template, line -> {
            line.setPositionOverride(position);
            line.setSizeOverride(size);
            line.setRotation(degrees);
        });
    }

    private void injectPoint(String template, float x, float y) {

        DimensionVector2Struct position = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(x),
                DimensionValueStruct.ofAbsolute(y));

        menuManager.inject(graphMarks, template, point -> {
            point.setPositionOverride(position);
            point.setRotation(RecorderSetting.KEY_DIAMOND_DEGREES);
        });
    }

    private float toGraphX(float time, float width) {
        return recorderManager.getView().toFraction(time) * width;
    }

    // How high a value stands on the graph, the least framed at the bottom and the greatest at the top
    private float toGraphY(double value, float height) {

        double minimum = recorderManager.getGraphMinimum();

        return (float) ((value - minimum) / (recorderManager.getGraphMaximum() - minimum) * height);
    }

    // Inspector \\

    // The selected curve and key with what can be done to them, else the selected node, else how to begin
    private void refreshInspector(RecordingClipStruct clip) {

        menuManager.ejectAll(recorderMenu, RecorderSetting.ENTRY_KEY_INSPECTOR);
        this.nowLabel = null;

        int entryPoint = RecorderSetting.ENTRY_KEY_INSPECTOR;
        RecordingCurveTrackStruct track = recorderManager.getSelectedTrack();
        RecordingCurveStruct curve = recorderManager.getSelectedCurve();

        if (clip == null) {
            recorderPanelSystem.injectText(entryPoint, RecorderSetting.TEXT_KEYS_NO_CLIP);
            return;
        }

        if (track == null || recorderManager.getSelectedSlot() < 0) {
            recorderPanelSystem.refreshNode(entryPoint);
            recorderPanelSystem.injectHeader(entryPoint, RecorderSetting.TEXT_HEADER_KEYS);
            recorderPanelSystem.injectText(entryPoint, RecorderSetting.TEXT_KEYS_HINT_OPEN);
            recorderPanelSystem.injectText(entryPoint, RecorderSetting.TEXT_KEYS_HINT_SELECT);
            return;
        }

        recorderPanelSystem.injectHeader(entryPoint, String.format(RecorderSetting.FORMAT_CURVE_HEADER,
                track.getTrackLabel(), track.getCurveLabel(recorderManager.getSelectedSlot())));

        if (curve == null) {
            recorderPanelSystem.injectText(entryPoint, RecorderSetting.TEXT_CURVE_RECORDED);
            return;
        }

        this.nowLabel = recorderPanelSystem.injectText(entryPoint, RecorderSetting.TEXT_EMPTY);
        refreshNow();
        recorderPanelSystem.injectText(entryPoint, String.format(RecorderSetting.FORMAT_CURVE_KEYS, curve.size()));
        recorderPanelSystem.injectAction(entryPoint, RecorderSetting.TEXT_KEY_ADD, EditorSetting.RECORDER_KEY_KEY_ADD);

        int key = recorderManager.getSelectedKey();

        if (key >= 0 && key < curve.size()) {
            recorderPanelSystem.injectHeader(entryPoint, String.format(RecorderSetting.FORMAT_KEY_HEADER,
                    recorderPanelSystem.formatTime(clip.toTimelineTime(curve.getTime(key)))));
            recorderPanelSystem.injectSetting(entryPoint, RecorderSetting.TEXT_KEY_VALUE,
                    recorderManager.formatValue(curve.getProperty().getKind(), curve.getValue(key)),
                    EditorSetting.RECORDER_KEY_KEY_VALUE);
            recorderPanelSystem.injectSetting(entryPoint, RecorderSetting.TEXT_KEY_MODE,
                    curve.getMode(key).getLabel(), EditorSetting.RECORDER_KEY_KEY_MODE);
            recorderPanelSystem.injectAction(entryPoint, RecorderSetting.TEXT_KEY_DELETE,
                    EditorSetting.RECORDER_KEY_KEY_DELETE);
        } else
            recorderPanelSystem.injectText(entryPoint, RecorderSetting.TEXT_KEYS_HINT_KEY);

        recorderPanelSystem.injectHeader(entryPoint, RecorderSetting.TEXT_HEADER_CURVE);
        recorderPanelSystem.injectAction(entryPoint, RecorderSetting.TEXT_CURVE_REFIT,
                EditorSetting.RECORDER_KEY_CURVE_REFIT);
        recorderPanelSystem.injectAction(entryPoint, RecorderSetting.TEXT_CURVE_REMOVE,
                EditorSetting.RECORDER_KEY_CURVE_REMOVE);
        recorderPanelSystem.injectText(entryPoint, RecorderSetting.TEXT_CURVE_SHARED);
    }

    // The selected curve's value at the playhead, kept up as it plays
    private void refreshNow() {

        RecordingClipStruct clip = recorderManager.getKeyClip();
        RecordingCurveStruct curve = recorderManager.getSelectedCurve();

        if (nowLabel == null || clip == null || curve == null)
            return;

        nowLabel.setFontText(String.format(RecorderSetting.FORMAT_CURVE_NOW, recorderManager.formatValue(
                curve.getProperty().getKind(), curve.evaluate(clip.toTakeTime(recorderManager.getPlayhead())))));
    }

    // Playhead \\

    // The playhead across the rows and the graph, hidden while it lies out of view
    private void movePlayhead() {

        float time = recorderManager.getPlayhead();
        boolean shown = recorderManager.getView().contains(time);

        if (shown != playheadShown) {
            this.playheadShown = shown;
            this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;
            keysPlayhead.setSizeOverride(shown ? playheadSize : hiddenSize);
        }

        if (!shown)
            return;

        int step = toViewStep(time);

        if (step == shownPlayheadStep)
            return;

        this.shownPlayheadStep = step;
        keysPlayhead.setPositionOverride(stripPositions[step]);
    }

    // Utility \\

    private void ejectChildren(ElementInstance container) {
        while (!container.getChildren().isEmpty())
            menuManager.eject(container, container.getChildren().get(0));
    }

    private int toViewStep(float time) {

        int step = Math.round(recorderManager.getView().toFraction(time) * RecorderSetting.TIMELINE_STEPS);

        return Math.clamp(step, 0, RecorderSetting.TIMELINE_STEPS);
    }

    private float toPercent(int step) {
        return RecorderSetting.TIMELINE_PERCENT * step / RecorderSetting.TIMELINE_STEPS;
    }
}
