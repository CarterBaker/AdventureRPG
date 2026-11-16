package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshHistoryStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterHistoryBranch extends BranchPackage {

    /*
     * Records a character mesh's undo steps. An edit opens a step with a copy
     * of the mesh as it stood; committing it clears the redo history and
     * drops the oldest steps past the limit, and cancelling it puts the copy
     * back. Undo and redo swap the mesh with the step on top of one stack and
     * move the side they replaced onto the other. A step that joined a part
     * puts the rig back with the mesh while the rig still stands as the
     * joining left it, the rig first so the mesh comes back to the bones it
     * names, the open mesh's other steps held out of the rig's relay
     * meanwhile, since each was recorded against the rig it will come back
     * to; once the rig has changed some other way the mesh alone is undone,
     * the part's bones left unused, and a joining waiting to be redone can no
     * longer be.
     */

    // Internal
    private CharacterRigBranch characterRigBranch;
    private CharacterMeshBranch characterMeshBranch;

    // Base \\

    @Override
    protected void get() {
        this.characterRigBranch = get(CharacterRigBranch.class);
        this.characterMeshBranch = get(CharacterMeshBranch.class);
    }

    // Step \\

    void begin(CharacterMeshDocumentInstance document) {

        if (!document.hasOpenStep())
            document.setOpenStep(snapshot(document, null, null));
    }

    // Opens the step of a part being joined, the rig as it stood before and as the joining leaves it
    void begin(CharacterMeshDocumentInstance document, ArpgObjectStruct rigBefore, ArpgObjectStruct rigAfter) {

        if (!document.hasOpenStep())
            document.setOpenStep(snapshot(document, rigBefore, rigAfter));
    }

    void commit(CharacterMeshDocumentInstance document) {

        CharacterMeshHistoryStruct step = document.getOpenStep();

        if (step == null)
            return;

        ObjectArrayList<CharacterMeshHistoryStruct> undoSteps = document.getUndoSteps();

        document.setOpenStep(null);
        document.getRedoSteps().clear();
        undoSteps.push(step);

        while (undoSteps.size() > EditorSetting.CHARACTER_MESH_HISTORY_MAX_STEPS)
            undoSteps.remove(0);
    }

    void cancel(CharacterMeshDocumentInstance document) {

        CharacterMeshHistoryStruct step = document.getOpenStep();

        if (step == null)
            return;

        document.setOpenStep(null);
        document.restore(step);
    }

    // Undo \\

    boolean undo(CharacterMeshDocumentInstance document) {
        return swap(document, document.getUndoSteps(), document.getRedoSteps());
    }

    boolean redo(CharacterMeshDocumentInstance document) {
        return swap(document, document.getRedoSteps(), document.getUndoSteps());
    }

    private boolean swap(
            CharacterMeshDocumentInstance document,
            ObjectArrayList<CharacterMeshHistoryStruct> from,
            ObjectArrayList<CharacterMeshHistoryStruct> to) {

        if (document.hasOpenStep() || from.isEmpty())
            return false;

        CharacterMeshHistoryStruct step = from.top();
        boolean paired = step.holdsRig() && characterRigBranch.matchesRig(step.getRigExpected());

        if (step.holdsRig() && !paired && from == document.getRedoSteps()) {
            from.clear();
            return false;
        }

        from.pop();
        to.push(paired
                ? snapshot(document, step.getRigExpected(), step.getRigRestore())
                : snapshot(document, null, null));

        if (paired) {
            characterMeshBranch.holdHistory(document);
            characterRigBranch.replaceRig(step.getRigRestore());
            characterMeshBranch.releaseHistory();
        }

        document.restore(step);
        document.markEdited();

        return true;
    }

    // Management \\

    void clear(CharacterMeshDocumentInstance document) {
        document.setOpenStep(null);
        document.getUndoSteps().clear();
        document.getRedoSteps().clear();
    }

    private CharacterMeshHistoryStruct snapshot(
            CharacterMeshDocumentInstance document,
            ArpgObjectStruct rigRestore,
            ArpgObjectStruct rigExpected) {
        return new CharacterMeshHistoryStruct(
                document.getMesh(),
                document.getSelection(),
                document.getSelectionWeights(),
                document.isDirty(),
                rigRestore,
                rigExpected);
    }

    // Accessible \\

    boolean canUndo(CharacterMeshDocumentInstance document) {
        return !document.getUndoSteps().isEmpty();
    }

    boolean canRedo(CharacterMeshDocumentInstance document) {
        return !document.getRedoSteps().isEmpty();
    }
}
