package editor.bootstrap.materialspipeline.materialsmanager;

import editor.bootstrap.materialspipeline.materials.MaterialDocumentStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class MaterialHistoryBranch extends BranchPackage {

    /*
     * The undo history of the open material: each edit keeps a copy of the
     * material as it stood before it, oldest first, so Undo puts the last one
     * back and Redo takes it again. Steps of one row in a row, a slider
     * dragged or a step button held, are one edit, as a painting tool's
     * slider undoes at once; any other edit starts a new one. Opening,
     * reverting or deleting a material clears it.
     */

    // History
    private ObjectArrayList<MaterialDocumentStruct> undoSteps;
    private ObjectArrayList<MaterialDocumentStruct> redoSteps;

    // Merge — the row the last edit stepped, null when the last edit was no row's step
    private String mergeKey;

    // Base \\

    @Override
    protected void create() {

        // History
        this.undoSteps = new ObjectArrayList<>();
        this.redoSteps = new ObjectArrayList<>();
    }

    // Record \\

    // The material as it stands kept before an edit; a step of the same row as the last edit joins it
    void record(MaterialDocumentStruct document, String rowKey) {

        if (rowKey != null && rowKey.equals(mergeKey))
            return;

        undoSteps.add(document.copy());
        redoSteps.clear();
        this.mergeKey = rowKey;

        while (undoSteps.size() > EditorSetting.MATERIALS_HISTORY_MAX_STEPS)
            undoSteps.remove(0);
    }

    // History \\

    boolean undo(MaterialDocumentStruct document) {
        return step(document, undoSteps, redoSteps);
    }

    boolean redo(MaterialDocumentStruct document) {
        return step(document, redoSteps, undoSteps);
    }

    // The latest step put back, the material as it stood kept on the other side
    private boolean step(
            MaterialDocumentStruct document,
            ObjectArrayList<MaterialDocumentStruct> from,
            ObjectArrayList<MaterialDocumentStruct> to) {

        if (from.isEmpty())
            return false;

        to.add(document.copy());
        document.copyFrom(from.pop());
        this.mergeKey = null;

        return true;
    }

    void clear() {
        undoSteps.clear();
        redoSteps.clear();
        this.mergeKey = null;
    }
}
