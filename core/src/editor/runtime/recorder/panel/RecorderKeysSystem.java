package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderDragKind;
import editor.bootstrap.recorderpipeline.recording.RecorderKeyMode;
import editor.bootstrap.recorderpipeline.recording.RecorderNodeKind;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.bootstrap.recorderpipeline.recording.RecordingBodyTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingDragStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingNodeStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingViewStruct;
import editor.runtime.EditorSetting;
import editor.runtime.recorder.RecorderSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderKeysSystem extends SystemPackage {

    /*
     * Fills the Keys pane below the timeline while it shows. Its outliner
     * lists the selected clip's take track by track, each opened to a row for
     * every value it holds and, for a character, a row of its movement and one
     * of its gestures; every row's strip shows its keys or nodes across the
     * stretch of the timeline in view, a closed track's strip every key of
     * every curve it holds. Beneath the rows the graph draws the selected
     * curve across the same stretch, between the least and greatest values
     * framed, with its keys — every selected one marked — the handles of the
     * key shown and the box a selection is being drawn with, from marks made
     * once and placed again, so a mark that has not moved is left alone.
     * Its inspector shows the selected curve, key or node and what can be
     * done to it. The whole pane is drawn again when the recorder's revision,
     * its keys or its view move; keys or a handle being dragged move only the
     * selected marks on their strip, the graph's marks that changed and the
     * inspector's key, and the playhead follows every frame from positions
     * built once.
     */

    // Internal
    private MenuManager menuManager;
    private RecorderManager recorderManager;
    private RecorderPanelSystem recorderPanelSystem;

    // Menus
    private MenuInstance recorderMenu;
    private ElementInstance keysTitle;
    private ElementInstance keyGraph;
    private ElementInstance keysPlayhead;
    private ElementInstance graphTop;
    private ElementInstance graphBottom;
    private ElementInstance nowLabel;
    private ElementInstance keyTimeLabel;
    private ElementInstance keyValueLabel;

    // Strips
    private DimensionVector2Struct[] stripPositions;
    private boolean[] stepTaken;
    private DimensionVector2Struct hiddenPosition;
    private int shownPlayheadStep;
    private boolean playheadShown;
    private DimensionVector2Struct playheadSize;
    private DimensionVector2Struct hiddenSize;
    private ObjectArrayList<ElementInstance> selectedDiamonds;
    private IntArrayList selectedDiamondKeys;

    // Graph
    private RecorderMarkPoolStruct segmentPool;
    private RecorderMarkPoolStruct handleLinePool;
    private RecorderMarkPoolStruct keyPool;
    private RecorderMarkPoolStruct selectedKeyPool;
    private RecorderMarkPoolStruct handlePool;
    private RecorderMarkPoolStruct boxPool;
    private RecorderMarkPoolStruct[] pools;

    // State
    private boolean shown;
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
        this.selectedDiamonds = new ObjectArrayList<>();
        this.selectedDiamondKeys = new IntArrayList();

        // State
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
        this.keysTitle = requireEntryPoint(RecorderSetting.ENTRY_KEYS_TITLE);
        this.keyGraph = requireEntryPoint(EditorSetting.RECORDER_ENTRY_KEY_GRAPH);
        this.keysPlayhead = requireEntryPoint(RecorderSetting.ENTRY_KEYS_PLAYHEAD);
        this.graphTop = requireEntryPoint(RecorderSetting.ENTRY_GRAPH_TOP);
        this.graphBottom = requireEntryPoint(RecorderSetting.ENTRY_GRAPH_BOTTOM);

        requireEntryPoint(EditorSetting.RECORDER_ENTRY_KEY_ROWS);
        requireEntryPoint(RecorderSetting.ENTRY_KEY_INSPECTOR);

        // Graph — lines lie under points
        ElementInstance graphMarks = requireEntryPoint(RecorderSetting.ENTRY_GRAPH_MARKS);
        ElementInstance graphLines = requireChild(graphMarks, RecorderSetting.ELEMENT_GRAPH_LINES);
        ElementInstance graphPoints = requireChild(graphMarks, RecorderSetting.ELEMENT_GRAPH_POINTS);

        this.segmentPool = new RecorderMarkPoolStruct(RecorderSetting.TEMPLATE_GRAPH_SEGMENT, graphLines);
        this.handleLinePool = new RecorderMarkPoolStruct(RecorderSetting.TEMPLATE_GRAPH_HANDLE_LINE, graphLines);
        this.keyPool = new RecorderMarkPoolStruct(RecorderSetting.TEMPLATE_GRAPH_KEY, graphPoints);
        this.selectedKeyPool = new RecorderMarkPoolStruct(RecorderSetting.TEMPLATE_GRAPH_KEY_SELECTED, graphPoints);
        this.handlePool = new RecorderMarkPoolStruct(RecorderSetting.TEMPLATE_GRAPH_HANDLE, graphPoints);
        this.boxPool = new RecorderMarkPoolStruct(RecorderSetting.TEMPLATE_GRAPH_BOX, graphPoints);
        this.pools = new RecorderMarkPoolStruct[] {
                segmentPool, handleLinePool, keyPool, selectedKeyPool, handlePool, boxPool };
    }

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = recorderMenu.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Recorder menu '" + EditorSetting.RECORDER_MENU + "' has no entry point "
                    + entryPoint + ".");

        return element;
    }

    private ElementInstance requireChild(ElementInstance parent, String childId) {

        ElementInstance element = parent.findChildById(childId);

        if (element == null)
            throwException("Recorder menu '" + EditorSetting.RECORDER_MENU + "' has no element '" + childId + "'.");

        return element;
    }

    // Update \\

    @Override
    protected void update() {

        boolean keys = recorderManager.getPane() == RecorderPane.KEYS;

        if (keys != shown) {
            this.shown = keys;
            this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        }

        if (!keys)
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
        refreshGraphLabels(clip);
        refreshGraph(clip);
        refreshInspector(clip);
    }

    // Keys or a handle being dragged, or a box drawn: only the selected marks on their strip, the graph's marks and
    // the inspector's key move
    private void refreshCurve() {

        RecordingClipStruct clip = recorderManager.getKeyClip();

        this.shownCurveRevision = recorderManager.getCurveRevision();

        moveSelectedDiamonds(clip);
        refreshGraph(clip);
        refreshKey(clip);
    }

    // Rows \\

    private void refreshRows(RecordingClipStruct clip) {

        menuManager.ejectAll(recorderMenu, EditorSetting.RECORDER_ENTRY_KEY_ROWS);
        selectedDiamonds.clear();
        selectedDiamondKeys.clear();

        if (clip == null) {
            recorderPanelSystem.injectText(EditorSetting.RECORDER_ENTRY_KEY_ROWS, RecorderSetting.TEXT_KEYS_NO_CLIP);
            return;
        }

        ObjectArrayList<RecordingCurveTrackStruct> tracks = recorderManager.getKeyTracks();

        if (tracks.isEmpty())
            recorderPanelSystem.injectText(EditorSetting.RECORDER_ENTRY_KEY_ROWS, RecorderSetting.TEXT_KEYS_NO_TRACKS);

        for (int i = 0; i < tracks.size(); i++)
            injectTrack(tracks.get(i), i, clip);
    }

    // A track's own row with every key it holds, and, opened, its node rows and a row for every value it holds
    private void injectTrack(RecordingCurveTrackStruct track, int trackIndex, RecordingClipStruct clip) {

        boolean opened = recorderManager.isTrackOpened(track);
        ElementInstance trackStrip = injectRow(
                RecorderSetting.TEMPLATE_KEY_TRACK_ROW,
                (opened ? RecorderSetting.TEXT_TRACK_OPENED : RecorderSetting.TEXT_TRACK_CLOSED)
                        + track.getTrackLabel(),
                toRowKey(trackIndex, EditorSetting.RECORDER_KEY_ROW_TRACK));

        clearSteps();

        for (int slot = 0; slot < track.getCurveCount(); slot++)
            if (track.hasCurve(slot))
                injectSummary(trackStrip, clip, track.getCurve(slot));

        if (!opened)
            return;

        if (track instanceof RecordingBodyTrackStruct body) {
            injectNodes(injectRow(RecorderSetting.TEMPLATE_KEY_NODE_ROW, RecorderSetting.TEXT_ROW_MOVEMENT,
                    toRowKey(trackIndex, EditorSetting.RECORDER_KEY_ROW_MOVEMENT)), body, RecorderNodeKind.MOVEMENT);
            injectNodes(injectRow(RecorderSetting.TEMPLATE_KEY_NODE_ROW, RecorderSetting.TEXT_ROW_GESTURES,
                    toRowKey(trackIndex, EditorSetting.RECORDER_KEY_ROW_GESTURES)), body, RecorderNodeKind.GESTURE);
        }

        for (int slot = 0; slot < track.getCurveCount(); slot++) {

            boolean selected = track == recorderManager.getSelectedTrack() && slot == recorderManager.getSelectedSlot();
            ElementInstance strip = injectRow(
                    selected ? RecorderSetting.TEMPLATE_KEY_CURVE_ROW_SELECTED : RecorderSetting.TEMPLATE_KEY_CURVE_ROW,
                    track.getCurveLabel(slot),
                    toRowKey(trackIndex, slot));

            if (track.hasCurve(slot))
                injectKeys(strip, clip, track.getCurve(slot), selected);
        }
    }

    // One row of the outliner, its name and its strip both handing back the row's key; its strip comes back
    private ElementInstance injectRow(String template, String label, String rowKey) {

        ElementInstance row = menuManager.inject(recorderMenu, EditorSetting.RECORDER_ENTRY_KEY_ROWS, template);
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

    // Every key of the curve within its clip and the view, one mark however many share a step, every selected one
    // always marked and kept so a drag can move them alone
    private void injectKeys(
            ElementInstance strip,
            RecordingClipStruct clip,
            RecordingCurveStruct curve,
            boolean marked) {

        IntArrayList selectedKeys = recorderManager.getSelectedKeys();
        int selectedSlot = 0;

        clearSteps();

        for (int i = 0; i < curve.size(); i++) {

            boolean selected = marked && selectedSlot < selectedKeys.size() && selectedKeys.getInt(selectedSlot) == i;

            if (selected)
                selectedSlot++;

            int step = resolveKeyStep(clip, curve.getTime(i));

            if (step == EngineSetting.INDEX_NOT_FOUND || !selected && stepTaken[step])
                continue;

            stepTaken[step] = true;

            ElementInstance diamond = injectDiamond(strip, selected
                    ? RecorderSetting.TEMPLATE_KEY_DIAMOND_SELECTED
                    : RecorderSetting.TEMPLATE_KEY_DIAMOND, step);

            if (!selected)
                continue;

            selectedDiamonds.add(diamond);
            selectedDiamondKeys.add(i);
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

    private ElementInstance injectDiamond(ElementInstance strip, String template, int step) {

        DimensionVector2Struct position = stripPositions[step];

        return menuManager.inject(strip, template, diamond -> {
            diamond.setPositionOverride(position);
            diamond.setRotation(RecorderSetting.KEY_DIAMOND_DEGREES);
        });
    }

    // Each selected key's mark follows its key along its strip, set aside while it lies outside its clip or the view
    private void moveSelectedDiamonds(RecordingClipStruct clip) {

        RecordingCurveStruct curve = recorderManager.getSelectedCurve();

        if (clip == null || curve == null)
            return;

        for (int i = 0; i < selectedDiamonds.size(); i++) {

            int key = selectedDiamondKeys.getInt(i);

            if (key >= curve.size())
                continue;

            int step = resolveKeyStep(clip, curve.getTime(key));

            selectedDiamonds.get(i).setPositionOverride(step == EngineSetting.INDEX_NOT_FOUND
                    ? hiddenPosition
                    : stripPositions[step]);
        }
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

    // The greatest and least values framed, or how to begin when no curve is selected
    private void refreshGraphLabels(RecordingClipStruct clip) {

        RecordingCurveStruct curve = recorderManager.getSelectedCurve();

        if (clip == null || curve == null) {
            graphTop.setFontText(RecorderSetting.TEXT_GRAPH_EMPTY);
            graphBottom.setFontText(RecorderSetting.TEXT_EMPTY);
            return;
        }

        graphTop.setFontText(recorderManager.formatValue(
                curve.getProperty().getKind(), recorderManager.getGraphMaximum()));
        graphBottom.setFontText(recorderManager.formatValue(
                curve.getProperty().getKind(), recorderManager.getGraphMinimum()));
    }

    // The selected curve across the view as a line through its keys, a key too close to the last drawn left out
    // unless selected, and the handles of the selected key drawn out
    private void refreshGraph(RecordingClipStruct clip) {

        for (int i = 0; i < pools.length; i++)
            pools[i].begin();

        RecordingCurveStruct curve = recorderManager.getSelectedCurve();
        float width = keyGraph.getComputedW();
        float height = keyGraph.getComputedH();

        if (clip != null && curve != null && width > 0f && height > 0f) {
            placeCurve(clip, curve, width, height);
            placeBox(width, height);
        }

        for (int i = 0; i < pools.length; i++)
            setAsideUnused(pools[i]);
    }

    private void placeCurve(RecordingClipStruct clip, RecordingCurveStruct curve, float width, float height) {

        placeCurveLine(clip, curve, width, height);

        IntArrayList selectedKeys = recorderManager.getSelectedKeys();
        int shownKey = recorderManager.getSelectedKey();
        int selectedSlot = 0;
        float lastX = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < curve.size(); i++) {

            boolean selected = selectedSlot < selectedKeys.size() && selectedKeys.getInt(selectedSlot) == i;

            if (selected)
                selectedSlot++;

            float time = clip.toTimelineTime(curve.getTime(i));

            if (time < clip.getStart() || time > clip.getEnd() || !recorderManager.getView().contains(time))
                continue;

            float x = toGraphX(time, width);
            float y = toGraphY(curve.getValue(i), height);

            if (!selected && x - lastX < RecorderSetting.GRAPH_KEY_SPACING_PIXELS)
                continue;

            lastX = x;

            if (i == shownKey && curve.getMode(i) != RecorderKeyMode.CONSTANT) {

                if (i > 0)
                    placeHandle(x, y, toGraphX(clip.toTimelineTime(curve.getTime(i) + curve.getInTime(i)), width),
                            toGraphY(curve.getValue(i) + curve.getInValue(i), height));

                if (i < curve.size() - 1)
                    placeHandle(x, y, toGraphX(clip.toTimelineTime(curve.getTime(i) + curve.getOutTime(i)), width),
                            toGraphY(curve.getValue(i) + curve.getOutValue(i), height));
            }

            placePoint(selected ? selectedKeyPool : keyPool, x, y);
        }
    }

    // The box a selection is being drawn with, across the moments it spans and, drawn on the graph, its values;
    // drawn on a row it reaches from the bottom of the graph to the top
    private void placeBox(float width, float height) {

        RecordingDragStruct drag = recorderManager.getDrag();

        if (!drag.getKind().isBox() || !drag.isMoved())
            return;

        boolean valued = drag.getKind() == RecorderDragKind.GRAPH_BOX;
        float left = Math.clamp(toGraphX(drag.getPressTime(), width), 0f, width);
        float right = Math.clamp(toGraphX(drag.getReachTime(), width), 0f, width);
        float bottom = valued ? Math.clamp(toGraphY(drag.getPressValue(), height), 0f, height) : 0f;
        float top = valued ? Math.clamp(toGraphY(drag.getReachValue(), height), 0f, height) : height;

        placeLine(boxPool, left, bottom, right, bottom, RecorderSetting.GRAPH_BOX_LINE_PIXELS);
        placeLine(boxPool, right, bottom, right, top, RecorderSetting.GRAPH_BOX_LINE_PIXELS);
        placeLine(boxPool, right, top, left, top, RecorderSetting.GRAPH_BOX_LINE_PIXELS);
        placeLine(boxPool, left, top, left, bottom, RecorderSetting.GRAPH_BOX_LINE_PIXELS);
    }

    // The curve sampled every few pixels across the part of its clip in view, each stretch a turned line
    private void placeCurveLine(RecordingClipStruct clip, RecordingCurveStruct curve, float width, float height) {

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

            placeLine(segmentPool, lastX, lastY, x, y, RecorderSetting.GRAPH_LINE_PIXELS);
            lastX = x;
            lastY = y;
        }
    }

    private void placeHandle(float keyX, float keyY, float handleX, float handleY) {
        placeLine(handleLinePool, keyX, keyY, handleX, handleY, RecorderSetting.GRAPH_HANDLE_LINE_PIXELS);
        placePoint(handlePool, handleX, handleY);
    }

    // A line from one point to another, laid from its start and turned toward its end
    private void placeLine(
            RecorderMarkPoolStruct pool,
            float fromX,
            float fromY,
            float toX,
            float toY,
            float thickness) {

        float length = (float) Math.hypot(toX - fromX, toY - fromY);

        if (length <= 0f)
            return;

        float degrees = (float) Math.toDegrees(Math.atan2(toY - fromY, toX - fromX));
        int index = takeMark(pool);

        if (pool.isPlaced(index, fromX, fromY, length, degrees))
            return;

        ElementInstance line = pool.get(index);

        line.setPositionOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(fromX),
                DimensionValueStruct.ofAbsolute(fromY)));
        line.setSizeOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(length),
                DimensionValueStruct.ofAbsolute(thickness)));
        line.setRotation(degrees);
    }

    private void placePoint(RecorderMarkPoolStruct pool, float x, float y) {

        int index = takeMark(pool);

        if (pool.isPlaced(index, x, y, 0f, RecorderSetting.KEY_DIAMOND_DEGREES))
            return;

        ElementInstance point = pool.get(index);

        point.setPositionOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(x),
                DimensionValueStruct.ofAbsolute(y)));
        point.setRotation(RecorderSetting.KEY_DIAMOND_DEGREES);
    }

    // The pool's next mark, made from its template the first time the pool runs short
    private int takeMark(RecorderMarkPoolStruct pool) {

        if (pool.isExhausted())
            pool.add(menuManager.inject(pool.getContainer(), pool.getTemplate(), null));

        return pool.next();
    }

    // The marks the last drawing used and this one did not, set aside off the graph
    private void setAsideUnused(RecorderMarkPoolStruct pool) {

        for (int i = pool.getUsed(); i < pool.getShown(); i++) {
            pool.get(i).setPositionOverride(hiddenPosition);
            pool.forget(i);
        }

        pool.setShown(pool.getUsed());
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
        this.keyTimeLabel = null;
        this.keyValueLabel = null;

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
            recorderPanelSystem.injectText(entryPoint, RecorderSetting.TEXT_KEYS_HINT_VIEW);
            recorderPanelSystem.injectText(entryPoint, RecorderSetting.TEXT_KEYS_HINT_VALUES);
            return;
        }

        recorderPanelSystem.injectHeader(entryPoint, String.format(RecorderSetting.FORMAT_CURVE_HEADER,
                track.getTrackLabel(), track.getCurveLabel(recorderManager.getSelectedSlot())));

        if (curve == null) {
            recorderPanelSystem.injectText(entryPoint, RecorderSetting.TEXT_CURVE_UNLAID);
            return;
        }

        this.nowLabel = recorderPanelSystem.injectText(entryPoint, RecorderSetting.TEXT_EMPTY);
        refreshNow();
        recorderPanelSystem.injectText(entryPoint, String.format(RecorderSetting.FORMAT_CURVE_KEYS, curve.size()));

        int selectedCount = recorderManager.getSelectedKeys().size();

        if (selectedCount > 1)
            recorderPanelSystem.injectText(entryPoint,
                    String.format(RecorderSetting.FORMAT_KEYS_SELECTED, selectedCount));
        recorderPanelSystem.injectAction(entryPoint, RecorderSetting.TEXT_KEY_ADD, EditorSetting.RECORDER_KEY_KEY_ADD);

        int key = recorderManager.getSelectedKey();

        if (key >= 0 && key < curve.size()) {
            this.keyTimeLabel = recorderPanelSystem.injectHeader(entryPoint, RecorderSetting.TEXT_EMPTY);
            this.keyValueLabel = recorderPanelSystem.injectSetting(entryPoint, RecorderSetting.TEXT_KEY_VALUE,
                    RecorderSetting.TEXT_EMPTY, EditorSetting.RECORDER_KEY_KEY_VALUE);
            refreshKey(clip);
            recorderPanelSystem.injectSetting(entryPoint, RecorderSetting.TEXT_KEY_MODE,
                    curve.getMode(key).getLabel(), EditorSetting.RECORDER_KEY_KEY_MODE);
            recorderPanelSystem.injectAction(entryPoint, RecorderSetting.TEXT_KEY_DELETE,
                    EditorSetting.RECORDER_KEY_KEY_DELETE);
        } else
            recorderPanelSystem.injectText(entryPoint, RecorderSetting.TEXT_KEYS_HINT_KEY);

        recorderPanelSystem.injectHeader(entryPoint, RecorderSetting.TEXT_HEADER_CURVE);
        recorderPanelSystem.injectAction(entryPoint, RecorderSetting.TEXT_CURVE_REFIT,
                EditorSetting.RECORDER_KEY_CURVE_REFIT);
        recorderPanelSystem.injectAction(entryPoint, RecorderSetting.TEXT_TAKE_REFIT,
                EditorSetting.RECORDER_KEY_TAKE_REFIT);
        recorderPanelSystem.injectText(entryPoint, RecorderSetting.TEXT_CURVE_SHARED);
    }

    // The selected key's moment and value, kept up as it is dragged
    private void refreshKey(RecordingClipStruct clip) {

        RecordingCurveStruct curve = recorderManager.getSelectedCurve();
        int key = recorderManager.getSelectedKey();

        if (keyTimeLabel == null || clip == null || curve == null || key < 0 || key >= curve.size())
            return;

        keyTimeLabel.setFontText(String.format(RecorderSetting.FORMAT_KEY_HEADER,
                recorderPanelSystem.formatTime(clip.toTimelineTime(curve.getTime(key)))));
        keyValueLabel.setFontText(recorderManager.formatValue(curve.getProperty().getKind(), curve.getValue(key)));
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
        boolean inView = recorderManager.getView().contains(time);

        if (inView != playheadShown) {
            this.playheadShown = inView;
            this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;
            keysPlayhead.setSizeOverride(inView ? playheadSize : hiddenSize);
        }

        if (!inView)
            return;

        int step = toViewStep(time);

        if (step == shownPlayheadStep)
            return;

        this.shownPlayheadStep = step;
        keysPlayhead.setPositionOverride(stripPositions[step]);
    }

    // Utility \\

    private int toViewStep(float time) {

        int step = Math.round(recorderManager.getView().toFraction(time) * RecorderSetting.TIMELINE_STEPS);

        return Math.clamp(step, 0, RecorderSetting.TIMELINE_STEPS);
    }

    private float toPercent(int step) {
        return RecorderSetting.TIMELINE_PERCENT * step / RecorderSetting.TIMELINE_STEPS;
    }
}
