package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.bootstrap.recorderpipeline.recording.RecordingDragStruct;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.runtime.EditorSetting;
import engine.input.Buttons;
import engine.input.Input;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderBranch extends BranchPackage {

    /*
     * Menu event handlers for the Recorder tab. Every action works on the one
     * recorder all tabs share. New and renamed recordings are named through
     * the name dialog, takes through the text dialog, and deleting a recording
     * or a take asks for its name to be typed back. Dragging along the ruler
     * moves the playhead to the moment under the cursor. Pressing a take in
     * the bin picks it up, and letting go over the lanes lays it there. A
     * press on the lanes is handed to the recorder once, where it began, to
     * take hold of a clip's edge, a node, a clip or the empty lanes; letting
     * go trims, retimes, moves or scrolls, and a press that never travels
     * selects instead, or on an empty lane moves the playhead there. Holding
     * Shift lets a drag go free of the timeline's edges. Every modifier a drag
     * or the wheel reads is a held shortcut, resolved once from its name, so
     * each is bound in the Recorder's shortcut file. While the Cut tool is
     * chosen, the recorder is told every frame where it would cut with the pointer
     * over the lanes. The wheel over the lanes, the ruler, the Keys rows'
     * strips or the Keys graph zooms about the pointer, and with Shift, or
     * scrolled sideways, moves the view along; with Ctrl on the graph it zooms
     * the graph's values, and over the Keys rows' names it scrolls the rows.
     * The toolbar shows the Keys, the Bin or the Assets below the timeline. On
     * the Keys pane a name opens its track or selects its curve, a press on a
     * row's strip or on the graph is handed to the recorder once to take hold
     * of a key, a handle or a node, or on nothing to scroll the view as it
     * travels; with Shift a key pressed joins the keys selected and a press
     * on nothing boxes keys to add, with Ctrl to take away. The value under
     * the cursor on the graph is read from the
     * span of values it shows. On the Assets pane a section's head opens or
     * closes it. The drag is read from the window the press began in until the
     * button is let go. Rows hand back the index of the take they show, Keys
     * rows their track's place and what they show, section heads their
     * section, and setting rows the key of what they change. Exporting goes
     * through the export dialog, which holds the editor until the video is
     * finished. Deleting removes whatever is selected, keys before a node
     * before a clip.
     */

    // Internal
    private RecorderManager recorderManager;
    private NameDialogBranch nameDialogBranch;
    private ExportDialogBranch exportDialogBranch;
    private InputManager inputManager;
    private WindowManager windowManager;
    private ShortcutManager shortcutManager;

    // Shortcuts
    private int freeDragShortcutID;
    private int scrollViewShortcutID;
    private int zoomValuesShortcutID;
    private int extendSelectionShortcutID;
    private int subtractSelectionShortcutID;

    // Base \\

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.exportDialogBranch = get(ExportDialogBranch.class);
        this.inputManager = get(InputManager.class);
        this.windowManager = get(WindowManager.class);
        this.shortcutManager = get(ShortcutManager.class);
    }

    @Override
    protected void start() {

        // Shortcuts
        this.freeDragShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                EditorSetting.RECORDER_SHORTCUT_FREE_DRAG);
        this.scrollViewShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                EditorSetting.RECORDER_SHORTCUT_SCROLL_VIEW);
        this.zoomValuesShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                EditorSetting.RECORDER_SHORTCUT_ZOOM_VALUES);
        this.extendSelectionShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                EditorSetting.RECORDER_SHORTCUT_EXTEND_SELECTION);
        this.subtractSelectionShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                EditorSetting.RECORDER_SHORTCUT_SUBTRACT_SELECTION);
    }

    @Override
    protected void update() {
        updateDrag();
        hoverCut();
        scrollTimeline();
    }

    // A drag ends when its button is let go, and is dropped when its window leaves
    private void updateDrag() {

        RecordingDragStruct drag = recorderManager.getDrag();

        if (!drag.isActive())
            return;

        if (!windowManager.getWindows().contains(drag.getWindow())) {
            recorderManager.cancelDrag();
            return;
        }

        if (!inputManager.getRawInput(drag.getWindow()).isMouseDown(Buttons.LEFT))
            recorderManager.endDrag();
    }

    // Where the cut tool would cut, read from the lanes of the Recorder tab under the pointer
    private void hoverCut() {

        if (!recorderManager.isCutting())
            return;

        WindowInstance window = windowManager.getHoveredWindow();
        MenuInstance menu = window != null ? findRecorderMenu(window) : null;
        ElementInstance lanes = menu != null ? menu.getEntryPoint(EditorSetting.RECORDER_ENTRY_CLIP_LANES) : null;

        if (recorderManager.getDrag().isActive() || !recorderManager.hasRecording() || lanes == null
                || lanes.getComputedW() <= 0f || lanes.getComputedH() <= 0f || !isUnderCursor(window, lanes)) {
            recorderManager.clearCutHover();
            return;
        }

        recorderManager.hoverCut(
                resolveTime(window, lanes),
                resolveLane(window, lanes),
                resolveSecondsPerPixel(lanes),
                isSnapping(window));
    }

    // The wheel works alike over the lanes, the ruler, the Keys rows' strips and the Keys graph: it zooms about the
    // pointer, and with Shift, or scrolled sideways, moves the view along; with Ctrl on the graph it zooms its values
    // instead. Over the Keys rows' names it scrolls the rows
    private void scrollTimeline() {

        WindowInstance window = windowManager.getHoveredWindow();

        if (window == null)
            return;

        Input rawInput = inputManager.getRawInput(window);
        float wheel = rawInput.getScrollY();
        float sideways = rawInput.getScrollX();

        if (wheel == 0f && sideways == 0f)
            return;

        MenuInstance menu = findRecorderMenu(window);

        if (menu == null)
            return;

        ElementInstance lanes = menu.getEntryPoint(EditorSetting.RECORDER_ENTRY_CLIP_LANES);
        ElementInstance ruler = menu.getEntryPoint(EditorSetting.RECORDER_ENTRY_RULER);
        ElementInstance graph = menu.getEntryPoint(EditorSetting.RECORDER_ENTRY_KEY_GRAPH);
        ElementInstance keyRows = menu.getEntryPoint(EditorSetting.RECORDER_ENTRY_KEY_ROWS);
        boolean keys = recorderManager.getPane() == RecorderPane.KEYS;
        boolean overGraph = keys && isUnderCursor(window, graph);
        boolean overRows = keys && isUnderCursor(window, keyRows);

        if (overRows && inputManager.getHoverMouseX(window) < graph.getComputedLeft()) {
            keyRows.setScrollY(keyRows.getScrollY() - wheel * EngineSetting.MENU_SCROLL_PIXELS);
            return;
        }

        ElementInstance hovered = isUnderCursor(window, lanes) ? lanes
                : isUnderCursor(window, ruler) ? ruler
                        : overGraph || overRows ? graph : null;

        if (hovered == null)
            return;

        if (sideways != 0f)
            recorderManager.scrollView(sideways * EditorSetting.RECORDER_SCROLL_SHARE);

        if (wheel == 0f)
            return;

        float factor = (float) Math.pow(EditorSetting.RECORDER_ZOOM_STEP, -wheel);

        if (shortcutManager.isHeld(scrollViewShortcutID, window))
            recorderManager.scrollView(-wheel * EditorSetting.RECORDER_SCROLL_SHARE);
        else if (overGraph && shortcutManager.isHeld(zoomValuesShortcutID, window))
            recorderManager.zoomGraph(resolveValue(window, graph), factor);
        else
            recorderManager.zoomView(resolveTime(window, hovered), factor);
    }

    // Recordings \\

    public void newRecording(WindowInstance window) {
        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_NEW_RECORDING,
                recorderManager::isRecordingNameAvailable,
                recorderManager::createRecording);
    }

    public void openRecording(String recordingName) {
        recorderManager.openRecording(recordingName);
    }

    public void saveRecording() {
        recorderManager.saveRecording();
    }

    public void renameRecording(WindowInstance window) {

        if (!recorderManager.hasRecording())
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_RENAME_RECORDING,
                recorderManager::isRecordingNameAvailable,
                recorderManager::renameRecording);
    }

    public void deleteRecording(WindowInstance window) {

        if (!recorderManager.hasRecording())
            return;

        String recordingName = recorderManager.getRecording().getRecordingName();

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_DELETE_PREFIX + recordingName + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX,
                recordingName::equals,
                confirmedName -> recorderManager.deleteRecording());
    }

    // Transport \\

    public void record() {
        recorderManager.record();
    }

    public void play() {
        recorderManager.play();
    }

    public void pause() {
        recorderManager.pause();
    }

    public void togglePlayback() {
        recorderManager.togglePlayback();
    }

    public void stop() {
        recorderManager.stop();
    }

    public void toStart() {
        recorderManager.toStart();
    }

    public void toEnd() {
        recorderManager.toEnd();
    }

    public void scrubTimeline(WindowInstance window, ElementInstance track) {

        if (track.getComputedW() > 0f)
            recorderManager.scrub(resolveFraction(window, track));
    }

    // View \\

    public void zoomIn() {
        recorderManager.zoomViewIn();
    }

    public void zoomOut() {
        recorderManager.zoomViewOut();
    }

    public void zoomFit() {
        recorderManager.fitView();
    }

    // Panes \\

    public void showKeys() {
        recorderManager.showPane(RecorderPane.KEYS);
    }

    public void showBin() {
        recorderManager.showPane(RecorderPane.BIN);
    }

    public void showAssets() {
        recorderManager.showPane(RecorderPane.ASSETS);
    }

    // Keys \\

    public void frameKeys() {
        recorderManager.frameKeys();
    }

    public void selectAllKeys() {
        recorderManager.selectAllKeys();
    }

    public void deleteSelection() {
        recorderManager.deleteSelection();
    }

    // A track's name opens or closes it, a curve's name selects it
    public void pressKeyName(String rowKey) {

        int trackIndex = resolveKeyTrack(rowKey);
        int row = resolveKeyRow(rowKey);

        if (row == EditorSetting.RECORDER_KEY_ROW_TRACK)
            recorderManager.toggleTrack(trackIndex);
        else if (row >= 0)
            recorderManager.selectCurve(trackIndex, row);
    }

    // A press on a row's strip is settled once, where it began, and what it took hold of follows the cursor
    public void dragKeyStrip(String rowKey, WindowInstance window, ElementInstance strip) {

        if (strip.getComputedW() <= 0f)
            return;

        if (!recorderManager.getDrag().isActive())
            recorderManager.pressKeyRow(
                    resolveKeyTrack(rowKey),
                    resolveKeyRow(rowKey),
                    resolveTime(window, strip),
                    resolveSecondsPerPixel(strip),
                    shortcutManager.isHeld(extendSelectionShortcutID, window),
                    shortcutManager.isHeld(subtractSelectionShortcutID, window),
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));

        recorderManager.dragKeysTo(
                inputManager.getHoverMouseX(window),
                inputManager.getHoverMouseY(window),
                resolveTime(window, strip),
                0.0,
                resolveSecondsPerPixel(strip),
                isSnapping(window));
    }

    public void dragGraph(WindowInstance window, ElementInstance graph) {

        if (graph.getComputedW() <= 0f || graph.getComputedH() <= 0f)
            return;

        if (!recorderManager.getDrag().isActive())
            recorderManager.pressGraph(
                    resolveTime(window, graph),
                    resolveValue(window, graph),
                    resolveSecondsPerPixel(graph),
                    (recorderManager.getGraphMaximum() - recorderManager.getGraphMinimum()) / graph.getComputedH(),
                    shortcutManager.isHeld(extendSelectionShortcutID, window),
                    shortcutManager.isHeld(subtractSelectionShortcutID, window),
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));

        recorderManager.dragKeysTo(
                inputManager.getHoverMouseX(window),
                inputManager.getHoverMouseY(window),
                resolveTime(window, graph),
                resolveValue(window, graph),
                resolveSecondsPerPixel(graph),
                isSnapping(window));
    }

    // The value under the cursor on the graph, the bottom of the graph its least and the top its greatest
    private double resolveValue(WindowInstance window, ElementInstance graph) {

        double minimum = recorderManager.getGraphMinimum();
        float height = Math.clamp(
                (inputManager.getHoverMouseY(window) - graph.getComputedTop()) / graph.getComputedH(), 0f, 1f);

        return minimum + (recorderManager.getGraphMaximum() - minimum) * height;
    }

    private int resolveKeyTrack(String rowKey) {
        return Integer.parseInt(rowKey.substring(0, rowKey.indexOf(EditorSetting.RECORDER_KEY_ROW_SEPARATOR)));
    }

    private int resolveKeyRow(String rowKey) {

        int separator = rowKey.indexOf(EditorSetting.RECORDER_KEY_ROW_SEPARATOR);

        return Integer.parseInt(rowKey.substring(separator + EditorSetting.RECORDER_KEY_ROW_SEPARATOR.length()));
    }

    private void openKeyValue(WindowInstance window) {

        if (recorderManager.getSelectedCurve() == null || recorderManager.getSelectedKey() < 0)
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_KEY_VALUE,
                recorderManager.formatKeyValueInput(),
                recorderManager::isKeyValueValid,
                recorderManager::setKeyValue);
    }

    // Takes \\

    // The take is picked up on the press and carried over the lanes until let go
    public void dragTake(String takeKey, WindowInstance window) {

        if (!recorderManager.getDrag().isActive())
            recorderManager.beginTakeDrag(
                    Integer.parseInt(takeKey),
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));

        dragOver(window, findLanes(window));
    }

    public void placeTake(String takeKey) {
        recorderManager.placeTakeAtPlayhead(Integer.parseInt(takeKey));
    }

    public void renameTake(String takeKey, WindowInstance window) {

        int takeIndex = Integer.parseInt(takeKey);

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_RENAME_TAKE,
                recorderManager.getRecording().getTakes().get(takeIndex).getName(),
                recorderManager::isLabelValid,
                takeName -> recorderManager.renameTake(takeIndex, takeName));
    }

    public void deleteTake(String takeKey, WindowInstance window) {

        int takeIndex = Integer.parseInt(takeKey);
        String takeName = recorderManager.getRecording().getTakes().get(takeIndex).getName();

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_DELETE_PREFIX + takeName + EditorSetting.DIALOG_TITLE_DELETE_SUFFIX,
                "",
                takeName::equals,
                confirmedName -> recorderManager.deleteTake(takeIndex));
    }

    // Timeline \\

    // What a press on the lanes takes hold of is settled once, where it began, and carried until let go
    public void dragLanes(WindowInstance window, ElementInstance lanes) {

        if (lanes.getComputedW() <= 0f || lanes.getComputedH() <= 0f)
            return;

        if (!recorderManager.getDrag().isActive())
            recorderManager.pressLanes(
                    resolveTime(window, lanes),
                    resolveLane(window, lanes),
                    isNameRow(window, lanes),
                    resolveSecondsPerPixel(lanes),
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));

        dragOver(window, lanes);
    }

    // Tells the recorder where what is carried would land with the cursor where it is now
    private void dragOver(WindowInstance window, ElementInstance lanes) {

        float x = inputManager.getHoverMouseX(window);
        float y = inputManager.getHoverMouseY(window);

        if (lanes == null || lanes.getComputedW() <= 0f || lanes.getComputedH() <= 0f) {
            recorderManager.dragTo(x, y, false, 0f, 0, 0f, false);
            return;
        }

        recorderManager.dragTo(
                x,
                y,
                isUnderCursor(window, lanes),
                resolveTime(window, lanes),
                resolveLane(window, lanes),
                resolveSecondsPerPixel(lanes),
                isSnapping(window));
    }

    // How far along the element the cursor stands, 0 to 1
    private float resolveFraction(WindowInstance window, ElementInstance element) {
        return Math.clamp(
                (inputManager.getHoverMouseX(window) - element.getComputedLeft()) / element.getComputedW(), 0f, 1f);
    }

    // The moment of the timeline under the cursor, as the view shows it
    private float resolveTime(WindowInstance window, ElementInstance element) {
        return recorderManager.getView().toTime(resolveFraction(window, element));
    }

    private float resolveSecondsPerPixel(ElementInstance element) {
        return recorderManager.getView().getSpan() / element.getComputedW();
    }

    // The lane under the cursor, the highest drawn at the top
    private int resolveLane(WindowInstance window, ElementInstance lanes) {
        return EditorSetting.RECORDER_LANE_COUNT - 1 - resolveRow(window, lanes);
    }

    // The row under the cursor counted down from the top of the lanes, the cursor's height running upward
    private int resolveRow(WindowInstance window, ElementInstance lanes) {

        float laneHeight = lanes.getComputedH() / EditorSetting.RECORDER_LANE_COUNT;
        int row = (int) Math.floor(resolveDepth(window, lanes) / laneHeight);

        return Math.clamp(row, 0, EditorSetting.RECORDER_LANE_COUNT - 1);
    }

    // How far below the top of the lanes the cursor stands
    private float resolveDepth(WindowInstance window, ElementInstance lanes) {
        return lanes.getComputedTop() + lanes.getComputedH() - inputManager.getHoverMouseY(window);
    }

    // True when the cursor is in the upper row of its lane, where a clip's name and gestures lie
    private boolean isNameRow(WindowInstance window, ElementInstance lanes) {

        float laneHeight = lanes.getComputedH() / EditorSetting.RECORDER_LANE_COUNT;
        float within = resolveDepth(window, lanes) - resolveRow(window, lanes) * laneHeight;

        return within < laneHeight
                * (EditorSetting.RECORDER_CLIP_BAR_INSET_PIXELS + EditorSetting.RECORDER_CLIP_NAME_ROW_PIXELS)
                / EditorSetting.RECORDER_LANE_PIXELS;
    }

    private boolean isUnderCursor(WindowInstance window, ElementInstance element) {

        if (element == null)
            return false;

        float x = inputManager.getHoverMouseX(window);
        float y = inputManager.getHoverMouseY(window);

        return x >= element.getComputedLeft() && x <= element.getComputedLeft() + element.getComputedW()
                && y >= element.getComputedTop() && y <= element.getComputedTop() + element.getComputedH();
    }

    // What is dragged snaps to the timeline's edges unless the free drag is held
    private boolean isSnapping(WindowInstance window) {
        return !shortcutManager.isHeld(freeDragShortcutID, window);
    }

    // The lanes of the Recorder tab open in the window, null when it shows none
    private ElementInstance findLanes(WindowInstance window) {

        MenuInstance menu = findRecorderMenu(window);

        return menu != null ? menu.getEntryPoint(EditorSetting.RECORDER_ENTRY_CLIP_LANES) : null;
    }

    private MenuInstance findRecorderMenu(WindowInstance window) {

        ObjectArrayList<MenuInstance> menus = window.getMenuListHandle().getMenus();

        for (int i = 0; i < menus.size(); i++)
            if (menus.get(i).getMenuData().getName().equals(EditorSetting.RECORDER_MENU))
                return menus.get(i);

        return null;
    }

    // Assets \\

    public void toggleAssetSection(String sectionKey) {
        recorderManager.toggleAssetSection(Integer.parseInt(sectionKey));
    }

    // Settings \\

    public void actSetting(String settingKey, WindowInstance window) {

        if (settingKey.startsWith(EditorSetting.RECORDER_KEY_ARM_PREFIX)) {
            recorderManager.toggleArm(resolveChannel(settingKey, EditorSetting.RECORDER_KEY_ARM_PREFIX));
            return;
        }

        if (settingKey.startsWith(EditorSetting.RECORDER_KEY_CLIP_CHANNEL_PREFIX)) {
            recorderManager.toggleClipChannel(
                    resolveChannel(settingKey, EditorSetting.RECORDER_KEY_CLIP_CHANNEL_PREFIX));
            return;
        }

        switch (settingKey) {
            case EditorSetting.RECORDER_KEY_CLIP_MUTE -> recorderManager.toggleClipMute();
            case EditorSetting.RECORDER_KEY_CLIP_RAMP -> recorderManager.cycleClipTimeRamp();
            case EditorSetting.RECORDER_KEY_CLIP_TRIM_START -> recorderManager.trimClipStart();
            case EditorSetting.RECORDER_KEY_CLIP_TRIM_END -> recorderManager.trimClipEnd();
            case EditorSetting.RECORDER_KEY_CLIP_SPLIT -> recorderManager.splitClip();
            case EditorSetting.RECORDER_KEY_CLIP_STAMP -> recorderManager.stampClip();
            case EditorSetting.RECORDER_KEY_CLIP_REMOVE -> recorderManager.removeClip();
            case EditorSetting.RECORDER_KEY_NODE_NEXT -> recorderManager.nextNodeState();
            case EditorSetting.RECORDER_KEY_NODE_PREVIOUS -> recorderManager.previousNodeState();
            case EditorSetting.RECORDER_KEY_NODE_REMOVE -> recorderManager.removeNode();
            case EditorSetting.RECORDER_KEY_KEY_ADD -> recorderManager.addKeyAtPlayhead();
            case EditorSetting.RECORDER_KEY_KEY_DELETE -> recorderManager.deleteKeys();
            case EditorSetting.RECORDER_KEY_KEY_MODE -> recorderManager.cycleKeyMode();
            case EditorSetting.RECORDER_KEY_KEY_VALUE -> openKeyValue(window);
            case EditorSetting.RECORDER_KEY_CURVE_REFIT -> recorderManager.refitCurve();
            case EditorSetting.RECORDER_KEY_TAKE_REFIT -> recorderManager.refitTake();
            case EditorSetting.RECORDER_KEY_RESOLUTION -> recorderManager.cycleResolution();
            case EditorSetting.RECORDER_KEY_FRAME_RATE -> recorderManager.cycleFrameRate();
            case EditorSetting.RECORDER_KEY_FORMAT -> recorderManager.cycleFormat();
            case EditorSetting.RECORDER_KEY_MOTION_BLUR -> recorderManager.cycleMotionBlur();
            case EditorSetting.RECORDER_KEY_SUPERSAMPLE -> recorderManager.toggleSupersample();
            case EditorSetting.RECORDER_KEY_RANGE_START -> recorderManager.setRangeStart();
            case EditorSetting.RECORDER_KEY_RANGE_END -> recorderManager.setRangeEnd();
            case EditorSetting.RECORDER_KEY_RANGE_CLEAR -> recorderManager.clearRange();
            case EditorSetting.RECORDER_KEY_EXPORT -> exportDialogBranch.export(window);
            case EditorSetting.RECORDER_KEY_CANCEL_EXPORT -> recorderManager.stop();
            default -> throwException("Unknown recorder setting: " + settingKey);
        }
    }

    private RecorderChannel resolveChannel(String settingKey, String prefix) {

        RecorderChannel channel = RecorderChannel.findByKey(settingKey.substring(prefix.length()));

        if (channel == null)
            throwException("Unknown recorder channel: " + settingKey);

        return channel;
    }
}
