package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.recorderpipeline.pianoroll.PianoRollDragKind;
import editor.bootstrap.recorderpipeline.pianorollmanager.PianoRollManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.runtime.EditorSetting;
import editor.runtime.pianoroll.PianoRollSetting;
import engine.input.Buttons;
import engine.input.Input;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PianoRollBranch extends BranchPackage {

    /*
     * Menu event handlers for the Piano Roll tab, its keys and its File and
     * Edit menu actions. A press on the grid is handed to the piano roll
     * once, where it began, as the tick and pitch under the pointer, with
     * whether Shift adds and Ctrl takes away, and every frame after as where
     * the pointer has moved to, snapped to the Snap grid unless the free snap
     * is held; letting go of the mouse ends it. The wheel over the grid
     * scrolls its pitches, with Shift moves along in time and with Ctrl
     * zooms about the pointer. A key of the keyboard clicked sounds its
     * pitch, and a press on the ruler moves the playhead there. Musical
     * Typing is turned on and off, and its octave and velocity stepped, by
     * their keys; the Notes actions run by the key their shortcut and the
     * Tools panel share; the Properties rows step, cycle and take typed
     * values. Every modifier is a held shortcut resolved once from its name.
     */

    // Internal
    private PianoRollManager pianoRollManager;
    private RecorderManager recorderManager;
    private NameDialogBranch nameDialogBranch;
    private InputManager inputManager;
    private WindowManager windowManager;
    private ShortcutManager shortcutManager;

    // Shortcuts
    private int extendSelectionShortcutID;
    private int subtractSelectionShortcutID;
    private int freeSnapShortcutID;
    private int scrollTimeShortcutID;
    private int zoomTimeShortcutID;

    // Base \\

    @Override
    protected void get() {
        this.pianoRollManager = get(PianoRollManager.class);
        this.recorderManager = get(RecorderManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.inputManager = get(InputManager.class);
        this.windowManager = get(WindowManager.class);
        this.shortcutManager = get(ShortcutManager.class);
    }

    @Override
    protected void start() {

        // Shortcuts
        this.extendSelectionShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                EditorSetting.PIANO_ROLL_SHORTCUT_EXTEND_SELECTION);
        this.subtractSelectionShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                EditorSetting.PIANO_ROLL_SHORTCUT_SUBTRACT_SELECTION);
        this.freeSnapShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                EditorSetting.PIANO_ROLL_SHORTCUT_FREE_SNAP);
        this.scrollTimeShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                EditorSetting.PIANO_ROLL_SHORTCUT_SCROLL_TIME);
        this.zoomTimeShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                EditorSetting.PIANO_ROLL_SHORTCUT_ZOOM_TIME);
    }

    @Override
    protected void update() {
        endDragOnRelease();
        scrollGrid();
    }

    // A drag ends the frame the mouse is let go, or once the window it began in has gone
    private void endDragOnRelease() {

        WindowInstance window = pianoRollManager.getDragWindow();

        if (pianoRollManager.getDragKind() == PianoRollDragKind.NONE || window == null)
            return;

        if (!windowManager.getWindows().contains(window)) {
            pianoRollManager.cancelDrag();
            return;
        }

        if (!inputManager.getRawInput(window).isMouseDown(Buttons.LEFT))
            pianoRollManager.endGridDrag();
    }

    // The wheel over the grid or the keyboard scrolls the pitches, with Shift moves along in time, with Ctrl zooms
    // about the pointer
    private void scrollGrid() {

        WindowInstance window = windowManager.getHoveredWindow();
        MenuInstance menu = window != null ? findMenu(window) : null;

        if (menu == null)
            return;

        Input rawInput = inputManager.getRawInput(window);
        float wheel = rawInput.getScrollY();
        ElementInstance grid = menu.getEntryPoint(PianoRollSetting.ENTRY_GRID);
        ElementInstance keys = menu.getEntryPoint(PianoRollSetting.ENTRY_KEYS);

        if (wheel == 0f || !isUnderCursor(window, grid) && !isUnderCursor(window, keys))
            return;

        if (shortcutManager.isHeld(zoomTimeShortcutID, window))
            pianoRollManager.zoomTime(resolveTick(window, grid),
                    (float) Math.pow(EditorSetting.PIANO_ROLL_ZOOM_STEP, -wheel));
        else if (shortcutManager.isHeld(scrollTimeShortcutID, window))
            pianoRollManager.scrollTime(-wheel * EditorSetting.PIANO_ROLL_SCROLL_SHARE);
        else
            pianoRollManager.scrollPitch(Math.round(wheel) * EditorSetting.PIANO_ROLL_SCROLL_PITCHES);
    }

    // Grid \\

    // What a press on the grid takes hold of is settled once, where it began, and carried until let go
    public void dragGrid(WindowInstance window, ElementInstance grid) {

        if (grid.getComputedW() <= 0f || grid.getComputedH() <= 0f)
            return;

        int tick = resolveTick(window, grid);
        int pitch = resolvePitch(window, grid);
        float x = inputManager.getHoverMouseX(window);
        float y = inputManager.getHoverMouseY(window);

        if (pianoRollManager.getDragKind() == PianoRollDragKind.NONE)
            pianoRollManager.pressGrid(
                    tick,
                    pitch,
                    Math.round(EditorSetting.PIANO_ROLL_RESIZE_GRAB_PIXELS * resolveTicksPerPixel(grid)),
                    shortcutManager.isHeld(extendSelectionShortcutID, window),
                    shortcutManager.isHeld(subtractSelectionShortcutID, window),
                    window,
                    x,
                    y);

        pianoRollManager.dragGridTo(tick, pitch, x, y, !shortcutManager.isHeld(freeSnapShortcutID, window));
    }

    // The playhead moved to the moment of the song under the pointer on the ruler
    public void scrub(WindowInstance window, ElementInstance track) {

        if (pianoRollManager.getClip() == null || track.getComputedW() <= 0f)
            return;

        int tick = resolveTick(window, track) + pianoRollManager.getOriginTick();

        recorderManager.seek((float) recorderManager.getRecording().getTempo().toSeconds(tick));
    }

    // A key of the keyboard sounds its pitch
    public void playKey(String pitchKey) {
        pianoRollManager.previewPitch(Integer.parseInt(pitchKey));
    }

    // The clip's own tick under the pointer, as the view shows it
    private int resolveTick(WindowInstance window, ElementInstance element) {

        float fraction = Math.clamp(
                (inputManager.getHoverMouseX(window) - element.getComputedLeft()) / element.getComputedW(), 0f, 1f);

        return pianoRollManager.getViewStartTick() + Math.round(fraction * pianoRollManager.getViewSpanTicks());
    }

    // The pitch of the row under the pointer, the highest drawn at the top
    private int resolvePitch(WindowInstance window, ElementInstance grid) {

        float depth = grid.getComputedTop() + grid.getComputedH() - inputManager.getHoverMouseY(window);
        int row = (int) Math.floor(depth / PianoRollSetting.ROW_PIXELS);

        return pianoRollManager.getLowPitch() + pianoRollManager.getVisibleRows() - 1 - row;
    }

    private float resolveTicksPerPixel(ElementInstance grid) {
        return pianoRollManager.getViewSpanTicks() / grid.getComputedW();
    }

    private boolean isUnderCursor(WindowInstance window, ElementInstance element) {

        if (element == null)
            return false;

        float x = inputManager.getHoverMouseX(window);
        float y = inputManager.getHoverMouseY(window);

        return x >= element.getComputedLeft() && x <= element.getComputedLeft() + element.getComputedW()
                && y >= element.getComputedTop() && y <= element.getComputedTop() + element.getComputedH();
    }

    // The Piano Roll menu open in the window, null when it shows none
    private MenuInstance findMenu(WindowInstance window) {

        ObjectArrayList<MenuInstance> menus = window.getMenuListHandle().getMenus();

        for (int i = 0; i < menus.size(); i++)
            if (menus.get(i).getMenuData().getName().equals(EditorSetting.PIANO_ROLL_MENU))
                return menus.get(i);

        return null;
    }

    // Musical Typing \\

    public void toggleTyping(WindowInstance window) {
        pianoRollManager.toggleTyping(window);
    }

    public void octaveDown() {
        pianoRollManager.stepTypingOctave(-1);
    }

    public void octaveUp() {
        pianoRollManager.stepTypingOctave(1);
    }

    public void velocityDown() {
        pianoRollManager.stepTypingVelocity(-1);
    }

    public void velocityUp() {
        pianoRollManager.stepTypingVelocity(1);
    }

    // Notes \\

    public void act(String actionKey) {
        pianoRollManager.act(actionKey);
    }

    public void deleteSelection() {
        pianoRollManager.deleteSelection();
    }

    public void fitView() {
        pianoRollManager.fitView();
    }

    // File and Edit Menus \\

    public void saveRecording() {
        recorderManager.saveRecording();
    }

    public void undo() {
        pianoRollManager.undo();
    }

    public void redo() {
        pianoRollManager.redo();
    }

    public void togglePlayback() {
        recorderManager.togglePlayback();
    }

    public void record() {
        recorderManager.record();
    }

    public void stop() {
        recorderManager.stop();
    }

    // Rows \\

    public void decreaseRow(String key) {
        pianoRollManager.stepRow(key, -1);
    }

    public void increaseRow(String key) {
        pianoRollManager.stepRow(key, 1);
    }

    public void previousRow(String key) {
        pianoRollManager.stepRow(key, -1);
    }

    public void nextRow(String key) {
        pianoRollManager.stepRow(key, 1);
    }

    public void editRow(String key, WindowInstance window) {
        nameDialogBranch.openText(
                window,
                EditorSetting.DIALOG_TITLE_EDIT_VALUE,
                pianoRollManager.getRowValueText(key),
                text -> pianoRollManager.isRowValueValid(key, text),
                text -> pianoRollManager.setRowValue(key, text));
    }
}
