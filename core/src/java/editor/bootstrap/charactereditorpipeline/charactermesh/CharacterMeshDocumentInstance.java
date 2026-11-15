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
     * and its active vertex, the face corners selected in the UV Editor
     * (cleared whenever the faces are renumbered), the vertices hidden out
     * of the way (never drawn, picked, reached or selected, their faces
     * left out of the preview, kept through edits that keep the vertex
     * count, and every one shown again when an undo or a revert renumbers
     * the mesh), the vertex each mirrors across x, its undo history, and
     * the shape key being sculpted with the base positions it was lifted
     * from. The revision moves on every change of geometry or
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

    // UV Selection — one flag per face corner, four a face
    private BooleanArrayList cornerSelection;

    // Hidden
    private BooleanArrayList hidden;
    private int hiddenCount;

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

        // UV Selection
        this.cornerSelection = new BooleanArrayList();

        // Hidden
        this.hidden = new BooleanArrayList();

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
        cornerSelection.clear();
        hidden.clear();
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

        int vertexCount = mesh.getVertexCount();

        mesh.set(step.getMesh());

        if (mesh.getVertexCount() != vertexCount)
            hidden.clear();

        selection.clear();
        selection.addAll(step.getSelection());
        selectionWeights.clear();
        selectionWeights.addAll(step.getSelectionWeights());
        cornerSelection.clear();
        syncSelection();

        this.dirty = step.isDirty();
        this.mirrorStale = true;
        this.revision++;
    }

    // Selection changed
    public void markSelectionChanged() {
        this.revision++;
    }

    // Keeps one selection flag, weight and hidden flag per vertex, new vertices unselected and shown, after the vertex
    // count changes; every vertex outside the selection is held whole again
    public void syncSelection() {

        int vertexCount = mesh.getVertexCount();

        selection.size(vertexCount);
        selectionWeights.size(vertexCount);
        hidden.size(vertexCount);
        this.hiddenCount = 0;

        for (int i = 0; i < vertexCount; i++) {

            if (!selection.getBoolean(i))
                selectionWeights.set(i, 1f);

            if (hidden.getBoolean(i))
                hiddenCount++;
        }

        if (activeVertex >= vertexCount)
            this.activeVertex = EngineSetting.INDEX_NOT_FOUND;

        syncCornerSelection();
    }

    // One flag per face corner; a change in the face count renumbers the faces, so the corners selected are dropped
    private void syncCornerSelection() {

        int cornerCount = mesh.getFaceCount() * EngineSetting.QUAD_VERTEX_COUNT;

        if (cornerSelection.size() == cornerCount)
            return;

        cornerSelection.clear();
        cornerSelection.size(cornerCount);
    }

    public void setCornerSelected(int faceIndex, int corner, boolean selected) {
        cornerSelection.set(faceIndex * EngineSetting.QUAD_VERTEX_COUNT + corner, selected);
    }

    public void clearCornerSelection() {

        for (int i = 0; i < cornerSelection.size(); i++)
            cornerSelection.set(i, false);
    }

    public void setActiveVertex(int activeVertex) {
        this.activeVertex = activeVertex;
    }

    // Hides a vertex or shows it again; a hidden vertex leaves the selection
    public void setHidden(int vertexIndex, boolean hide) {

        if (hidden.getBoolean(vertexIndex) == hide)
            return;

        hidden.set(vertexIndex, hide);
        hiddenCount += hide ? 1 : -1;

        if (hide)
            selection.set(vertexIndex, false);
    }

    // The hidden flags just written in place over a renumbering, counted again
    public void markHiddenRemapped() {
        syncSelection();
        this.revision++;
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

    public boolean isCornerSelected(int faceIndex, int corner) {
        return cornerSelection.getBoolean(faceIndex * EngineSetting.QUAD_VERTEX_COUNT + corner);
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

    public BooleanArrayList getHidden() {
        return hidden;
    }

    public boolean isHidden(int vertexIndex) {
        return hidden.getBoolean(vertexIndex);
    }

    public boolean hasHidden() {
        return hiddenCount > 0;
    }

    public int getHiddenCount() {
        return hiddenCount;
    }

    // Whether a face stands on a hidden vertex, so it is left out of the preview and hides nothing behind it
    public boolean isFaceHidden(int faceIndex) {

        if (hiddenCount == 0)
            return false;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (hidden.getBoolean(mesh.getFaceVertex(faceIndex, c)))
                return true;

        return false;
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
