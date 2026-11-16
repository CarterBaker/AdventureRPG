package editor.bootstrap.charactereditorpipeline.charactermesh;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import engine.root.StructPackage;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class CharacterMeshHistoryStruct extends StructPackage {

    /*
     * One undo step of a character mesh: a copy of the whole mesh and of
     * its vertex selection, with how much of each vertex the selection
     * holds, as they stood on one side of an edit, and whether the mesh
     * then held unsaved changes, so undo, redo and a cancelled edit each
     * put one side back exactly. A step that joined a part to the body also
     * holds the rig on both sides of the joining, the rig to put back with
     * the mesh and the rig it must still be for that, since the part's
     * weights name the bones it brought.
     */

    // Mesh
    private final RiggedMeshStruct mesh;

    // Selection
    private final BooleanArrayList selection;
    private final FloatArrayList selectionWeights;

    // State
    private final boolean dirty;

    // Rig — the rig put back with this side's mesh, and the rig as the other side left it, both null for a step of
    // the mesh alone
    private final ArpgObjectStruct rigRestore;
    private final ArpgObjectStruct rigExpected;

    // Constructor \\

    public CharacterMeshHistoryStruct(
            RiggedMeshStruct mesh,
            BooleanArrayList selection,
            FloatArrayList selectionWeights,
            boolean dirty,
            ArpgObjectStruct rigRestore,
            ArpgObjectStruct rigExpected) {

        // Mesh
        this.mesh = new RiggedMeshStruct(mesh);

        // Selection
        this.selection = new BooleanArrayList(selection);
        this.selectionWeights = new FloatArrayList(selectionWeights);

        // State
        this.dirty = dirty;

        // Rig
        this.rigRestore = rigRestore;
        this.rigExpected = rigExpected;
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

    // Whether the step joined a part, so the rig goes back with the mesh
    public boolean holdsRig() {
        return rigRestore != null;
    }

    public ArpgObjectStruct getRigRestore() {
        return rigRestore;
    }

    public ArpgObjectStruct getRigExpected() {
        return rigExpected;
    }
}
