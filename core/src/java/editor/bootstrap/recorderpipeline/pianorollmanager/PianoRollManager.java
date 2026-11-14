package editor.bootstrap.recorderpipeline.pianorollmanager;

import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.song.SongClipStruct;
import application.bootstrap.musicpipeline.util.NoteUtility;
import application.bootstrap.musicpipeline.samplermanager.SamplerManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.recorderpipeline.pianoroll.PianoRollDragKind;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PianoRollManager extends ManagerPackage {

    /*
     * Owns the Piano Roll: the notes of the notes clip selected on the
     * Timeline, which every tab shares through RecorderManager, the notes
     * selected among them, the view of pitches and ticks, the press on its
     * grid and what it takes hold of, and Musical Typing. Every edit — a note
     * drawn, erased, cut, moved, lengthened, typed in or changed by a Notes
     * action or a Properties row — builds the clip's notes anew, put in order
     * with the selection carried along, and replaces the clip's codec clip
     * through RecorderManager as one edit kept for undo; the clip's own
     * history is stepped back and forth through the File and Edit menus.
     * Positions are the clip's own ticks from where its notes begin, and the
     * Snap grid is laid on the song's ticks, so a clip laid off the beat
     * still snaps to the beat. With Scale Lock on, every note drawn, moved,
     * transposed or typed keeps to the song's key. A key clicked on the
     * keyboard sounds its pitch for a moment through the sampler.
     */

    // Internal
    private RecorderManager recorderManager;
    private ToolManager toolManager;
    private SamplerManager samplerManager;
    private InstrumentManager instrumentManager;
    private PianoRollHistoryBranch pianoRollHistoryBranch;
    private PianoRollTypingBranch pianoRollTypingBranch;

    // Clip — the notes clip shown, as found on the Timeline last frame
    private RecordingClipStruct shownClip;

    // Selection — the notes selected by their place in the clip's notes, the one picked last at the end
    private IntArrayList selectedNotes;

    // View — the lowest pitch drawn, how many rows the grid holds, and the ticks across it
    private int lowPitch;
    private int visibleRows;
    private int viewStartTick;
    private int viewSpanTicks;
    private int viewRevision;

    // Drag
    private PianoRollDragKind dragKind;
    private WindowInstance dragWindow;
    private int pressNote;
    private int pressTick;
    private int pressPitch;
    private int reachTick;
    private int reachPitch;
    private int dragTicks;
    private int dragPitches;
    private float pressX;
    private float pressY;
    private boolean moved;
    private boolean extending;
    private boolean subtracting;

    // Preview — the note a clicked key sounds, and how long it has left
    private int previewHandle;
    private float previewLeft;

    // Scratch — notes as records of start, length, pitch, velocity and whether each is selected, and one record held
    // while they are put in order
    private IntArrayList records;
    private int[] heldRecord;

    // Status
    private int revision;
    private String statusText;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.pianoRollHistoryBranch = create(PianoRollHistoryBranch.class);
        this.pianoRollTypingBranch = create(PianoRollTypingBranch.class);

        // Selection
        this.selectedNotes = new IntArrayList();

        // View
        this.lowPitch = EditorSetting.PIANO_ROLL_DEFAULT_LOW_PITCH;
        this.visibleRows = 1;
        this.viewSpanTicks = EngineSetting.SONG_TICKS_PER_BEAT * EditorSetting.RECORDER_QUARTERS_PER_WHOLE;

        // Drag
        this.dragKind = PianoRollDragKind.NONE;

        // Preview
        this.previewHandle = EngineSetting.INDEX_NOT_FOUND;

        // Scratch
        this.records = new IntArrayList();
        this.heldRecord = new int[EditorSetting.PIANO_ROLL_RECORD_STRIDE];

        // Status
        this.statusText = EditorSetting.PIANO_ROLL_STATUS_READY;
    }

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.toolManager = get(ToolManager.class);
        this.samplerManager = get(SamplerManager.class);
        this.instrumentManager = get(InstrumentManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        RecordingClipStruct clip = recorderManager.findNotesClip();

        if (clip != shownClip) {
            this.shownClip = clip;
            selectedNotes.clear();
            cancelDrag();
            fitView();
            revision++;
        }

        updatePreview();
    }

    // A key's note let go once it has sounded its moment
    private void updatePreview() {

        if (previewHandle == EngineSetting.INDEX_NOT_FOUND)
            return;

        this.previewLeft -= internal.getDeltaTime();

        if (previewLeft > 0f)
            return;

        samplerManager.noteOff(previewHandle);
        this.previewHandle = EngineSetting.INDEX_NOT_FOUND;
    }

    // Clip \\

    // The notes clip shown, null while the Timeline's selection is no notes clip
    public RecordingClipStruct getClip() {
        return shownClip;
    }

    public SongClipStruct getNotes() {
        return shownClip != null ? shownClip.getSongClip() : null;
    }

    // The tick of the song the clip's own first tick lies on, so the Snap grid falls on the song's beats
    public int getOriginTick() {
        return recorderManager.toTick(shownClip.getOrigin());
    }

    // The clip's own tick its span starts at
    public int getClipStartTick() {
        return (int) Math.round(shownClip.toSongTick(shownClip.getInPoint()));
    }

    // The clip's own tick its span ends at
    public int getClipEndTick() {
        return (int) Math.round(shownClip.toSongTick(shownClip.getOutPoint()));
    }

    // The clip's own tick the playhead stands at
    public double getPlayheadTick() {
        return shownClip.toSongTick(shownClip.toChildTime(recorderManager.getPlayhead()));
    }

    // The instrument the clip plays, the default instrument while none is shown
    public short resolveInstrumentID() {

        SongClipStruct notes = getNotes();

        return notes != null && notes.getInstrumentID() != EngineSetting.INSTRUMENT_NONE
                ? notes.getInstrumentID()
                : instrumentManager.getInstrumentIDFromInstrumentName(EngineSetting.SONG_DEFAULT_INSTRUMENT);
    }

    // Grid \\

    public int getSnapTicks() {
        return EditorSetting.PIANO_ROLL_GRID_TICKS[Math.round(getOption(ToolOption.SNAP))];
    }

    public int getNoteLengthTicks() {
        return EditorSetting.PIANO_ROLL_GRID_TICKS[Math.round(getOption(ToolOption.NOTE_LENGTH))];
    }

    private int getVelocity() {
        return Math.round(getOption(ToolOption.VELOCITY));
    }

    private boolean isScaleLocked() {
        return Math.round(getOption(ToolOption.SCALE_LOCK)) == 1;
    }

    private float getOption(ToolOption option) {
        return toolManager.getOption(ToolCanvas.PIANO_ROLL, option);
    }

    public EditorTool getTool() {
        return toolManager.getTool(ToolCanvas.PIANO_ROLL);
    }

    // The clip's own tick on the Snap grid nearest the tick, the grid laid on the song's ticks
    public int snapTick(int tick) {

        int snap = getSnapTicks();
        int origin = getOriginTick();

        return Math.round((tick + origin) / (float) snap) * snap - origin;
    }

    // The pitch kept to the song's key while Scale Lock is on, moved the way given when it is off the scale
    public int lockPitch(int pitch, int direction) {

        int held = Math.clamp(pitch, EngineSetting.NOTE_PITCH_MIN, EngineSetting.NOTE_PITCH_MAX);
        RecordingInstance recording = recorderManager.getRecording();

        if (!isScaleLocked() || recording == null)
            return held;

        if (direction == 0)
            return NoteUtility.snapToScale(held, recording.getKeyRoot(), recording.getKeyScale());

        while (!NoteUtility.isInScale(held, recording.getKeyRoot(), recording.getKeyScale())
                && held + direction >= EngineSetting.NOTE_PITCH_MIN && held + direction <= EngineSetting.NOTE_PITCH_MAX)
            held += direction;

        return held;
    }

    // View \\

    // The view across the clip's span, from a little before it to a little after, and its notes' pitches centred
    public void fitView() {

        if (shownClip == null)
            return;

        int start = getClipStartTick();
        int span = Math.max(getClipEndTick() - start, EditorSetting.PIANO_ROLL_MIN_SPAN_TICKS);

        this.viewStartTick = start;
        this.viewSpanTicks = Math.clamp(span, EditorSetting.PIANO_ROLL_MIN_SPAN_TICKS,
                EditorSetting.PIANO_ROLL_MAX_SPAN_TICKS);

        SongClipStruct notes = getNotes();

        if (notes.getNoteCount() > 0) {

            int low = EngineSetting.NOTE_PITCH_MAX;
            int high = EngineSetting.NOTE_PITCH_MIN;

            for (int note = 0; note < notes.getNoteCount(); note++) {
                low = Math.min(low, notes.getNotePitch(note));
                high = Math.max(high, notes.getNotePitch(note));
            }

            scrollPitchTo((low + high - visibleRows) / 2);
        }

        viewRevision++;
    }

    // The grid holds this many rows of pitches, as the tab is now sized
    public void setVisibleRows(int rows) {

        if (rows == visibleRows || rows < 1)
            return;

        this.visibleRows = rows;
        scrollPitchTo(lowPitch);
        viewRevision++;
    }

    public void scrollPitch(int pitches) {
        scrollPitchTo(lowPitch + pitches);
        viewRevision++;
    }

    private void scrollPitchTo(int pitch) {
        this.lowPitch = Math.clamp(pitch, EngineSetting.NOTE_PITCH_MIN,
                Math.max(EngineSetting.NOTE_PITCH_MIN, EngineSetting.NOTE_PITCH_MAX + 1 - visibleRows));
    }

    // The view moved along by a share of its own span
    public void scrollTime(float share) {
        this.viewStartTick = Math.round(viewStartTick + share * viewSpanTicks);
        viewRevision++;
    }

    // The view zoomed by the factor about the tick, which stays where it is drawn
    public void zoomTime(int tick, float factor) {

        int span = Math.clamp(Math.round(viewSpanTicks * factor), EditorSetting.PIANO_ROLL_MIN_SPAN_TICKS,
                EditorSetting.PIANO_ROLL_MAX_SPAN_TICKS);
        float share = (tick - viewStartTick) / (float) viewSpanTicks;

        this.viewStartTick = Math.round(tick - share * span);
        this.viewSpanTicks = span;
        viewRevision++;
    }

    // Keyboard \\

    // The pitch sounded a moment, as a key of the keyboard clicked
    public void previewPitch(int pitch) {

        if (previewHandle != EngineSetting.INDEX_NOT_FOUND)
            samplerManager.noteOff(previewHandle);

        this.previewHandle = samplerManager.noteOn(resolveInstrumentID(), pitch, getVelocity());
        this.previewLeft = EditorSetting.PIANO_ROLL_KEY_PREVIEW_SECONDS;
    }

    // Grid Press \\

    // What a press on the grid takes hold of, settled where it began: with Select, a note's end to lengthen the notes
    // selected, a note to move them — with Shift or Ctrl, to add it to or take it from them should the press not
    // travel — or the empty grid to draw a box; with Draw, a new note drawn out from the press, or a note to move;
    // with Erase, every note it passes over; with Cut, the note pressed cut where it is pressed
    public void pressGrid(
            int tick,
            int pitch,
            int resizeTicks,
            boolean extendingHeld,
            boolean subtractingHeld,
            WindowInstance window,
            float x,
            float y) {

        if (shownClip == null || dragKind != PianoRollDragKind.NONE || !canEdit())
            return;

        EditorTool tool = getTool();
        int note = findNoteAt(tick, pitch);

        this.dragWindow = window;
        this.pressNote = note;
        this.pressTick = tick;
        this.pressPitch = pitch;
        this.reachTick = tick;
        this.reachPitch = pitch;
        this.dragTicks = 0;
        this.dragPitches = 0;
        this.pressX = x;
        this.pressY = y;
        this.moved = false;
        this.extending = extendingHeld;
        this.subtracting = subtractingHeld;

        if (tool == EditorTool.CUT) {
            if (note != EngineSetting.INDEX_NOT_FOUND)
                cutNote(note, snapTick(tick));
            return;
        }

        if (tool == EditorTool.ERASE) {
            this.dragKind = PianoRollDragKind.ERASE;
            eraseAt(tick, pitch);
            return;
        }

        if (note != EngineSetting.INDEX_NOT_FOUND) {
            this.dragKind = isOnEnd(note, tick, resizeTicks) ? PianoRollDragKind.RESIZE : PianoRollDragKind.MOVE;
            revision++;
            return;
        }

        this.dragKind = tool == EditorTool.DRAW ? PianoRollDragKind.DRAW : PianoRollDragKind.BOX;
        revision++;
    }

    // Where the drag would land with the pointer where it is now: the notes' shift in snapped ticks and in pitches,
    // their ends' shift, the drawn note's end or the box's far corner, the drawing redrawn only when it changes
    public void dragGridTo(int tick, int pitch, float x, float y, boolean snapping) {

        if (dragKind == PianoRollDragKind.NONE)
            return;

        if (!moved && Math.abs(x - pressX) + Math.abs(y - pressY) >= EditorSetting.PIANO_ROLL_DRAG_THRESHOLD_PIXELS)
            this.moved = true;

        if (dragKind == PianoRollDragKind.ERASE) {
            eraseAt(tick, pitch);
            return;
        }

        int ticks = tick - pressTick;
        int shiftTicks = snapping ? Math.round(ticks / (float) getSnapTicks()) * getSnapTicks() : ticks;
        int shiftPitches = dragKind == PianoRollDragKind.MOVE ? pitch - pressPitch : 0;

        if (shiftTicks == dragTicks && shiftPitches == dragPitches && tick == reachTick && pitch == reachPitch)
            return;

        this.dragTicks = shiftTicks;
        this.dragPitches = shiftPitches;
        this.reachTick = tick;
        this.reachPitch = pitch;
        revision++;
    }

    // The press let go: what it took hold of acts on what it reached, or, never travelled, selects as a click
    public void endGridDrag() {

        PianoRollDragKind kind = dragKind;

        if (kind == PianoRollDragKind.NONE)
            return;

        cancelDrag();

        switch (kind) {
            case MOVE -> {
                if (moved)
                    moveSelection(pressNote, dragTicks, dragPitches);
                else
                    clickNote(pressNote);
            }
            case RESIZE -> {
                if (moved)
                    resizeSelection(pressNote, dragTicks);
                else
                    clickNote(pressNote);
            }
            case BOX -> {
                if (moved)
                    boxSelect();
                else if (!extending && !subtracting)
                    clearSelection();
            }
            case DRAW -> drawNote(pressTick, Math.max(reachTick, pressTick), pressPitch);
            case ERASE, NONE -> {
            }
        }

        revision++;
    }

    public void cancelDrag() {
        this.dragKind = PianoRollDragKind.NONE;
        this.dragWindow = null;
    }

    // Selection \\

    // A note clicked selects it alone, with Shift joins or leaves the selection, with Ctrl leaves it
    private void clickNote(int note) {

        int at = selectedNotes.indexOf(note);

        if (subtracting) {
            if (at != EngineSetting.INDEX_NOT_FOUND)
                selectedNotes.removeInt(at);
        } else if (extending) {
            if (at != EngineSetting.INDEX_NOT_FOUND)
                selectedNotes.removeInt(at);
            else
                selectedNotes.add(note);
        } else {
            selectedNotes.clear();
            selectedNotes.add(note);
        }

        previewNote(note);
        revision++;
    }

    // The notes within the box selected: alone, added with Shift, taken away with Ctrl
    private void boxSelect() {

        SongClipStruct notes = getNotes();
        int fromTick = Math.min(pressTick, reachTick);
        int toTick = Math.max(pressTick, reachTick);
        int fromPitch = Math.min(pressPitch, reachPitch);
        int toPitch = Math.max(pressPitch, reachPitch);

        if (!extending && !subtracting)
            selectedNotes.clear();

        for (int note = 0; note < notes.getNoteCount(); note++) {

            int pitch = notes.getNotePitch(note);
            int start = notes.getNoteStart(note);
            boolean inside = pitch >= fromPitch && pitch <= toPitch
                    && start < toTick && start + notes.getNoteLength(note) > fromTick;

            if (!inside)
                continue;

            int at = selectedNotes.indexOf(note);

            if (subtracting && at != EngineSetting.INDEX_NOT_FOUND)
                selectedNotes.removeInt(at);
            else if (!subtracting && at == EngineSetting.INDEX_NOT_FOUND)
                selectedNotes.add(note);
        }
    }

    public void clearSelection() {
        selectedNotes.clear();
        revision++;
    }

    public boolean isSelected(int note) {
        return selectedNotes.contains(note);
    }

    private void previewNote(int note) {
        previewPitch(getNotes().getNotePitch(note));
    }

    // Edits \\

    // A note of the Note Length and Velocity drawn from the snapped press, lengthened to the snapped end it was drawn
    // to, on the pitch pressed kept to the key, and selected alone
    private void drawNote(int fromTick, int toTick, int pitch) {

        int snap = getSnapTicks();
        int start = Math.max(getClipStartTick(), Math.max(0, snapTick(fromTick)));
        int end = moved ? Math.max(start + snap, snapTick(toTick)) : start + getNoteLengthTicks();

        loadRecords(false);
        addRecord(start, end - start, lockPitch(pitch, 0), getVelocity(), true);
        commit(EditorSetting.PIANO_ROLL_LABEL_DRAWN);
        previewPitch(lockPitch(pitch, 0));
    }

    // A note typed with Musical Typing written into the clip while the transport records
    void addTypedNote(int start, int length, int pitch, int velocity) {

        if (shownClip == null)
            return;

        loadRecords(true);
        addRecord(Math.max(0, start), Math.max(1, length), pitch, velocity, false);
        commit(EditorSetting.PIANO_ROLL_LABEL_TYPED);
    }

    private void eraseAt(int tick, int pitch) {

        int note = findNoteAt(tick, pitch);

        if (note == EngineSetting.INDEX_NOT_FOUND)
            return;

        loadRecords(true);
        removeRecord(note);
        commit(EditorSetting.PIANO_ROLL_LABEL_ERASED);
    }

    // The note cut in two where it is pressed, snapped, when that lies inside it
    private void cutNote(int note, int tick) {

        SongClipStruct notes = getNotes();
        int start = notes.getNoteStart(note);
        int end = start + notes.getNoteLength(note);

        if (tick <= start || tick >= end)
            return;

        loadRecords(true);
        records.set(note * EditorSetting.PIANO_ROLL_RECORD_STRIDE + EditorSetting.PIANO_ROLL_RECORD_LENGTH,
                tick - start);
        addRecord(tick, end - tick, notes.getNotePitch(note), notes.getNoteVelocity(note), false);
        commit(EditorSetting.PIANO_ROLL_LABEL_CUT);
    }

    // Every note selected — the pressed one first among them when it was not — moved by the ticks and pitches
    // given, none before the clip's notes begin, each pitch kept to the key
    private void moveSelection(int pressed, int ticks, int pitches) {

        if (!selectedNotes.contains(pressed) && !subtracting) {

            if (!extending)
                selectedNotes.clear();

            selectedNotes.add(pressed);
        }

        loadRecords(true);

        int direction = Integer.signum(pitches);

        for (int at = 0; at < records.size(); at += EditorSetting.PIANO_ROLL_RECORD_STRIDE) {

            if (records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_SELECTED) == 0)
                continue;

            records.set(at + EditorSetting.PIANO_ROLL_RECORD_START,
                    Math.max(0, records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_START) + ticks));
            records.set(at + EditorSetting.PIANO_ROLL_RECORD_PITCH,
                    lockPitch(records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_PITCH) + pitches, direction));
        }

        commit(EditorSetting.PIANO_ROLL_LABEL_MOVED);
    }

    // Every note selected lengthened or shortened by the ticks given, none shorter than a tick
    private void resizeSelection(int pressed, int ticks) {

        if (!selectedNotes.contains(pressed)) {
            selectedNotes.clear();
            selectedNotes.add(pressed);
        }

        loadRecords(true);

        for (int at = 0; at < records.size(); at += EditorSetting.PIANO_ROLL_RECORD_STRIDE)
            if (records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_SELECTED) != 0)
                records.set(at + EditorSetting.PIANO_ROLL_RECORD_LENGTH,
                        Math.max(1, records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_LENGTH) + ticks));

        commit(EditorSetting.PIANO_ROLL_LABEL_RESIZED);
    }

    public void deleteSelection() {

        if (!requireSelection())
            return;

        loadRecords(true);

        for (int note = records.size() / EditorSetting.PIANO_ROLL_RECORD_STRIDE - 1; note >= 0; note--)
            if (records.getInt(note * EditorSetting.PIANO_ROLL_RECORD_STRIDE
                    + EditorSetting.PIANO_ROLL_RECORD_SELECTED) != 0)
                removeRecord(note);

        commit(EditorSetting.PIANO_ROLL_LABEL_DELETED);
    }

    // Actions \\

    // One of the Notes actions, by the key its shortcut and the Tools panel share
    public void act(String actionKey) {

        if (actionKey.equals(EditorSetting.PIANO_ROLL_KEY_SELECT_ALL)) {
            selectAll();
            return;
        }

        if (!requireSelection())
            return;

        loadRecords(true);

        switch (actionKey) {
            case EditorSetting.PIANO_ROLL_KEY_QUANTIZE -> quantize();
            case EditorSetting.PIANO_ROLL_KEY_TRANSPOSE_UP -> transpose(1);
            case EditorSetting.PIANO_ROLL_KEY_TRANSPOSE_DOWN -> transpose(-1);
            case EditorSetting.PIANO_ROLL_KEY_OCTAVE_UP -> transpose(EngineSetting.NOTE_SEMITONES_PER_OCTAVE);
            case EditorSetting.PIANO_ROLL_KEY_OCTAVE_DOWN -> transpose(-EngineSetting.NOTE_SEMITONES_PER_OCTAVE);
            case EditorSetting.PIANO_ROLL_KEY_DOUBLE_LENGTH -> scaleLengths(2f);
            case EditorSetting.PIANO_ROLL_KEY_HALF_LENGTH -> scaleLengths(0.5f);
            case EditorSetting.PIANO_ROLL_KEY_DUPLICATE -> duplicate();
            case EditorSetting.PIANO_ROLL_KEY_LEGATO -> legato();
            default -> throwException("Unknown piano roll action: " + actionKey);
        }

        commit(findActionLabel(actionKey));
    }

    private void selectAll() {

        SongClipStruct notes = getNotes();

        if (notes == null)
            return;

        selectedNotes.clear();

        for (int note = 0; note < notes.getNoteCount(); note++)
            selectedNotes.add(note);

        revision++;
    }

    // Each selected note's start brought to the Snap grid, and its length to a whole number of the grid's steps
    private void quantize() {

        int snap = getSnapTicks();

        for (int at = 0; at < records.size(); at += EditorSetting.PIANO_ROLL_RECORD_STRIDE) {

            if (records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_SELECTED) == 0)
                continue;

            int length = records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_LENGTH);

            records.set(at + EditorSetting.PIANO_ROLL_RECORD_START,
                    Math.max(0, snapTick(records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_START))));
            records.set(at + EditorSetting.PIANO_ROLL_RECORD_LENGTH,
                    Math.max(snap, Math.round(length / (float) snap) * snap));
        }
    }

    // Each selected note moved by the semitones given; with Scale Lock on, on to the next pitch of the key that way
    private void transpose(int semitones) {
        for (int at = 0; at < records.size(); at += EditorSetting.PIANO_ROLL_RECORD_STRIDE)
            if (records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_SELECTED) != 0)
                records.set(at + EditorSetting.PIANO_ROLL_RECORD_PITCH, lockPitch(
                        records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_PITCH) + semitones,
                        Integer.signum(semitones)));
    }

    private void scaleLengths(float factor) {
        for (int at = 0; at < records.size(); at += EditorSetting.PIANO_ROLL_RECORD_STRIDE)
            if (records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_SELECTED) != 0)
                records.set(at + EditorSetting.PIANO_ROLL_RECORD_LENGTH,
                        Math.max(1, Math.round(records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_LENGTH) * factor)));
    }

    // A copy of the selected notes laid straight after them, the copies selected in their place
    private void duplicate() {

        int from = Integer.MAX_VALUE;
        int to = 0;
        int count = records.size() / EditorSetting.PIANO_ROLL_RECORD_STRIDE;

        for (int note = 0; note < count; note++) {

            int at = note * EditorSetting.PIANO_ROLL_RECORD_STRIDE;

            if (records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_SELECTED) == 0)
                continue;

            int start = records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_START);

            from = Math.min(from, start);
            to = Math.max(to, start + records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_LENGTH));
        }

        int shift = to - from;

        for (int note = 0; note < count; note++) {

            int at = note * EditorSetting.PIANO_ROLL_RECORD_STRIDE;

            if (records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_SELECTED) == 0)
                continue;

            records.set(at + EditorSetting.PIANO_ROLL_RECORD_SELECTED, 0);
            addRecord(records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_START) + shift,
                    records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_LENGTH),
                    records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_PITCH),
                    records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_VELOCITY), true);
        }
    }

    // Each selected note held on until the next note of the clip begins after it; the last keeps its length
    private void legato() {

        int count = records.size() / EditorSetting.PIANO_ROLL_RECORD_STRIDE;

        for (int note = 0; note < count; note++) {

            int at = note * EditorSetting.PIANO_ROLL_RECORD_STRIDE;

            if (records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_SELECTED) == 0)
                continue;

            int start = records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_START);
            int next = Integer.MAX_VALUE;

            for (int other = 0; other < count; other++) {

                int otherStart = records.getInt(other * EditorSetting.PIANO_ROLL_RECORD_STRIDE
                        + EditorSetting.PIANO_ROLL_RECORD_START);

                if (otherStart > start)
                    next = Math.min(next, otherStart);
            }

            if (next != Integer.MAX_VALUE)
                records.set(at + EditorSetting.PIANO_ROLL_RECORD_LENGTH, next - start);
        }
    }

    private String findActionLabel(String actionKey) {

        for (int i = 0; i < EditorSetting.PIANO_ROLL_ACTION_KEYS.length; i++)
            if (EditorSetting.PIANO_ROLL_ACTION_KEYS[i].equals(actionKey))
                return EditorSetting.PIANO_ROLL_ACTION_LABELS[i];

        return actionKey;
    }

    // Records \\

    // The clip's notes as records, each marked selected when it is, unless the selection is to be dropped
    private void loadRecords(boolean keepSelection) {

        SongClipStruct notes = getNotes();

        records.clear();

        for (int note = 0; note < notes.getNoteCount(); note++)
            addRecord(notes.getNoteStart(note), notes.getNoteLength(note), notes.getNotePitch(note),
                    notes.getNoteVelocity(note), keepSelection && selectedNotes.contains(note));
    }

    private void addRecord(int start, int length, int pitch, int velocity, boolean selected) {
        records.add(start);
        records.add(length);
        records.add(Math.clamp(pitch, EngineSetting.NOTE_PITCH_MIN, EngineSetting.NOTE_PITCH_MAX));
        records.add(Math.clamp(velocity, EngineSetting.NOTE_VELOCITY_MIN, EngineSetting.NOTE_VELOCITY_MAX));
        records.add(selected ? 1 : 0);
    }

    private void removeRecord(int note) {
        records.removeElements(note * EditorSetting.PIANO_ROLL_RECORD_STRIDE,
                (note + 1) * EditorSetting.PIANO_ROLL_RECORD_STRIDE);
    }

    // The records put in order of their start, then pitch, written as the clip's new notes with the selection carried
    // along, the clip lengthened to hold them and its span to show them, as one edit kept for undo
    private void commit(String label) {

        int stride = EditorSetting.PIANO_ROLL_RECORD_STRIDE;
        int count = records.size() / stride;

        sortRecords(count);

        int[] notes = new int[count * EngineSetting.SONG_NOTE_STRIDE];
        int end = 0;

        selectedNotes.clear();

        for (int note = 0; note < count; note++) {

            int at = note * stride;

            System.arraycopy(records.elements(), at, notes, note * EngineSetting.SONG_NOTE_STRIDE,
                    EngineSetting.SONG_NOTE_STRIDE);
            end = Math.max(end, records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_START)
                    + records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_LENGTH));

            if (records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_SELECTED) != 0)
                selectedNotes.add(note);
        }

        SongClipStruct previous = getNotes();
        SongClipStruct next = new SongClipStruct(
                previous.getKind(),
                previous.getLane(),
                previous.getStart(),
                Math.max(previous.getLength(), end),
                previous.getOffset(),
                previous.isMuted(),
                previous.getVolume(),
                previous.getTranspose(),
                previous.getInstrumentName(),
                notes,
                previous.getSongName(),
                previous.getSoundName());

        pianoRollHistoryBranch.record(shownClip, previous);
        replace(next);

        if (end > getClipEndTick())
            recorderManager.extendClip(shownClip, (float) recorderManager.getRecording().getTempo().toSeconds(
                    getOriginTick() + end));

        setStatus(EditorSetting.PIANO_ROLL_STATUS_EDITED + label);
    }

    // The clip's notes replaced by the codec clip given, as the Timeline replaces any clip's
    void replace(SongClipStruct notes) {
        recorderManager.replaceSongClip(shownClip, notes);
        revision++;
    }

    // Records in order of their start, then pitch, by insertion, so notes already in order stay where they are
    private void sortRecords(int count) {

        int stride = EditorSetting.PIANO_ROLL_RECORD_STRIDE;
        int[] elements = records.elements();
        int[] held = heldRecord;

        for (int note = 1; note < count; note++) {

            System.arraycopy(elements, note * stride, held, 0, stride);

            int to = note;

            while (to > 0 && isAfter(elements, (to - 1) * stride, held)) {
                System.arraycopy(elements, (to - 1) * stride, elements, to * stride, stride);
                to--;
            }

            System.arraycopy(held, 0, elements, to * stride, stride);
        }
    }

    private boolean isAfter(int[] elements, int at, int[] held) {

        int start = elements[at + EditorSetting.PIANO_ROLL_RECORD_START];
        int heldStart = held[EditorSetting.PIANO_ROLL_RECORD_START];

        return start > heldStart || start == heldStart
                && elements[at + EditorSetting.PIANO_ROLL_RECORD_PITCH] > held[EditorSetting.PIANO_ROLL_RECORD_PITCH];
    }

    // History \\

    public void undo() {

        if (shownClip == null || !pianoRollHistoryBranch.undo(shownClip, getNotes())) {
            setStatus(EditorSetting.PIANO_ROLL_STATUS_NOTHING_TO_UNDO);
            return;
        }

        selectedNotes.clear();
        setStatus(EditorSetting.PIANO_ROLL_STATUS_UNDONE);
    }

    public void redo() {

        if (shownClip == null || !pianoRollHistoryBranch.redo(shownClip, getNotes())) {
            setStatus(EditorSetting.PIANO_ROLL_STATUS_NOTHING_TO_REDO);
            return;
        }

        selectedNotes.clear();
        setStatus(EditorSetting.PIANO_ROLL_STATUS_REDONE);
    }

    // Rows \\

    // The Properties rows of the notes selected: the one picked last's pitch, start, length and velocity, each moving
    // every note selected
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (shownClip == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.PIANO_ROLL_ROWS_NO_CLIP));
            return;
        }

        if (selectedNotes.isEmpty()) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.PIANO_ROLL_ROWS_NO_NOTE));
            return;
        }

        SongClipStruct notes = getNotes();
        int focused = getFocusedNote();
        float beat = EngineSetting.SONG_TICKS_PER_BEAT;
        float snapBeats = getSnapTicks() / beat;

        rows.add(CharacterViewRowStruct.header(String.format(EditorSetting.PIANO_ROLL_ROWS_HEADER_FORMAT,
                selectedNotes.size())));
        rows.add(CharacterViewRowStruct.ranged(EditorSetting.PIANO_ROLL_ROW_PITCH,
                EditorSetting.PIANO_ROLL_ROW_LABEL_PITCH, getRowValueText(EditorSetting.PIANO_ROLL_ROW_PITCH),
                notes.getNotePitch(focused), EngineSetting.NOTE_PITCH_MIN, EngineSetting.NOTE_PITCH_MAX, 1f));
        rows.add(CharacterViewRowStruct.ranged(EditorSetting.PIANO_ROLL_ROW_START,
                EditorSetting.PIANO_ROLL_ROW_LABEL_START, getRowValueText(EditorSetting.PIANO_ROLL_ROW_START),
                notes.getNoteStart(focused) / beat, getClipStartTick() / beat,
                Math.max(getClipEndTick(), getClipStartTick() + getSnapTicks()) / beat, snapBeats));
        rows.add(CharacterViewRowStruct.ranged(EditorSetting.PIANO_ROLL_ROW_LENGTH,
                EditorSetting.PIANO_ROLL_ROW_LABEL_LENGTH, getRowValueText(EditorSetting.PIANO_ROLL_ROW_LENGTH),
                notes.getNoteLength(focused) / beat, snapBeats,
                Math.max(snapBeats * 2f, (getClipEndTick() - getClipStartTick()) / beat), snapBeats));
        rows.add(CharacterViewRowStruct.ranged(EditorSetting.PIANO_ROLL_ROW_VELOCITY,
                EditorSetting.PIANO_ROLL_ROW_LABEL_VELOCITY, getRowValueText(EditorSetting.PIANO_ROLL_ROW_VELOCITY),
                notes.getNoteVelocity(focused), EngineSetting.NOTE_VELOCITY_MIN, EngineSetting.NOTE_VELOCITY_MAX, 1f));
    }

    private int getFocusedNote() {
        return selectedNotes.getInt(selectedNotes.size() - 1);
    }

    public String getRowValueText(String key) {

        if (shownClip == null || selectedNotes.isEmpty())
            return EditorSetting.RECORDER_ROW_VALUE_NONE;

        SongClipStruct notes = getNotes();
        int focused = getFocusedNote();
        float beat = EngineSetting.SONG_TICKS_PER_BEAT;

        return switch (key) {
            case EditorSetting.PIANO_ROLL_ROW_PITCH -> String.format(EditorSetting.PIANO_ROLL_PITCH_FORMAT,
                    NoteUtility.toNoteName(notes.getNotePitch(focused)), notes.getNotePitch(focused));
            case EditorSetting.PIANO_ROLL_ROW_START -> CharacterViewUtility.formatNumber(
                    notes.getNoteStart(focused) / beat);
            case EditorSetting.PIANO_ROLL_ROW_LENGTH -> CharacterViewUtility.formatNumber(
                    notes.getNoteLength(focused) / beat);
            case EditorSetting.PIANO_ROLL_ROW_VELOCITY -> Integer.toString(notes.getNoteVelocity(focused));
            default -> EditorSetting.RECORDER_ROW_VALUE_NONE;
        };
    }

    // Every note selected moved by whole steps of its row: a semitone, a Snap step, a velocity
    public void stepRow(String key, int steps) {

        if (!requireSelection())
            return;

        loadRecords(true);
        shiftSelected(key, steps * resolveRowStep(key), false);
        commit(EditorSetting.PIANO_ROLL_LABEL_SET);
    }

    public boolean isRowValueValid(String key, String text) {
        return !Float.isNaN(parseRowValue(key, text));
    }

    // Every note selected set to the value typed: a pitch by number or name, a start or length in beats, a velocity
    public void setRowValue(String key, String text) {

        float value = parseRowValue(key, text);

        if (Float.isNaN(value) || !requireSelection())
            return;

        loadRecords(true);
        shiftSelected(key, Math.round(value), true);
        commit(EditorSetting.PIANO_ROLL_LABEL_SET);
    }

    // The value typed in the row's own whole units — a pitch, ticks, a velocity — not a number when it is none
    private float parseRowValue(String key, String text) {

        if (key.equals(EditorSetting.PIANO_ROLL_ROW_PITCH)) {

            int named = NoteUtility.parseNoteName(text.trim());

            if (named != EngineSetting.INDEX_NOT_FOUND)
                return named;
        }

        float number = CharacterViewUtility.parseNumber(text);
        boolean inBeats = key.equals(EditorSetting.PIANO_ROLL_ROW_START)
                || key.equals(EditorSetting.PIANO_ROLL_ROW_LENGTH);

        return inBeats ? number * EngineSetting.SONG_TICKS_PER_BEAT : number;
    }

    private int resolveRowStep(String key) {
        return key.equals(EditorSetting.PIANO_ROLL_ROW_START) || key.equals(EditorSetting.PIANO_ROLL_ROW_LENGTH)
                ? getSnapTicks()
                : 1;
    }

    // The field the row names moved by the amount, or set to it, on every selected record, each held to its range
    private void shiftSelected(String key, int amount, boolean set) {

        int field = switch (key) {
            case EditorSetting.PIANO_ROLL_ROW_PITCH -> EditorSetting.PIANO_ROLL_RECORD_PITCH;
            case EditorSetting.PIANO_ROLL_ROW_START -> EditorSetting.PIANO_ROLL_RECORD_START;
            case EditorSetting.PIANO_ROLL_ROW_LENGTH -> EditorSetting.PIANO_ROLL_RECORD_LENGTH;
            default -> EditorSetting.PIANO_ROLL_RECORD_VELOCITY;
        };
        int low = switch (field) {
            case EditorSetting.PIANO_ROLL_RECORD_PITCH -> EngineSetting.NOTE_PITCH_MIN;
            case EditorSetting.PIANO_ROLL_RECORD_START -> 0;
            case EditorSetting.PIANO_ROLL_RECORD_LENGTH -> 1;
            default -> EngineSetting.NOTE_VELOCITY_MIN;
        };
        int high = switch (field) {
            case EditorSetting.PIANO_ROLL_RECORD_PITCH -> EngineSetting.NOTE_PITCH_MAX;
            case EditorSetting.PIANO_ROLL_RECORD_VELOCITY -> EngineSetting.NOTE_VELOCITY_MAX;
            default -> Integer.MAX_VALUE;
        };

        for (int at = 0; at < records.size(); at += EditorSetting.PIANO_ROLL_RECORD_STRIDE) {

            if (records.getInt(at + EditorSetting.PIANO_ROLL_RECORD_SELECTED) == 0)
                continue;

            int value = set ? amount : records.getInt(at + field) + amount;

            if (field == EditorSetting.PIANO_ROLL_RECORD_PITCH)
                value = lockPitch(value, set ? 0 : Integer.signum(amount));

            records.set(at + field, Math.clamp(value, low, high));
        }
    }

    // Find \\

    // The note under the clip's own tick on the pitch, the one begun last where several are; -1 for none
    public int findNoteAt(int tick, int pitch) {

        SongClipStruct notes = getNotes();
        int found = EngineSetting.INDEX_NOT_FOUND;

        if (notes == null)
            return found;

        for (int note = 0; note < notes.getNoteCount(); note++)
            if (notes.getNotePitch(note) == pitch && tick >= notes.getNoteStart(note)
                    && tick < notes.getNoteStart(note) + notes.getNoteLength(note))
                found = note;

        return found;
    }

    // True when the press lies on the note's last stretch, where its end is taken hold of
    private boolean isOnEnd(int note, int tick, int grabTicks) {

        SongClipStruct notes = getNotes();
        int end = notes.getNoteStart(note) + notes.getNoteLength(note);
        int zone = Math.min(grabTicks,
                Math.round(notes.getNoteLength(note) * EditorSetting.PIANO_ROLL_RESIZE_GRAB_SHARE));

        return end - tick <= zone;
    }

    // Utility \\

    private boolean requireSelection() {

        if (shownClip != null && !selectedNotes.isEmpty())
            return true;

        setStatus(shownClip == null
                ? EditorSetting.PIANO_ROLL_STATUS_NO_CLIP
                : EditorSetting.PIANO_ROLL_STATUS_NOTHING_SELECTED);
        return false;
    }

    // Notes are edited whenever a notes clip is shown, but for an export; typed notes go in while the transport
    // records
    private boolean canEdit() {
        return recorderManager.getState() != RecorderState.EXPORTING;
    }

    void setStatus(String statusText) {
        this.statusText = statusText;
        revision++;
    }

    void notifyChanged() {
        revision++;
    }

    // Musical Typing \\

    // Musical Typing turned on or off for the Piano Roll's window, which claims its keys while it is on
    public void toggleTyping(WindowInstance window) {
        pianoRollTypingBranch.toggle(window);
    }

    // Every note key read on the Piano Roll's window while it is the active one, each sounding and, while the
    // transport records, written into the clip
    public void updateTyping(WindowInstance window, boolean active) {
        pianoRollTypingBranch.update(window, active);
    }

    public void stepTypingOctave(int direction) {
        pianoRollTypingBranch.stepOctave(direction);
    }

    public void stepTypingVelocity(int direction) {
        pianoRollTypingBranch.stepVelocity(direction);
    }

    public boolean isTyping() {
        return pianoRollTypingBranch.isOn();
    }

    public String describeTyping() {
        return pianoRollTypingBranch.describe();
    }

    // Accessible \\

    public IntArrayList getSelectedNotes() {
        return selectedNotes;
    }

    public int getLowPitch() {
        return lowPitch;
    }

    public int getVisibleRows() {
        return visibleRows;
    }

    public int getViewStartTick() {
        return viewStartTick;
    }

    public int getViewSpanTicks() {
        return viewSpanTicks;
    }

    public int getViewRevision() {
        return viewRevision;
    }

    public PianoRollDragKind getDragKind() {
        return dragKind;
    }

    public WindowInstance getDragWindow() {
        return dragWindow;
    }

    public boolean isDragMoved() {
        return moved;
    }

    public int getDragTicks() {
        return dragTicks;
    }

    public int getDragPitches() {
        return dragPitches;
    }

    public int getPressTick() {
        return pressTick;
    }

    public int getPressPitch() {
        return pressPitch;
    }

    public int getReachTick() {
        return reachTick;
    }

    public int getReachPitch() {
        return reachPitch;
    }

    public int getRevision() {
        return revision;
    }

    public String getStatusText() {
        return statusText;
    }
}
