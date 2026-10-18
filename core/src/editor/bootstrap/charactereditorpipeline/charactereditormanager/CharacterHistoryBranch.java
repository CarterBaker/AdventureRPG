package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshHistoryStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterHistoryBranch extends BranchPackage {

    /*
     * Records a character mesh's undo steps. An edit opens a step with a copy
     * of the mesh as it stood; committing it clears the redo history and
     * drops the oldest steps past the limit, and cancelling it puts the copy
     * back. Undo and redo swap the mesh with the step on top of one stack and
     * move the side they replaced onto the other.
     */

    // Step \\

    void begin(CharacterMeshDocumentInstance document) {

        if (!document.hasOpenStep())
            document.setOpenStep(snapshot(document));
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

        CharacterMeshHistoryStruct step = from.pop();

        to.push(snapshot(document));
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

    private CharacterMeshHistoryStruct snapshot(CharacterMeshDocumentInstance document) {
        return new CharacterMeshHistoryStruct(document.getMesh(), document.getSelection(), document.isDirty());
    }

    // Accessible \\

    boolean canUndo(CharacterMeshDocumentInstance document) {
        return !document.getUndoSteps().isEmpty();
    }

    boolean canRedo(CharacterMeshDocumentInstance document) {
        return !document.getRedoSteps().isEmpty();
    }
}
