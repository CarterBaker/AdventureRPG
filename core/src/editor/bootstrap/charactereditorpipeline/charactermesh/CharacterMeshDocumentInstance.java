package editor.bootstrap.charactereditorpipeline.charactermesh;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterMeshDocumentInstance extends InstancePackage {

    /*
     * One rigged mesh file open in the character editor: the registered mesh
     * it rebuilds in place, the file's own fields as last read or written,
     * the mesh being edited, its vertex selection and active vertex, the
     * vertex each mirrors across x, and its undo history. The revision moves
     * on every change of geometry or selection so viewports know to redraw;
     * dirty marks changes not yet saved.
     */

    // Identity
    private String meshName;
    private MeshHandle meshHandle;
    private ArpgObjectStruct sourceArpg;

    // Mesh
    private RiggedMeshStruct mesh;

    // Selection
    private BooleanArrayList selection;
    private int activeVertex;

    // Mirror
    private IntArrayList mirrorVertices;
    private boolean mirrorStale;

    // History
    private ObjectArrayList<CharacterMeshHistoryStruct> undoSteps;
    private ObjectArrayList<CharacterMeshHistoryStruct> redoSteps;
    private CharacterMeshHistoryStruct openStep;

    // State
    private boolean dirty;
    private int revision;

    // Constructor \\

    public void constructor(MeshHandle meshHandle, ArpgObjectStruct sourceArpg, RiggedMeshStruct mesh) {

        // Identity
        this.meshName = meshHandle.getMeshName();
        this.meshHandle = meshHandle;

        // Selection
        this.selection = new BooleanArrayList();

        // Mirror
        this.mirrorVertices = new IntArrayList();

        // History
        this.undoSteps = new ObjectArrayList<>();
        this.redoSteps = new ObjectArrayList<>();

        replace(sourceArpg, mesh);
    }

    // Management \\

    // Takes the file as read from disk again, dropping the selection and every unsaved change
    public void replace(ArpgObjectStruct sourceArpg, RiggedMeshStruct mesh) {

        this.sourceArpg = sourceArpg;
        this.mesh = mesh;
        this.activeVertex = EngineSetting.INDEX_NOT_FOUND;
        this.mirrorStale = true;
        this.dirty = false;
        this.revision++;

        selection.clear();
        selection.size(mesh.getVertexCount());
    }

    public void markSaved(ArpgObjectStruct sourceArpg) {
        this.sourceArpg = sourceArpg;
        this.dirty = false;
        this.revision++;
    }

    // Geometry changed: the mesh needs saving, viewports redrawing and the mirror pairs finding again
    public void markEdited() {
        this.dirty = true;
        this.mirrorStale = true;
        this.revision++;
    }

    // Puts one side of an undo step back: its mesh, its selection and whether it held unsaved changes
    public void restore(CharacterMeshHistoryStruct step) {

        mesh.set(step.getMesh());
        selection.clear();
        selection.addAll(step.getSelection());
        syncSelection();

        this.dirty = step.isDirty();
        this.mirrorStale = true;
        this.revision++;
    }

    // Selection changed
    public void markSelectionChanged() {
        this.revision++;
    }

    // Keeps one selection flag per vertex, new vertices unselected, after the vertex count changes
    public void syncSelection() {

        selection.size(mesh.getVertexCount());

        if (activeVertex >= mesh.getVertexCount())
            this.activeVertex = EngineSetting.INDEX_NOT_FOUND;
    }

    public void setActiveVertex(int activeVertex) {
        this.activeVertex = activeVertex;
    }

    public void setOpenStep(CharacterMeshHistoryStruct openStep) {
        this.openStep = openStep;
    }

    public void markMirrorResolved() {
        this.mirrorStale = false;
    }

    // Accessible \\

    public String getMeshName() {
        return meshName;
    }

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    public ArpgObjectStruct getSourceArpg() {
        return sourceArpg;
    }

    public RiggedMeshStruct getMesh() {
        return mesh;
    }

    public BooleanArrayList getSelection() {
        return selection;
    }

    public boolean isSelected(int vertexIndex) {
        return selection.getBoolean(vertexIndex);
    }

    public int getActiveVertex() {
        return activeVertex;
    }

    public IntArrayList getMirrorVertices() {
        return mirrorVertices;
    }

    public boolean isMirrorStale() {
        return mirrorStale;
    }

    public ObjectArrayList<CharacterMeshHistoryStruct> getUndoSteps() {
        return undoSteps;
    }

    public ObjectArrayList<CharacterMeshHistoryStruct> getRedoSteps() {
        return redoSteps;
    }

    public CharacterMeshHistoryStruct getOpenStep() {
        return openStep;
    }

    public boolean hasOpenStep() {
        return openStep != null;
    }

    public boolean isDirty() {
        return dirty;
    }

    public int getRevision() {
        return revision;
    }
}
