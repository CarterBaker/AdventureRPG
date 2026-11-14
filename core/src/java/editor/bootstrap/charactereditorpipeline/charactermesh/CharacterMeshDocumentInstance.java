package editor.bootstrap.charactereditorpipeline.charactermesh;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterMeshDocumentInstance extends InstancePackage {

    /*
     * One rigged mesh file open in the character editor: the registered
     * mesh it rebuilds in place, the file's own fields as last read or
     * written, the mesh being edited, its vertex selection with how much of
     * each vertex it holds (a whole vertex 1, one the brush's soft edge
     * took in less, every vertex outside it held whole so it joins whole)
     * and its active vertex, the vertex each mirrors across x, its undo
     * history, and the shape key being sculpted with the base positions it
     * was lifted from. The revision moves on every change of geometry or
     * selection so viewports know to redraw; dirty marks changes not yet
     * saved.
     */

    // Identity
    private String meshName;
    private MeshHandle meshHandle;
    private ArpgObjectStruct sourceArpg;

    // Mesh
    private RiggedMeshStruct mesh;

    // Selection
    private BooleanArrayList selection;
    private FloatArrayList selectionWeights;
    private int activeVertex;

    // Mirror
    private IntArrayList mirrorVertices;
    private boolean mirrorStale;

    // Shape
    private int editShape;
    private FloatArrayList shapeBase;

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
        this.selectionWeights = new FloatArrayList();

        // Mirror
        this.mirrorVertices = new IntArrayList();

        // Shape
        this.shapeBase = new FloatArrayList();

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
        this.editShape = EngineSetting.INDEX_NOT_FOUND;
        this.revision++;

        shapeBase.clear();
        selection.clear();
        selectionWeights.clear();
        syncSelection();
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
        selectionWeights.clear();
        selectionWeights.addAll(step.getSelectionWeights());
        syncSelection();

        this.dirty = step.isDirty();
        this.mirrorStale = true;
        this.revision++;
    }

    // Selection changed
    public void markSelectionChanged() {
        this.revision++;
    }

    // Keeps one selection flag and weight per vertex, new vertices unselected, after the vertex count changes; every
    // vertex outside the selection is held whole again
    public void syncSelection() {

        int vertexCount = mesh.getVertexCount();

        selection.size(vertexCount);
        selectionWeights.size(vertexCount);

        for (int i = 0; i < vertexCount; i++)
            if (!selection.getBoolean(i))
                selectionWeights.set(i, 1f);

        if (activeVertex >= vertexCount)
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

    // The shape key being sculpted changed; its base positions are held in getShapeBase()
    public void setEditShape(int editShape) {
        this.editShape = editShape;
        this.revision++;
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

    public FloatArrayList getSelectionWeights() {
        return selectionWeights;
    }

    // How much of a vertex the selection holds, none when it is not selected
    public float getSelectionWeight(int vertexIndex) {
        return selection.getBoolean(vertexIndex) ? selectionWeights.getFloat(vertexIndex) : 0f;
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

    public int getEditShape() {
        return editShape;
    }

    public boolean isEditingShape() {
        return editShape != EngineSetting.INDEX_NOT_FOUND;
    }

    public FloatArrayList getShapeBase() {
        return shapeBase;
    }

    public boolean isDirty() {
        return dirty;
    }

    public int getRevision() {
        return revision;
    }
}
