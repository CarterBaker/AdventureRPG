package editor.bootstrap.recorderpipeline.pianorollmanager;

import application.bootstrap.musicpipeline.song.SongClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class PianoRollHistoryBranch extends BranchPackage {

    /*
     * The Piano Roll's undo and redo. A notes clip's notes are a codec clip
     * replaced whole by every edit and never changed in place, so each step
     * keeps the clip and the notes it held before; undo puts them back and
     * keeps what it took off for redo, and a new edit lets every redo go. A
     * step belongs to its clip, so undo and redo walk only the clip shown,
     * and at most PIANO_ROLL_HISTORY_DEPTH steps are kept.
     */

    // Internal
    private PianoRollManager pianoRollManager;

    // History
    private ObjectArrayList<RecordingClipStruct> undoClips;
    private ObjectArrayList<SongClipStruct> undoNotes;
    private ObjectArrayList<RecordingClipStruct> redoClips;
    private ObjectArrayList<SongClipStruct> redoNotes;

    // Base \\

    @Override
    protected void create() {

        // History
        this.undoClips = new ObjectArrayList<>();
        this.undoNotes = new ObjectArrayList<>();
        this.redoClips = new ObjectArrayList<>();
        this.redoNotes = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.pianoRollManager = get(PianoRollManager.class);
    }

    // Record \\

    // An edit about to replace the clip's notes kept to undo, every redo let go
    void record(RecordingClipStruct clip, SongClipStruct previous) {

        push(undoClips, undoNotes, clip, previous);
        redoClips.clear();
        redoNotes.clear();
    }

    private void push(
            ObjectArrayList<RecordingClipStruct> clips,
            ObjectArrayList<SongClipStruct> notes,
            RecordingClipStruct clip,
            SongClipStruct held) {

        if (clips.size() == EditorSetting.PIANO_ROLL_HISTORY_DEPTH) {
            clips.remove(0);
            notes.remove(0);
        }

        clips.add(clip);
        notes.add(held);
    }

    // Step \\

    // The clip's last edit undone, what it held now kept for redo; false when the clip has none
    boolean undo(RecordingClipStruct clip, SongClipStruct current) {
        return step(undoClips, undoNotes, redoClips, redoNotes, clip, current);
    }

    boolean redo(RecordingClipStruct clip, SongClipStruct current) {
        return step(redoClips, redoNotes, undoClips, undoNotes, clip, current);
    }

    private boolean step(
            ObjectArrayList<RecordingClipStruct> fromClips,
            ObjectArrayList<SongClipStruct> fromNotes,
            ObjectArrayList<RecordingClipStruct> toClips,
            ObjectArrayList<SongClipStruct> toNotes,
            RecordingClipStruct clip,
            SongClipStruct current) {

        int at = fromClips.lastIndexOf(clip);

        if (at == EngineSetting.INDEX_NOT_FOUND)
            return false;

        SongClipStruct restored = fromNotes.get(at);

        fromClips.remove(at);
        fromNotes.remove(at);
        push(toClips, toNotes, clip, current);
        pianoRollManager.replace(restored);
        return true;
    }
}
