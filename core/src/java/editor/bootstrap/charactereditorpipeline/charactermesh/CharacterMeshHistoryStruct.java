package editor.bootstrap.charactereditorpipeline.charactermesh;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class CharacterMeshHistoryStruct extends StructPackage {

    /*
     * One undo step of a character mesh: a copy of the whole mesh and of
     * its vertex selection, with how much of each vertex the selection
     * holds, as they stood on one side of an edit, and whether the mesh
     * then held unsaved changes, so undo, redo and a cancelled edit each
     * put one side back exactly.
     */

    // Mesh
    private final RiggedMeshStruct mesh;

    // Selection
    private final BooleanArrayList selection;
    private final FloatArrayList selectionWeights;

    // State
    private final boolean dirty;

    // Constructor \\

    public CharacterMeshHistoryStruct(
            RiggedMeshStruct mesh,
            BooleanArrayList selection,
            FloatArrayList selectionWeights,
            boolean dirty) {

        // Mesh
        this.mesh = new RiggedMeshStruct(mesh);

        // Selection
        this.selection = new BooleanArrayList(selection);
        this.selectionWeights = new FloatArrayList(selectionWeights);

        // State
        this.dirty = dirty;
    }

    // Accessible \\

    public RiggedMeshStruct getMesh() {
        return mesh;
    }

    public BooleanArrayList getSelection() {
        return selection;
    }

    public FloatArrayList getSelectionWeights() {
        return selectionWeights;
    }

    public boolean isDirty() {
        return dirty;
    }
}
