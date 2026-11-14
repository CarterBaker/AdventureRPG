package editor.runtime.characterview.history;

import editor.runtime.EditorSetting;
import engine.root.StructPackage;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterViewHistoryStruct extends StructPackage {

    /*
     * The undo history of one ARPG entry a character tool edits through
     * InfoManager, kept as whole copies of the entry. An edit records the
     * entry as it stood before it; a gesture that edits on every frame —
     * a drag in the viewport — opens one step as it starts and commits it as
     * it ends, so the whole drag undoes at once, or hands the entry back as
     * it stood when the drag is cancelled. Recording clears the redo steps
     * and drops the oldest past the limit. Undo and redo hand back the copy
     * to put in place and keep the entry as it stood on the other stack.
     */

    // Steps
    private final ObjectArrayList<ArpgObjectStruct> undoSteps;
    private final ObjectArrayList<ArpgObjectStruct> redoSteps;
    private ArpgObjectStruct openStep;

    // Constructor \\

    public CharacterViewHistoryStruct() {

        // Steps
        this.undoSteps = new ObjectArrayList<>();
        this.redoSteps = new ObjectArrayList<>();
    }

    // Record \\

    // The entry as it stands before an edit; inside an open gesture the gesture's own step already holds it
    public void record(ArpgObjectStruct entryArpg) {

        if (openStep == null)
            push(entryArpg.deepCopy());
    }

    public void begin(ArpgObjectStruct entryArpg) {

        if (openStep == null)
            this.openStep = entryArpg.deepCopy();
    }

    // Closes an open gesture; one that changed nothing leaves no step
    public void commit(ArpgObjectStruct entryArpg) {

        ArpgObjectStruct step = openStep;
        this.openStep = null;

        if (step != null && !step.equals(entryArpg))
            push(step);
    }

    // Drops an open gesture, handing back the entry as it stood when the gesture began; null when none is open
    public ArpgObjectStruct cancel() {

        ArpgObjectStruct step = openStep;
        this.openStep = null;

        return step;
    }

    private void push(ArpgObjectStruct step) {

        redoSteps.clear();
        undoSteps.push(step);

        while (undoSteps.size() > EditorSetting.CHARACTER_VIEW_HISTORY_MAX_STEPS)
            undoSteps.remove(0);
    }

    // Undo \\

    // The copy to put in place of the entry, null when there is nothing to undo or a gesture is open
    public ArpgObjectStruct undo(ArpgObjectStruct entryArpg) {
        return swap(undoSteps, redoSteps, entryArpg);
    }

    public ArpgObjectStruct redo(ArpgObjectStruct entryArpg) {
        return swap(redoSteps, undoSteps, entryArpg);
    }

    private ArpgObjectStruct swap(
            ObjectArrayList<ArpgObjectStruct> from,
            ObjectArrayList<ArpgObjectStruct> to,
            ArpgObjectStruct entryArpg) {

        if (openStep != null || from.isEmpty())
            return null;

        to.push(entryArpg.deepCopy());

        return from.pop();
    }

    // Management \\

    public void clear() {
        this.openStep = null;
        undoSteps.clear();
        redoSteps.clear();
    }

    // Accessible \\

    public boolean isOpen() {
        return openStep != null;
    }
}
