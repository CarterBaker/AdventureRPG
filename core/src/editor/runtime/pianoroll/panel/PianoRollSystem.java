package editor.runtime.pianoroll.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.musicpipeline.song.SongClipStruct;
import application.bootstrap.musicpipeline.util.NoteUtility;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.recorderpipeline.pianoroll.PianoRollDragKind;
import editor.bootstrap.recorderpipeline.pianorollmanager.PianoRollManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.pianoroll.PianoRollSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class PianoRollSystem extends SystemPackage {

    /*
     * Binds the Piano Roll tab to a UI render target, opens its menu and
     * draws it: whenever the piano roll, the recorder or the view moves it
     * draws again the rows of the pitches in view with the keyboard beside
     * them, a key's name on every C, the clip's span, a line on every beat
     * the grid has room for and a stronger one on every bar of the song, the
     * ruler's bar numbers, and the clip's notes in view, the ones selected
     * drawn selected — while notes are dragged, where they would land, a
     * note being drawn from its press to the pointer, a box being drawn
     * across the notes it would select. Both playheads follow the playhead
     * every frame from positions built once, hidden while it is out of view,
     * so playing allocates nothing. Every frame it hands Musical Typing the
     * tab's window and whether it is the active one; the header names the
     * clip and its instrument and says what Musical Typing plays. A resized
     * tab is measured again a few times a second.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private ActiveWindowManager activeWindowManager;
    private PianoRollManager pianoRollManager;
    private RecorderManager recorderManager;

    // Menus
    private MenuInstance menu;
    private ElementInstance statusLabel;
    private ElementInstance titleLabel;
    private ElementInstance typingLabel;
    private ElementInstance grid;
    private ElementInstance playhead;
    private ElementInstance rulerPlayhead;
    private ElementInstance box;

    // Positions — across the grid by steps, a pitch's row down it, and a note's bar by its width in steps, built the
    // first time each is needed
    private DimensionVector2Struct[] stepPositions;
    private DimensionVector2Struct[] rowPositions;
    private DimensionVector2Struct[][] notePositions;
    private DimensionVector2Struct[] noteSizes;
    private DimensionVector2Struct[] spanSizes;
    private DimensionVector2Struct playheadSize;
    private DimensionVector2Struct rulerPlayheadSize;
    private DimensionVector2Struct hiddenSize;
    private int shownPlayheadStep;
    private boolean playheadShown;

    // State
    private int shownRevision;
    private int shownViewRevision;
    private int shownRecorderRevision;
    private float shownGridWidth;
    private float shownGridHeight;
    private float refreshElapsed;

    // Base \\

    @Override
    protected void create() {

        int steps = PianoRollSetting.GRID_STEPS;

        // Positions
        this.stepPositions = new DimensionVector2Struct[steps + 1];

        for (int i = 0; i <= steps; i++)
            stepPositions[i] = new DimensionVector2Struct(
                    DimensionValueStruct.ofPercent(toPercent(i)),
                    DimensionValueStruct.ofAbsolute(0f));

        this.rowPositions = new DimensionVector2Struct[PianoRollSetting.ROWS_MAX];

        for (int i = 0; i < rowPositions.length; i++)
            rowPositions[i] = new DimensionVector2Struct(
                    DimensionValueStruct.ofAbsolute(0f),
                    DimensionValueStruct.ofAbsolute(-i * PianoRollSetting.ROW_PIXELS));

        this.notePositions = new DimensionVector2Struct[PianoRollSetting.ROWS_MAX][steps + 1];
        this.noteSizes = new DimensionVector2Struct[steps + 1];
        this.spanSizes = new DimensionVector2Struct[steps + 1];
        this.playheadSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(PianoRollSetting.PLAYHEAD_PIXELS),
                DimensionValueStruct.ofPercent(PianoRollSetting.GRID_PERCENT));
        this.rulerPlayheadSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(PianoRollSetting.PLAYHEAD_PIXELS),
                DimensionValueStruct.ofAbsolute(PianoRollSetting.RULER_PLAYHEAD_PIXELS));
        this.hiddenSize = new DimensionVector2Struct(
                DimensionValueStruct.ofAbsolute(0f),
                DimensionValueStruct.ofAbsolute(0f));
        this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;
        this.playheadShown = true;

        // State
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownViewRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownRecorderRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.activeWindowManager = get(ActiveWindowManager.class);
        this.pianoRollManager = get(PianoRollManager.class);
        this.recorderManager = get(RecorderManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.menu = menuManager.openMenu(EditorSetting.PIANO_ROLL_MENU, window);

        this.statusLabel = requireEntryPoint(PianoRollSetting.ENTRY_STATUS);
        this.titleLabel = requireEntryPoint(PianoRollSetting.ENTRY_TITLE);
        this.typingLabel = requireEntryPoint(PianoRollSetting.ENTRY_TYPING);
        this.grid = requireEntryPoint(PianoRollSetting.ENTRY_GRID);
        this.playhead = requireEntryPoint(PianoRollSetting.ENTRY_PLAYHEAD);
        this.rulerPlayhead = requireEntryPoint(PianoRollSetting.ENTRY_RULER_PLAYHEAD);
        this.box = requireEntryPoint(PianoRollSetting.ENTRY_BOX);

        requireEntryPoint(PianoRollSetting.ENTRY_KEYS);
        requireEntryPoint(PianoRollSetting.ENTRY_RULER);
        requireEntryPoint(PianoRollSetting.ENTRY_GRID_ROWS);
        requireEntryPoint(PianoRollSetting.ENTRY_GRID_LINES);
        requireEntryPoint(PianoRollSetting.ENTRY_GRID_NOTES);
        requireEntryPoint(PianoRollSetting.ENTRY_RULER_TRACK);
    }

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = menu.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Piano Roll menu '" + EditorSetting.PIANO_ROLL_MENU + "' has no entry point " + entryPoint
                    + ".");

        return element;
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        pianoRollManager.updateTyping(window, activeWindowManager.isActive(window));

        if (pianoRollManager.getRevision() != shownRevision
                || pianoRollManager.getViewRevision() != shownViewRevision
                || recorderManager.getRevision() != shownRecorderRevision)
            refresh();

        this.refreshElapsed += internal.getDeltaTime();

        if (refreshElapsed >= PianoRollSetting.REFRESH_SECONDS) {
            this.refreshElapsed = 0f;

            if (grid.getComputedW() != shownGridWidth || grid.getComputedH() != shownGridHeight)
                refresh();
        }

        movePlayhead();
    }

    // Draw \\

    private void refresh() {

        this.shownGridWidth = grid.getComputedW();
        this.shownGridHeight = grid.getComputedH();
        pianoRollManager.setVisibleRows(Math.clamp(
                (int) (shownGridHeight / PianoRollSetting.ROW_PIXELS), 1, PianoRollSetting.ROWS_MAX));

        this.shownRevision = pianoRollManager.getRevision();
        this.shownViewRevision = pianoRollManager.getViewRevision();
        this.shownRecorderRevision = recorderManager.getRevision();
        this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;

        menuManager.ejectAll(menu, PianoRollSetting.ENTRY_KEYS);
        menuManager.ejectAll(menu, PianoRollSetting.ENTRY_GRID_ROWS);
        menuManager.ejectAll(menu, PianoRollSetting.ENTRY_GRID_LINES);
        menuManager.ejectAll(menu, PianoRollSetting.ENTRY_GRID_NOTES);
        menuManager.ejectAll(menu, PianoRollSetting.ENTRY_RULER);

        statusLabel.setFontText(pianoRollManager.getStatusText());
        typingLabel.setFontText(pianoRollManager.describeTyping());
        injectRows();

        RecordingClipStruct clip = pianoRollManager.getClip();

        box.setSizeOverride(hiddenSize);

        if (clip == null) {
            titleLabel.setFontText(PianoRollSetting.TEXT_TITLE);
            return;
        }

        titleLabel.setFontText(String.format(PianoRollSetting.FORMAT_TITLE,
                CharacterViewUtility.toResourceLabel(clip.getSourceName()),
                recorderManager.getRecording().getRecordingName()));
        injectClipSpan();
        injectLines(recorderManager.getRecording());
        injectNotes();
        injectDraft();
    }

    // A row for every pitch in view with its key beside it, the highest at the top, a key's name on every C
    private void injectRows() {

        int rows = pianoRollManager.getVisibleRows();
        int lowPitch = pianoRollManager.getLowPitch();

        for (int row = 0; row < rows; row++) {

            int pitch = lowPitch + rows - 1 - row;

            if (pitch > EngineSetting.NOTE_PITCH_MAX)
                continue;

            boolean black = isBlack(pitch);
            DimensionVector2Struct position = rowPositions[row];
            String pitchKey = Integer.toString(pitch);

            menuManager.inject(menu, PianoRollSetting.ENTRY_GRID_ROWS,
                    black ? PianoRollSetting.TEMPLATE_ROW_BLACK : PianoRollSetting.TEMPLATE_ROW_WHITE,
                    injected -> injected.setPositionOverride(position));

            ElementInstance key = menuManager.inject(menu, PianoRollSetting.ENTRY_KEYS,
                    black ? PianoRollSetting.TEMPLATE_KEY_BLACK : PianoRollSetting.TEMPLATE_KEY_WHITE,
                    injected -> {
                        injected.setPositionOverride(position);
                        injected.setActionArgOverride(pitchKey);
                    });

            key.findChildById(PianoRollSetting.ELEMENT_KEY_LABEL).setFontText(
                    Math.floorMod(pitch, EngineSetting.NOTE_SEMITONES_PER_OCTAVE) == 0
                            ? NoteUtility.toNoteName(pitch)
                            : PianoRollSetting.TEXT_EMPTY);
        }
    }

    private boolean isBlack(int pitch) {
        return EngineSetting.NOTE_NAMES[Math.floorMod(pitch, EngineSetting.NOTE_SEMITONES_PER_OCTAVE)].length() > 1;
    }

    // The stretch of the grid the clip plays, shaded behind its notes
    private void injectClipSpan() {

        int from = toStep(pianoRollManager.getClipStartTick());
        int width = toStep(pianoRollManager.getClipEndTick()) - from;

        if (width <= 0)
            return;

        DimensionVector2Struct position = stepPositions[from];
        DimensionVector2Struct size = resolveSpanSize(width);

        menuManager.inject(menu, PianoRollSetting.ENTRY_GRID_LINES, PianoRollSetting.TEMPLATE_CLIP_SPAN,
                injected -> {
                    injected.setPositionOverride(position);
                    injected.setSizeOverride(size);
                });
    }

    // A line on every beat of the song in view while they stand far enough apart, a stronger one on every bar, and
    // the ruler's bar numbers as far apart as they read
    private void injectLines(RecordingInstance recording) {

        int origin = pianoRollManager.getOriginTick();
        int start = pianoRollManager.getViewStartTick();
        int span = pianoRollManager.getViewSpanTicks();
        int beatTicks = recording.getBeatTicks();
        int barTicks = recording.getBarTicks();
        float pixelsPerTick = shownGridWidth / span;
        boolean beats = beatTicks * pixelsPerTick >= PianoRollSetting.BEAT_LINE_MIN_PIXELS;
        int labelBars = Math.max(1, (int) Math.ceil(PianoRollSetting.RULER_MIN_PIXELS / (barTicks * pixelsPerTick)));
        int step = beats ? beatTicks : barTicks;

        for (long absolute = Math.floorDiv(start + origin, step) * (long) step;
                absolute - origin <= start + span; absolute += step) {

            long tick = absolute - origin;

            if (tick < start)
                continue;

            boolean bar = absolute % barTicks == 0;
            DimensionVector2Struct position = stepPositions[toStep((int) tick)];

            menuManager.inject(menu, PianoRollSetting.ENTRY_GRID_LINES,
                    bar ? PianoRollSetting.TEMPLATE_LINE_BAR : PianoRollSetting.TEMPLATE_LINE_BEAT,
                    injected -> injected.setPositionOverride(position));

            if (!bar || Math.floorDiv(absolute, barTicks) % labelBars != 0)
                continue;

            menuManager.inject(menu, PianoRollSetting.ENTRY_RULER, PianoRollSetting.TEMPLATE_RULER_TICK,
                    injected -> injected.setPositionOverride(position))
                    .findChildById(PianoRollSetting.ELEMENT_RULER_LABEL)
                    .setFontText(String.format(PianoRollSetting.FORMAT_BAR, Math.floorDiv(absolute, barTicks) + 1));
        }
    }

    // Every note of the clip in view on a row in view; while notes are dragged, the ones selected where they would
    // land
    private void injectNotes() {

        SongClipStruct notes = pianoRollManager.getNotes();
        PianoRollDragKind drag = pianoRollManager.isDragMoved()
                ? pianoRollManager.getDragKind()
                : PianoRollDragKind.NONE;

        for (int note = 0; note < notes.getNoteCount(); note++) {

            boolean selected = pianoRollManager.isSelected(note);
            int start = notes.getNoteStart(note);
            int length = notes.getNoteLength(note);
            int pitch = notes.getNotePitch(note);

            if (selected && drag == PianoRollDragKind.MOVE) {
                start = Math.max(0, start + pianoRollManager.getDragTicks());
                pitch += pianoRollManager.getDragPitches();
            } else if (selected && drag == PianoRollDragKind.RESIZE)
                length = Math.max(1, length + pianoRollManager.getDragTicks());

            injectNote(start, length, pitch, selected
                    ? PianoRollSetting.TEMPLATE_NOTE_SELECTED
                    : PianoRollSetting.TEMPLATE_NOTE);
        }
    }

    // A note being drawn from its press to the pointer, or a box across the notes it would select
    private void injectDraft() {

        if (!pianoRollManager.isDragMoved())
            return;

        int fromTick = Math.min(pianoRollManager.getPressTick(), pianoRollManager.getReachTick());
        int toTick = Math.max(pianoRollManager.getPressTick(), pianoRollManager.getReachTick());

        if (pianoRollManager.getDragKind() == PianoRollDragKind.DRAW)
            injectNote(pianoRollManager.snapTick(pianoRollManager.getPressTick()),
                    Math.max(pianoRollManager.getSnapTicks(),
                            pianoRollManager.snapTick(toTick) - pianoRollManager.snapTick(fromTick)),
                    pianoRollManager.lockPitch(pianoRollManager.getPressPitch(), 0),
                    PianoRollSetting.TEMPLATE_NOTE_GHOST);

        if (pianoRollManager.getDragKind() != PianoRollDragKind.BOX)
            return;

        int top = toRow(Math.max(pianoRollManager.getPressPitch(), pianoRollManager.getReachPitch()));
        int rows = Math.abs(pianoRollManager.getPressPitch() - pianoRollManager.getReachPitch()) + 1;
        int from = toStep(fromTick);

        box.setPositionOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(toPercent(from)),
                DimensionValueStruct.ofAbsolute(-top * PianoRollSetting.ROW_PIXELS)));
        box.setSizeOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(toPercent(Math.max(1, toStep(toTick) - from))),
                DimensionValueStruct.ofAbsolute(rows * PianoRollSetting.ROW_PIXELS)));
    }

    // A note's bar on its row across the part of it in view, left out when it lies wholly outside the view
    private void injectNote(int start, int length, int pitch, String template) {

        int row = toRow(pitch);
        int viewStart = pianoRollManager.getViewStartTick();
        int viewEnd = viewStart + pianoRollManager.getViewSpanTicks();

        if (row < 0 || row >= pianoRollManager.getVisibleRows() || start + length <= viewStart || start >= viewEnd)
            return;

        int from = toStep(start);
        int width = Math.max(1, toStep(start + length) - from);
        DimensionVector2Struct position = resolveNotePosition(row, from);
        DimensionVector2Struct size = resolveNoteSize(width);

        menuManager.inject(menu, PianoRollSetting.ENTRY_GRID_NOTES, template, injected -> {
            injected.setPositionOverride(position);
            injected.setSizeOverride(size);
        });
    }

    // Playhead \\

    // Both playheads at the playhead, hidden while it lies out of view or no clip is shown
    private void movePlayhead() {

        boolean shown = pianoRollManager.getClip() != null && recorderManager.hasRecording();
        int step = EngineSetting.INDEX_NOT_FOUND;

        if (shown) {

            double tick = pianoRollManager.getPlayheadTick();
            int start = pianoRollManager.getViewStartTick();

            shown = tick >= start && tick <= start + pianoRollManager.getViewSpanTicks();
            step = shown ? toStep((int) Math.round(tick)) : step;
        }

        if (shown != playheadShown) {
            this.playheadShown = shown;
            this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;
            playhead.setSizeOverride(shown ? playheadSize : hiddenSize);
            rulerPlayhead.setSizeOverride(shown ? rulerPlayheadSize : hiddenSize);
        }

        if (!shown || step == shownPlayheadStep)
            return;

        this.shownPlayheadStep = step;
        playhead.setPositionOverride(stepPositions[step]);
        rulerPlayhead.setPositionOverride(stepPositions[step]);
    }

    // Utility \\

    // How far across the grid a tick lies, in steps kept within it
    private int toStep(int tick) {

        float fraction = (tick - pianoRollManager.getViewStartTick()) / (float) pianoRollManager.getViewSpanTicks();

        return Math.clamp(Math.round(fraction * PianoRollSetting.GRID_STEPS), 0, PianoRollSetting.GRID_STEPS);
    }

    // The row a pitch is drawn on, counted down from the top of the grid
    private int toRow(int pitch) {
        return pianoRollManager.getLowPitch() + pianoRollManager.getVisibleRows() - 1 - pitch;
    }

    private float toPercent(int step) {
        return PianoRollSetting.GRID_PERCENT * step / PianoRollSetting.GRID_STEPS;
    }

    private DimensionVector2Struct resolveNotePosition(int row, int step) {

        DimensionVector2Struct position = notePositions[row][step];

        if (position != null)
            return position;

        position = new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(toPercent(step)),
                DimensionValueStruct.ofAbsolute(-(row * PianoRollSetting.ROW_PIXELS + 1f)));
        notePositions[row][step] = position;
        return position;
    }

    private DimensionVector2Struct resolveNoteSize(int steps) {

        DimensionVector2Struct size = noteSizes[steps];

        if (size != null)
            return size;

        size = new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(toPercent(steps)),
                DimensionValueStruct.ofAbsolute(PianoRollSetting.NOTE_PIXELS));
        noteSizes[steps] = size;
        return size;
    }

    private DimensionVector2Struct resolveSpanSize(int steps) {

        DimensionVector2Struct size = spanSizes[steps];

        if (size != null)
            return size;

        size = new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(toPercent(steps)),
                DimensionValueStruct.ofPercent(PianoRollSetting.GRID_PERCENT));
        spanSizes[steps] = size;
        return size;
    }
}
