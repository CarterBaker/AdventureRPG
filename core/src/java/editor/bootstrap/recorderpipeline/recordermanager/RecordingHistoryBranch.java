package editor.bootstrap.recorderpipeline.recordermanager;

import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingHistoryBranch extends BranchPackage {

    /*
     * The undo history of the open recording, kept as whole copies of it as
     * its file would hold it, built by RecordingFileBranch so the packed
     * samples of every track and curve no edit has touched are shared by
     * every copy and a step costs only what its edit changed. The base is the
     * recording as it stood after the last edit; an edit pushes the base onto
     * the undo steps and keeps the recording as it now stands as the next
     * base, clearing the redo steps and dropping the oldest past the limit,
     * and an edit that changed nothing leaves no step. Edits made one after
     * another to the same row are one step, so a slider dragged or a value
     * stepped undoes at once. Curves laid from what a take recorded move the
     * base without a step of their own, since they are laid the same way
     * again whenever a step brings back a recording without them. Undo and
     * redo hand back the copy to put in place, keeping the base on the other
     * stack; a row cancelled hands back the base it began from, leaving no
     * step and nothing to redo.
     */

    // Internal
    private RecordingFileBranch recordingFileBranch;

    // Steps
    private ObjectArrayList<ArpgObjectStruct> undoSteps;
    private ObjectArrayList<ArpgObjectStruct> redoSteps;
    private ArpgObjectStruct base;

    // Rows — the row being stepped, and the row the last step changed
    private String rowKey;
    private String stepRowKey;

    // Base \\

    @Override
    protected void create() {

        // Steps
        this.undoSteps = new ObjectArrayList<>();
        this.redoSteps = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.recordingFileBranch = get(RecordingFileBranch.class);
    }

    // Management \\

    // The history begun again from the recording as it stands, none when no recording is open
    void reset(RecordingInstance recording) {
        undoSteps.clear();
        redoSteps.clear();
        this.stepRowKey = null;
        this.base = recording != null ? recordingFileBranch.build(recording) : null;
    }

    // Record \\

    // An edit of the recording: merged into the last step when it changes the same row again, else a step of its own
    void commit(RecordingInstance recording) {

        if (recording == null || base == null)
            return;

        ArpgObjectStruct current = recordingFileBranch.build(recording);

        if (current.equals(base))
            return;

        if (rowKey == null || !rowKey.equals(stepRowKey) || undoSteps.isEmpty()) {
            redoSteps.clear();
            undoSteps.push(base);

            while (undoSteps.size() > EditorSetting.RECORDER_HISTORY_MAX_STEPS)
                undoSteps.remove(0);
        }

        this.stepRowKey = rowKey;
        this.base = current;
    }

    // Something derived from what the recording already holds was laid on it, which no step need undo
    void rebase(RecordingInstance recording) {
        if (recording != null && base != null)
            this.base = recordingFileBranch.build(recording);
    }

    // The row whose value the edits until endRow() step, so they are one step however many there are
    void beginRow(String steppedRowKey) {
        this.rowKey = steppedRowKey;
    }

    void endRow() {
        this.rowKey = null;
    }

    // The step the row made since beginRow() taken back as if never made, nothing left to redo: the copy to put in
    // place of the recording, null when the row changed nothing
    ArpgObjectStruct cancelRow() {

        boolean stepped = rowKey != null && rowKey.equals(stepRowKey) && !undoSteps.isEmpty();

        this.rowKey = null;
        this.stepRowKey = null;

        if (!stepped || base == null)
            return null;

        this.base = undoSteps.pop();
        return base;
    }

    // Undo \\

    // The copy to put in place of the recording, null when there is nothing to undo
    ArpgObjectStruct undo() {
        return swap(undoSteps, redoSteps);
    }

    ArpgObjectStruct redo() {
        return swap(redoSteps, undoSteps);
    }

    private ArpgObjectStruct swap(ObjectArrayList<ArpgObjectStruct> from, ObjectArrayList<ArpgObjectStruct> to) {

        if (base == null || from.isEmpty())
            return null;

        to.push(base);
        this.base = from.pop();
        this.stepRowKey = null;

        return base;
    }
}
