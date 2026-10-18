package editor.bootstrap.charactereditorpipeline.charactermesh;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;

public class CharacterMeshHistoryStruct extends StructPackage {

    /*
     * One undo step of a character mesh: a copy of the whole mesh and of its
     * vertex selection as they stood on one side of an edit, and whether the
     * mesh then held unsaved changes, so undo, redo and a cancelled edit each
     * put one side back exactly.
     */

    // Mesh
    private final RiggedMeshStruct mesh;

    // Selection
    private final BooleanArrayList selection;

    // State
    private final boolean dirty;

    // Constructor \\

    public CharacterMeshHistoryStruct(RiggedMeshStruct mesh, BooleanArrayList selection, boolean dirty) {

        // Mesh
        this.mesh = new RiggedMeshStruct(mesh);

        // Selection
        this.selection = new BooleanArrayList(selection);

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

    public boolean isDirty() {
        return dirty;
    }
}
