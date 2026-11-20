package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderDragKind;
import editor.bootstrap.recorderpipeline.recording.RecorderKeyMode;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingDragStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingViewStruct;
import editor.runtime.EditorSetting;
import editor.runtime.recorder.RecorderSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class RecorderKeyGraphSystem extends SystemPackage {

    /*
     * Fills the Keys Graph tab: the curve selected on the Keys tab drawn
     * across the stretch of the timeline in view, between the least and
     * greatest values framed, with its keys — every selected one marked — the
     * handles of the key shown and the box a selection is being drawn with,
     * from marks made once and placed again, so a mark that has not moved is
     * left alone. Its title names the curve, and the line beneath it the
     * value at the playhead, how many keys the curve holds and the key shown
     * with its moment, value and mode. Everything is drawn again when the
     * recorder's revision, its keys or its view move or the graph is resized;
     * keys or a handle being dragged move only the marks that changed and the
     * key's line, and the playhead follows every frame.
     */

    // Internal
    private MenuManager menuManager;
    private RecorderManager recorderManager;
    private RecorderRowSystem recorderRowSystem;

    // Menus
    private ElementInstance graphTitle;
    private ElementInstance graphDetail;
    private ElementInstance keyGraph;
    private ElementInstance graphTop;
    private ElementInstance graphBottom;
    private RecorderPlayheadStruct playhead;

    // Marks
    private DimensionVector2Struct hiddenPosition;
    private RecorderMarkPoolStruct segmentPool;
    private RecorderMarkPoolStruct handleLinePool;
    private RecorderMarkPoolStruct keyPool;
    private RecorderMarkPoolStruct selectedKeyPool;
    private RecorderMarkPoolStruct handlePool;
    private RecorderMarkPoolStruct boxPool;
    private RecorderMarkPoolStruct[] pools;

    // State
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

        // Marks
        this.hiddenPosition = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(RecorderSetting.HIDDEN_PANEL_PIXELS),
                DimensionValueStruct.ofAbsolute(0f));

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

        this.graphTitle = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_GRAPH_TITLE);
        this.graphDetail = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_GRAPH_DETAIL);
        this.keyGraph = recorderRowSystem.requireEntryPoint(EditorSetting.RECORDER_ENTRY_KEY_GRAPH);
        this.graphTop = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_GRAPH_TOP);
        this.graphBottom = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_GRAPH_BOTTOM);
        this.playhead = new RecorderPlayheadStruct(
                recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_GRAPH_PLAYHEAD));

        // Marks — lines lie under points
        ElementInstance graphMarks = recorderRowSystem.requireEntryPoint(RecorderSetting.ENTRY_GRAPH_MARKS);
        ElementInstance graphLines = recorderRowSystem.requireChild(graphMarks, RecorderSetting.ELEMENT_GRAPH_LINES);
        ElementInstance graphPoints = recorderRowSystem.requireChild(graphMarks, RecorderSetting.ELEMENT_GRAPH_POINTS);

        this.segmentPool = new RecorderMarkPoolStruct(RecorderSetting.TEMPLATE_GRAPH_SEGMENT, graphLines);
        this.handleLinePool = new RecorderMarkPoolStruct(RecorderSetting.TEMPLATE_GRAPH_HANDLE_LINE, graphLines);
        this.keyPool = new RecorderMarkPoolStruct(RecorderSetting.TEMPLATE_GRAPH_KEY, graphPoints);
        this.selectedKeyPool = new RecorderMarkPoolStruct(RecorderSetting.TEMPLATE_GRAPH_KEY_SELECTED, graphPoints);
        this.handlePool = new RecorderMarkPoolStruct(RecorderSetting.TEMPLATE_GRAPH_HANDLE, graphPoints);
        this.boxPool = new RecorderMarkPoolStruct(RecorderSetting.TEMPLATE_GRAPH_BOX, graphPoints);
        this.pools = new RecorderMarkPoolStruct[] {
                segmentPool, handleLinePool, keyPool, selectedKeyPool, handlePool, boxPool };
    }

    // Update \\

    @Override
    protected void update() {

        if (isStale())
            refreshGraph();
        else if (recorderManager.getCurveRevision() != shownCurveRevision)
            refreshCurve();

        this.refreshElapsed += internal.getDeltaTime();

        if (refreshElapsed >= RecorderSetting.REFRESH_SECONDS) {
            this.refreshElapsed = 0f;
            refreshDetail();
        }

        recorderRowSystem.placePlayhead(playhead);
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

    private void refreshGraph() {

        RecordingViewStruct view = recorderManager.getView();

        this.shownRevision = recorderManager.getRevision();
        this.shownKeysRevision = recorderManager.getKeysRevision();
        this.shownViewRevision = recorderManager.getViewRevision();
        this.shownCurveRevision = recorderManager.getCurveRevision();
        this.shownViewStart = view.getStart();
        this.shownViewSpan = view.getSpan();
        this.shownGraphWidth = keyGraph.getComputedW();
        this.shownGraphHeight = keyGraph.getComputedH();
        playhead.forget();

        RecordingClipStruct clip = recorderManager.getKeyClip();

        refreshTitle();
        refreshLabels(clip);
        placeMarks(clip);
        refreshDetail();
    }

    // Keys or a handle being dragged, or a box drawn: only the graph's marks and the key's line move
    private void refreshCurve() {
        this.shownCurveRevision = recorderManager.getCurveRevision();
        placeMarks(recorderManager.getKeyClip());
        refreshDetail();
    }

    // The curve selected by its track and value, or what to pick when none is
    private void refreshTitle() {

        RecordingCurveTrackStruct track = recorderManager.getSelectedTrack();
        int slot = recorderManager.getSelectedSlot();

        graphTitle.setFontText(track != null && slot >= 0
                ? String.format(RecorderSetting.FORMAT_CURVE_HEADER, track.getTrackLabel(), track.getCurveLabel(slot))
                : RecorderSetting.TEXT_GRAPH_TITLE);
    }

    // The greatest and least values framed, or how to begin when no curve is selected
    private void refreshLabels(RecordingClipStruct clip) {

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

    // The value at the playhead and the key count, with the key shown: its moment, value and mode
    private void refreshDetail() {

        RecordingClipStruct clip = recorderManager.getKeyClip();
        RecordingCurveStruct curve = recorderManager.getSelectedCurve();

        if (clip == null || curve == null) {
            graphDetail.setFontText(recorderManager.getSelectedTrack() != null
                    ? RecorderSetting.TEXT_CURVE_UNLAID
                    : RecorderSetting.TEXT_EMPTY);
            return;
        }

        String now = recorderManager.formatValue(
                curve.getProperty().getKind(), curve.evaluate(clip.toChildTime(recorderManager.getPlayhead())));
        int key = recorderManager.getSelectedKey();

        if (key < 0 || key >= curve.size()) {
            graphDetail.setFontText(String.format(RecorderSetting.FORMAT_GRAPH_NOW, now, curve.size()));
            return;
        }

        graphDetail.setFontText(String.format(RecorderSetting.FORMAT_GRAPH_KEY,
                now,
                curve.size(),
                recorderRowSystem.formatTime(clip.toTimelineTime(curve.getTime(key))),
                recorderManager.formatValue(curve.getProperty().getKind(), curve.getValue(key)),
                curve.getMode(key).getLabel()));
    }

    // Marks \\

    private void placeMarks(RecordingClipStruct clip) {

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

    // The selected curve across the view as a line through its keys, every key in view drawn whether or not the clip
    // plays that far, a key too close to the last drawn left out unless selected, and the handles of the selected
    // key drawn out
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

            if (!recorderManager.getView().contains(time))
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

    // The curve sampled every few pixels across the part in view of all it reaches — the whole of its take, and any
    // key lying past it — each stretch a turned line, so every key that shapes what the clip plays is seen
    private void placeCurveLine(RecordingClipStruct clip, RecordingCurveStruct curve, float width, float height) {

        if (curve.isEmpty())
            return;

        RecordingViewStruct view = recorderManager.getView();
        float reachStart = clip.toTimelineTime(Math.min(0f, curve.getTime(0)));
        float reachEnd = clip.toTimelineTime(Math.max(clip.getTake().getDuration(), curve.getTime(curve.size() - 1)));
        float from = Math.max(view.getStart(), reachStart);
        float to = Math.min(view.getEnd(), reachEnd);

        if (to <= from)
            return;

        int segments = Math.max(1, (int) Math.ceil(
                (to - from) / view.getSpan() * width / RecorderSetting.GRAPH_SEGMENT_PIXELS));
        float lastX = toGraphX(from, width);
        float lastY = toGraphY(curve.evaluate(clip.toChildTime(from)), height);

        for (int i = 1; i <= segments; i++) {

            float time = from + (to - from) * i / segments;
            float x = toGraphX(time, width);
            float y = toGraphY(curve.evaluate(clip.toChildTime(time)), height);

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

    // Utility \\

    private float toGraphX(float time, float width) {
        return recorderManager.getView().toFraction(time) * width;
    }

    // How high a value stands on the graph, the least framed at the bottom and the greatest at the top
    private float toGraphY(double value, float height) {

        double minimum = recorderManager.getGraphMinimum();

        return (float) ((value - minimum) / (recorderManager.getGraphMaximum() - minimum) * height);
    }
}
