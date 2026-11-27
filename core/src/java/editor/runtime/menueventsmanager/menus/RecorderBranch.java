package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderClipEdge;
import editor.bootstrap.recorderpipeline.recording.RecorderDragKind;
import editor.bootstrap.recorderpipeline.recording.RecordingDragStruct;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.bootstrap.soundlibrarypipeline.soundlibrarymanager.SoundLibraryManager;
import editor.bootstrap.transitionpipeline.transitionmanager.TransitionManager;
import editor.runtime.EditorSetting;
import engine.input.Buttons;
import engine.input.Input;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderBranch extends BranchPackage {

    /*
     * Menu event handlers for the Recorder tabs — the transport, the
     * Timeline, the Keys and the Keys Graph — and the Tools tab's actions on
     * them, each the same key its shortcut runs. Every action
     * works on the one recorder all tabs share; the recordings themselves are
     * made, renamed, copied and deleted from the Recordings tool, where a row
     * pressed is picked up and laid nested in the open recording when let go
     * over the Timeline's lanes, or opened when let go where it was pressed.
     * The lane strips select, mute and solo their lanes, the Properties rows
     * step, cycle and take typed values, and the File menu's Export sends a
     * video through the export dialog and exports music at once; Export MIDI
     * writes the music among the player's songs. Play loops round the range
     * while the Loop switch is on.
     * The range's ends are set at the playhead, and Back returns to
     * the recording the open one was opened inside. A note pressed in the
     * Note Library is picked up and laid as a note clip the same way.
     * Nest Selection names its new recording through the name dialog. Takes are named
     * through the text dialog, and deleting a take asks for its name to be
     * typed back. Dragging along the ruler
     * moves the playhead to the moment under the cursor. A take pressed in the
     * Palette tab is picked up, and letting go over the lanes
     * lays it there, or where it was pressed picks it; the take picked is
     * laid at the playhead, renamed or deleted by the take actions. A sound pressed in the sound library is picked up and laid
     * as a take of its own over the lanes of whichever Recorder tab the
     * cursor is let go above, in any window, or let go over the rows of a
     * Keys tab is added to the take shown there at the moment under the
     * cursor; a transition pressed in the transitions tool is picked up the
     * same way and laid on the clip edge of whichever Recorder tab's lanes it
     * is let go over. A
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
     * On the Keys tab a name selects its curve, a press on a
     * row's strip or on the graph is handed to the recorder once to take hold
     * of a key, a handle or a node, or on nothing to scroll the view as it
     * travels; a press on nothing boxes keys across every row or the graph,
     * with Shift to add them, with Ctrl to take them away, and the pan
     * button drags the rows' or the graph's view along; a sound's bar on
     * the Sounds layer is selected and dragged along its row to retime it, and
     * its volume, pitch and length are typed in the text dialog, while the
     * Sound Library's selection is what a sound is added from or replaced
     * with; a selected transition's length is typed the same way, and the
     * transitions tool's selection is what one is laid from or replaced
     * with. The value under the cursor on the graph is read from the span of
     * values it shows. On the Assets pane a section's head opens or
     * closes it. The drag is read from the window the press began in until the
     * button is let go. Rows hand back the index of the take they show, Keys
     * rows their track's place and what they show, section heads their
     * section, and setting rows the key of what they change. Exporting goes
     * through the export dialog, which holds the editor until the video is
     * finished. Deleting removes whatever is selected, keys before a sound
     * before a transition before a node before a clip.
     */

    // Internal
    private RecorderManager recorderManager;
    private NameDialogBranch nameDialogBranch;
    private ExportDialogBranch exportDialogBranch;
    private InputManager inputManager;
    private WindowManager windowManager;
    private ShortcutManager shortcutManager;
    private SoundManager soundManager;
    private SoundLibraryManager soundLibraryManager;
    private TransitionManager transitionManager;

    // Shortcuts
    private int freeDragShortcutID;
    private int scrollViewShortcutID;
    private int zoomValuesShortcutID;
    private int extendSelectionShortcutID;
    private int subtractSelectionShortcutID;

    // Scroll
    private ElementInstance scrolledStrip;

    // Base \\

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.exportDialogBranch = get(ExportDialogBranch.class);
        this.inputManager = get(InputManager.class);
        this.windowManager = get(WindowManager.class);
        this.shortcutManager = get(ShortcutManager.class);
        this.soundManager = get(SoundManager.class);
        this.soundLibraryManager = get(SoundLibraryManager.class);
        this.transitionManager = get(TransitionManager.class);
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
        panTimeline();
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

        int button = drag.getKind().isPanned() ? EditorSetting.RECORDER_BUTTON_PAN : Buttons.LEFT;

        if (!inputManager.getRawInput(drag.getWindow()).isMouseDown(button))
            recorderManager.endDrag();
    }

    // Where the cut tool would cut, read from the lanes of the Recorder tab under the pointer
    private void hoverCut() {

        if (!recorderManager.isCutting())
            return;

        WindowInstance window = windowManager.getHoveredWindow();
        MenuInstance menu = window != null ? findMenu(window, EditorSetting.RECORDER_TIMELINE_MENU) : null;
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

        MenuInstance timelineMenu = findMenu(window, EditorSetting.RECORDER_TIMELINE_MENU);
        MenuInstance keysMenu = findMenu(window, EditorSetting.RECORDER_KEYS_MENU);
        MenuInstance graphMenu = findMenu(window, EditorSetting.RECORDER_KEY_GRAPH_MENU);

        if (timelineMenu == null && keysMenu == null && graphMenu == null)
            return;

        ElementInstance lanes = findEntry(timelineMenu, EditorSetting.RECORDER_ENTRY_CLIP_LANES);
        ElementInstance ruler = findEntry(timelineMenu, EditorSetting.RECORDER_ENTRY_RULER);
        ElementInstance graph = findEntry(graphMenu, EditorSetting.RECORDER_ENTRY_KEY_GRAPH);
        ElementInstance keyRows = findEntry(keysMenu, EditorSetting.RECORDER_ENTRY_KEY_ROWS);
        ElementInstance keyStrips = findEntry(keysMenu, EditorSetting.RECORDER_ENTRY_KEY_STRIPS);
        boolean overGraph = isUnderCursor(window, graph);
        boolean overStrips = isUnderCursor(window, keyStrips);

        if (!overStrips && isUnderCursor(window, keyRows)) {
            keyRows.setScrollY(keyRows.getScrollY() - wheel * EngineSetting.MENU_SCROLL_PIXELS);
            return;
        }

        ElementInstance hovered = isUnderCursor(window, lanes) ? lanes
                : isUnderCursor(window, ruler) ? ruler
                        : overGraph ? graph
                                : overStrips ? keyStrips : null;

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

    // The pan button pressed over the lanes, the ruler or the Keys rows' strips drags the view along under the cursor
    // until let go, over a clip or a key as much as over nothing, so a zoomed timeline scrolls wherever it is held;
    // over the Keys graph it drags the graph's values along with it
    private void panTimeline() {

        RecordingDragStruct drag = recorderManager.getDrag();

        if (drag.isActive()) {

            WindowInstance dragWindow = drag.getWindow();

            if (drag.getKind() == RecorderDragKind.SCROLL)
                recorderManager.dragTo(
                        inputManager.getHoverMouseX(dragWindow),
                        inputManager.getHoverMouseY(dragWindow),
                        false,
                        0f,
                        0,
                        resolveSecondsPerPixel(scrolledStrip),
                        false);
            else if (drag.getKind() == RecorderDragKind.GRAPH_PAN && scrolledStrip.getComputedH() > 0f)
                recorderManager.dragKeysTo(
                        inputManager.getHoverMouseX(dragWindow),
                        inputManager.getHoverMouseY(dragWindow),
                        resolveTime(dragWindow, scrolledStrip),
                        resolveValue(dragWindow, scrolledStrip),
                        resolveSecondsPerPixel(scrolledStrip),
                        0.0,
                        false);

            return;
        }

        WindowInstance window = windowManager.getHoveredWindow();

        if (window == null || !inputManager.getRawInput(window).isMouseClicked(EditorSetting.RECORDER_BUTTON_PAN))
            return;

        MenuInstance timelineMenu = findMenu(window, EditorSetting.RECORDER_TIMELINE_MENU);
        ElementInstance lanes = findEntry(timelineMenu, EditorSetting.RECORDER_ENTRY_CLIP_LANES);
        ElementInstance ruler = findEntry(timelineMenu, EditorSetting.RECORDER_ENTRY_RULER);
        ElementInstance keyStrips = findEntry(
                findMenu(window, EditorSetting.RECORDER_KEYS_MENU), EditorSetting.RECORDER_ENTRY_KEY_STRIPS);
        ElementInstance graph = findEntry(
                findMenu(window, EditorSetting.RECORDER_KEY_GRAPH_MENU), EditorSetting.RECORDER_ENTRY_KEY_GRAPH);
        ElementInstance strip = isUnderCursor(window, lanes) ? lanes
                : isUnderCursor(window, ruler) ? ruler
                        : isUnderCursor(window, keyStrips) ? keyStrips
                                : isUnderCursor(window, graph) ? graph : null;

        if (strip == null || strip.getComputedW() <= 0f)
            return;

        this.scrolledStrip = strip;

        if (strip == graph)
            recorderManager.pressGraphPan(
                    resolveTime(window, graph),
                    resolveValue(window, graph),
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));
        else
            recorderManager.pressViewScroll(
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));
    }

    // Recordings \\

    public void saveRecording() {
        recorderManager.saveRecording();
    }

    public void undo() {
        recorderManager.undo();
    }

    public void redo() {
        recorderManager.redo();
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

    public void toggleLoop() {
        recorderManager.toggleLoop();
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

    public void setRangeStart() {
        recorderManager.setRangeStart();
    }

    public void setRangeEnd() {
        recorderManager.setRangeEnd();
    }

    public void backToParent() {
        recorderManager.backToParent();
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

    // A value's name shows its curve on the Keys Graph, keeping the keys selected, and a sound's name selects the first
    // time it is heard; a bone's or morph's summary row on the dope sheet picks it, its values listed in the
    // Properties tab and under its own row, and its first shown on the Keys Graph
    public void pressKeyName(String rowKey) {

        int row = resolveKeyRow(rowKey);

        if (row >= 0)
            recorderManager.selectCurve(resolveKeyTrack(rowKey), row);
        else if (row == EditorSetting.RECORDER_KEY_ROW_SUMMARY)
            recorderManager.selectKeyLayer(resolveKeyTrack(rowKey));
    }

    // A press on a row's strip is settled once, where it began, and what it took hold of follows the cursor; a box
    // reaches over every row between where it was pressed and the cursor
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
                0.0,
                isSnapping(window));

        if (recorderManager.getDrag().getKind() == RecorderDragKind.KEYS_BOX)
            reachBoxRows(window);
    }

    // The rows of the Keys tab whose strips stand between the height the box was pressed at and the cursor's
    private void reachBoxRows(WindowInstance window) {

        RecordingDragStruct drag = recorderManager.getDrag();
        ElementInstance rows = findEntry(findMenu(window, EditorSetting.RECORDER_KEYS_MENU),
                EditorSetting.RECORDER_ENTRY_KEY_ROWS);

        if (rows == null)
            return;

        float low = Math.min(drag.getPressY(), drag.getReachY());
        float high = Math.max(drag.getPressY(), drag.getReachY());
        ObjectArrayList<ElementInstance> children = rows.getChildren();

        recorderManager.clearBoxRows();

        for (int i = 0; i < children.size(); i++) {

            ElementInstance strip = children.get(i).findChildById(EditorSetting.RECORDER_ELEMENT_KEY_STRIP);
            String rowKey = strip != null ? strip.getEffectiveOnDragArg() : null;

            if (rowKey == null || strip.getComputedTop() > high
                    || strip.getComputedTop() + strip.getComputedH() < low)
                continue;

            recorderManager.addBoxRow(resolveKeyTrack(rowKey), resolveKeyRow(rowKey));
        }
    }

    public void dragGraph(WindowInstance window, ElementInstance graph) {

        if (graph.getComputedW() <= 0f || graph.getComputedH() <= 0f)
            return;

        double valuePerPixel = (recorderManager.getGraphMaximum() - recorderManager.getGraphMinimum())
                / graph.getComputedH();

        if (!recorderManager.getDrag().isActive())
            recorderManager.pressGraph(
                    resolveTime(window, graph),
                    resolveValue(window, graph),
                    resolveSecondsPerPixel(graph),
                    valuePerPixel,
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
                valuePerPixel,
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

    // Sounds \\

    // The Sound Library's selection heard in the selected sound's place
    private void replaceSound() {
        recorderManager.replaceSound(soundLibraryManager.getSelectedSoundID());
    }

    // The Sound Library's selection added to the selected clip's take at the playhead
    private void addSound() {
        recorderManager.addSoundAtPlayhead(soundLibraryManager.getSelectedSoundID());
    }

    private void openSoundVolume(WindowInstance window) {

        if (recorderManager.getSelectedSound() < 0)
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_SOUND_VOLUME,
                recorderManager.formatSoundVolumeInput(),
                recorderManager::isSoundVolumeValid,
                recorderManager::setSoundVolume);
    }

    private void openSoundPitch(WindowInstance window) {

        if (recorderManager.getSelectedSound() < 0)
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_SOUND_PITCH,
                recorderManager.formatSoundPitchInput(),
                recorderManager::isSoundPitchValid,
                recorderManager::setSoundPitch);
    }

    private void openSoundLength(WindowInstance window) {

        if (recorderManager.getSelectedSound() < 0)
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_SOUND_LENGTH,
                recorderManager.formatSoundLengthInput(),
                recorderManager::isSoundLengthValid,
                recorderManager::setSoundLength);
    }

    private void openTransitionLength(WindowInstance window) {

        if (recorderManager.getSelectedTransitionClip() == null)
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_TRANSITION_LENGTH,
                recorderManager.formatTransitionLengthInput(),
                recorderManager::isTransitionLengthValid,
                recorderManager::setTransitionLength);
    }

    // Takes \\

    // A take pressed in the Palette tab is picked up on the press and carried over the Timeline's lanes
    // under the cursor until let go, or picked when let go where it was pressed
    public void dragTake(String rowKey, WindowInstance window) {

        int takeIndex = recorderManager.findTakeRow(rowKey);

        if (takeIndex == EngineSetting.INDEX_NOT_FOUND)
            return;

        if (!recorderManager.getDrag().isActive())
            recorderManager.beginTakeDrag(
                    takeIndex,
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));

        WindowInstance hovered = windowManager.getHoveredWindow();

        dragOver(window, hovered, hovered != null ? findLanes(hovered) : null);
    }

    // A sound is picked up on the press wherever it was pressed, and carried over the lanes or the Keys pane's rows
    // of the Recorder tab under the cursor until let go
    public void dragSound(String soundName, WindowInstance window) {

        if (!recorderManager.getDrag().isActive())
            recorderManager.beginSoundDrag(
                    soundManager.getSoundIDFromSoundName(soundName),
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));

        WindowInstance hovered = windowManager.getHoveredWindow();
        ElementInstance graph = hovered != null ? findKeyStrips(hovered) : null;

        if (graph != null) {
            recorderManager.dragSoundOverKeys(
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window),
                    resolveTime(hovered, graph));
            return;
        }

        dragOver(window, hovered, hovered != null ? findLanes(hovered) : null);
    }

    // A saved recording is picked up on the press in the Recordings tab, and carried over the Timeline's lanes under
    // the cursor until let go
    public void dragRecording(String recordingName, WindowInstance window) {

        if (!recorderManager.getDrag().isActive())
            recorderManager.beginRecordingDrag(
                    recordingName,
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));

        WindowInstance hovered = windowManager.getHoveredWindow();

        dragOver(window, hovered, hovered != null ? findLanes(hovered) : null);
    }

    // A note of an instrument is picked up on the press in the Note Library, and carried over the Timeline's lanes
    // under the cursor until let go
    public void dragNote(String instrumentName, int pitch, WindowInstance window) {

        if (!recorderManager.getDrag().isActive())
            recorderManager.beginNoteDrag(
                    instrumentName,
                    pitch,
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));

        WindowInstance hovered = windowManager.getHoveredWindow();

        dragOver(window, hovered, hovered != null ? findLanes(hovered) : null);
    }

    // The selected clips nested into a new recording of the name typed
    private void openNestSelection(WindowInstance window) {

        if (recorderManager.getSelectedClip() < 0)
            return;

        nameDialogBranch.open(
                window,
                EditorSetting.DIALOG_TITLE_NEST_SELECTION,
                recorderManager::isRecordingNameAvailable,
                recorderManager::nestSelection);
    }

    // A transition is picked up on the press wherever it was pressed, and carried over the lanes of the Recorder tab
    // under the cursor until let go
    public void dragTransition(String transitionName, WindowInstance window) {

        if (!recorderManager.getDrag().isActive())
            recorderManager.beginTransitionDrag(
                    transitionManager.getTransitionHandleFromTransitionName(transitionName).getTransitionID(),
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));

        WindowInstance hovered = windowManager.getHoveredWindow();

        dragOver(window, hovered, hovered != null ? findLanes(hovered) : null);
    }

    // A filter is picked up on the press wherever it was pressed, and carried over the lanes of the Recorder tab under
    // the cursor until let go
    public void dragFilter(String filterName, WindowInstance window) {

        if (!recorderManager.getDrag().isActive())
            recorderManager.beginFilterDrag(
                    filterName,
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));

        WindowInstance hovered = windowManager.getHoveredWindow();

        dragOver(window, hovered, hovered != null ? findLanes(hovered) : null);
    }

    // The span the Keys tab's strips share, when the cursor is over those strips in the window; null otherwise
    private ElementInstance findKeyStrips(WindowInstance window) {

        MenuInstance menu = findMenu(window, EditorSetting.RECORDER_KEYS_MENU);
        ElementInstance keyStrips = findEntry(menu, EditorSetting.RECORDER_ENTRY_KEY_STRIPS);

        return isUnderCursor(window, keyStrips) && keyStrips.getComputedW() > 0f ? keyStrips : null;
    }

    // The take picked in the Palette tab laid at the playhead, renamed as typed, or deleted once its name
    // is typed to confirm
    private void placeTake() {

        int takeIndex = recorderManager.requireSelectedTake();

        if (takeIndex != EngineSetting.INDEX_NOT_FOUND)
            recorderManager.placeTakeAtPlayhead(takeIndex);
    }

    private void renameTake(WindowInstance window) {

        int takeIndex = recorderManager.requireSelectedTake();

        if (takeIndex == EngineSetting.INDEX_NOT_FOUND)
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_RENAME_TAKE,
                recorderManager.getRecording().getTakes().get(takeIndex).getName(),
                recorderManager::isLabelValid,
                takeName -> recorderManager.renameTake(takeIndex, takeName));
    }

    private void deleteTake(WindowInstance window) {

        int takeIndex = recorderManager.requireSelectedTake();

        if (takeIndex == EngineSetting.INDEX_NOT_FOUND)
            return;

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
                    shortcutManager.isHeld(extendSelectionShortcutID, window),
                    resolveSecondsPerPixel(lanes),
                    window,
                    inputManager.getHoverMouseX(window),
                    inputManager.getHoverMouseY(window));

        dragOver(window, window, lanes);
    }

    // Tells the recorder where what is carried would land with the cursor where it is now: how far it has travelled
    // is read in the window the press began in, where it lands on the lanes in the window showing them
    private void dragOver(WindowInstance window, WindowInstance lanesWindow, ElementInstance lanes) {

        float x = inputManager.getHoverMouseX(window);
        float y = inputManager.getHoverMouseY(window);

        if (lanes == null || lanes.getComputedW() <= 0f || lanes.getComputedH() <= 0f) {
            recorderManager.dragTo(x, y, false, 0f, 0, 0f, false);
            return;
        }

        recorderManager.dragTo(
                x,
                y,
                isUnderCursor(lanesWindow, lanes),
                resolveTime(lanesWindow, lanes),
                resolveLane(lanesWindow, lanes),
                resolveSecondsPerPixel(lanes),
                isSnapping(lanesWindow));
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
        return recorderManager.getLaneCount() - 1 - resolveRow(window, lanes);
    }

    // The row under the cursor counted down from the top of the lanes, the cursor's height running upward
    private int resolveRow(WindowInstance window, ElementInstance lanes) {

        float laneHeight = lanes.getComputedH() / recorderManager.getLaneCount();
        int row = (int) Math.floor(resolveDepth(window, lanes) / laneHeight);

        return Math.clamp(row, 0, recorderManager.getLaneCount() - 1);
    }

    // How far below the top of the lanes the cursor stands
    private float resolveDepth(WindowInstance window, ElementInstance lanes) {
        return lanes.getComputedTop() + lanes.getComputedH() - inputManager.getHoverMouseY(window);
    }

    // True when the cursor is in the upper row of its lane, where a clip's name and gestures lie
    private boolean isNameRow(WindowInstance window, ElementInstance lanes) {

        float laneHeight = lanes.getComputedH() / recorderManager.getLaneCount();
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

        return findEntry(
                findMenu(window, EditorSetting.RECORDER_TIMELINE_MENU), EditorSetting.RECORDER_ENTRY_CLIP_LANES);
    }

    // The Recorder tab menu of a name open in the window, null when the window shows none
    private MenuInstance findMenu(WindowInstance window, String menuName) {

        ObjectArrayList<MenuInstance> menus = window.getMenuListHandle().getMenus();

        for (int i = 0; i < menus.size(); i++)
            if (menus.get(i).getMenuData().getName().equals(menuName))
                return menus.get(i);

        return null;
    }

    private ElementInstance findEntry(MenuInstance menu, int entryPoint) {
        return menu != null ? menu.getEntryPoint(entryPoint) : null;
    }

    // Music \\

    // The File menu's Export: a video exports through the export dialog, frame by frame; music exports at once as the
    // game's song and sound
    public void exportMedia(WindowInstance window) {

        if (recorderManager.hasPicture())
            exportDialogBranch.export(window);
        else
            recorderManager.exportMusic();
    }

    // A lane's strip on the timeline selects it, its mix then shown in the Properties tab; its ... renames it
    public void selectLane(String laneKey) {
        recorderManager.selectLane(Integer.parseInt(laneKey));
    }

    public void renameLane(String laneKey, WindowInstance window) {

        int lane = Integer.parseInt(laneKey);

        recorderManager.selectLane(lane);

        if (recorderManager.getSelectedLane() != lane)
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_LANE_NAME,
                recorderManager.formatLaneNameInput(),
                recorderManager::isLaneNameValid,
                recorderManager::setSelectedLaneName);
    }

    // Rows \\

    public void decreaseRow(String key) {
        recorderManager.stepRow(key, -1);
    }

    public void increaseRow(String key) {
        recorderManager.stepRow(key, 1);
    }

    public void previousRow(String key) {
        recorderManager.cycleRow(key, -1);
    }

    public void nextRow(String key) {
        recorderManager.cycleRow(key, 1);
    }

    public void editRow(String key, WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_EDIT_VALUE,
                recorderManager.getRowValueText(key),
                text -> recorderManager.isRowValueValid(key, text),
                text -> recorderManager.setRowValue(key, text));
    }

    // Settings \\

    public void actSetting(String settingKey, WindowInstance window) {

        if (settingKey.startsWith(EditorSetting.RECORDER_KEY_TRANSITION_PREFIX)) {
            recorderManager.applyTransition(resolveEdge(settingKey));
            return;
        }

        switch (settingKey) {
            case EditorSetting.RECORDER_KEY_TAKE_PLACE -> placeTake();
            case EditorSetting.RECORDER_KEY_TAKE_RENAME -> renameTake(window);
            case EditorSetting.RECORDER_KEY_TAKE_DELETE -> deleteTake(window);
            case EditorSetting.RECORDER_KEY_CLIP_TRIM_START -> recorderManager.trimClipStart();
            case EditorSetting.RECORDER_KEY_CLIP_TRIM_END -> recorderManager.trimClipEnd();
            case EditorSetting.RECORDER_KEY_CLIP_SPLIT -> recorderManager.splitClip();
            case EditorSetting.RECORDER_KEY_CLIP_STAMP -> recorderManager.stampClip();
            case EditorSetting.RECORDER_KEY_CLIP_REMOVE -> recorderManager.removeClip();
            case EditorSetting.RECORDER_KEY_CLIP_NEST -> openNestSelection(window);
            case EditorSetting.RECORDER_KEY_CLIP_OPEN -> recorderManager.openNested(recorderManager.getSelectedClip());
            case EditorSetting.RECORDER_KEY_NODE_NEXT -> recorderManager.nextNodeState();
            case EditorSetting.RECORDER_KEY_NODE_PREVIOUS -> recorderManager.previousNodeState();
            case EditorSetting.RECORDER_KEY_NODE_REMOVE -> recorderManager.removeNode();
            case EditorSetting.RECORDER_KEY_KEY_ADD -> recorderManager.addKeyAtPlayhead();
            case EditorSetting.RECORDER_KEY_KEY_DELETE -> recorderManager.deleteKeys();
            case EditorSetting.RECORDER_KEY_KEY_COPY -> recorderManager.copyKeys();
            case EditorSetting.RECORDER_KEY_KEY_PASTE -> recorderManager.pasteKeys();
            case EditorSetting.RECORDER_KEY_KEY_MODE -> recorderManager.cycleKeyMode();
            case EditorSetting.RECORDER_KEY_KEY_VALUE -> openKeyValue(window);
            case EditorSetting.RECORDER_KEY_FRAME_KEYS -> recorderManager.frameKeys();
            case EditorSetting.RECORDER_KEY_CURVE_REFIT -> recorderManager.refitCurve();
            case EditorSetting.RECORDER_KEY_TAKE_REFIT -> recorderManager.refitTake();
            case EditorSetting.RECORDER_KEY_SOUND_VOLUME -> openSoundVolume(window);
            case EditorSetting.RECORDER_KEY_SOUND_PITCH -> openSoundPitch(window);
            case EditorSetting.RECORDER_KEY_SOUND_LENGTH -> openSoundLength(window);
            case EditorSetting.RECORDER_KEY_SOUND_REPLACE -> replaceSound();
            case EditorSetting.RECORDER_KEY_SOUND_COPY -> recorderManager.copySoundToPlayhead();
            case EditorSetting.RECORDER_KEY_SOUND_ADD -> addSound();
            case EditorSetting.RECORDER_KEY_SOUND_SEEK -> recorderManager.seekSound();
            case EditorSetting.RECORDER_KEY_SOUND_REMOVE -> recorderManager.removeSound();
            case EditorSetting.RECORDER_KEY_SOUND_REMOVE_ALL -> recorderManager.removeEverySound();
            case EditorSetting.RECORDER_KEY_TRANSITION_LENGTH -> openTransitionLength(window);
            case EditorSetting.RECORDER_KEY_TRANSITION_REPLACE -> recorderManager.replaceTransition();
            case EditorSetting.RECORDER_KEY_TRANSITION_REMOVE -> recorderManager.removeTransition();
            case EditorSetting.RECORDER_KEY_FILTER_APPLY -> recorderManager.applyFilter();
            case EditorSetting.RECORDER_KEY_FILTER_REMOVE -> recorderManager.removeFilter();
            case EditorSetting.RECORDER_KEY_EXPORT_MIDI -> recorderManager.exportMidi();
            default -> throwException("Unknown recorder setting: " + settingKey);
        }
    }

    private RecorderClipEdge resolveEdge(String settingKey) {

        RecorderClipEdge edge = RecorderClipEdge.findByKey(
                settingKey.substring(EditorSetting.RECORDER_KEY_TRANSITION_PREFIX.length()));

        if (edge == null)
            throwException("Unknown recorder clip edge: " + settingKey);

        return edge;
    }

}
