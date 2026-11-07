package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSelection;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

class CharacterSelectionBranch extends BranchPackage {

    /*
     * Owns which vertices of a character mesh are selected, and how much of
     * each: picked ones replacing, adding to, taking from or flipping the
     * selection, all or none, the inverse, everything joined to the
     * selection by faces, each selected vertex's mirror, and every vertex a
     * bone weighs on, each taken whole. A brush stroke selects as much of
     * each vertex as the tip covers it, the most any stamp gave, and taking
     * away leaves no more than the tip left uncovered; the transforms and
     * weight edits then act on each vertex as far as it is selected. Also
     * finds the vertex each one mirrors onto across x — the one at its
     * position with x negated on the editor's decimal grid, itself on the
     * centre plane — again whenever the geometry has changed.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;

    // Stroke
    private CharacterEditorSelection strokeSelection;

    // Scratch
    private Long2IntOpenHashMap position2Vertex;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.position2Vertex = new Long2IntOpenHashMap();
        this.position2Vertex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    // Picking \\

    void select(CharacterMeshDocumentInstance document, int vertexIndex, CharacterEditorSelection mode) {

        if (mode == CharacterEditorSelection.REPLACE)
            setAll(document, false);

        if (vertexIndex != EngineSetting.INDEX_NOT_FOUND) {
            apply(document, vertexIndex, mode);
            document.setActiveVertex(vertexIndex);
        }

        changed(document);
    }

    void select(CharacterMeshDocumentInstance document, IntArrayList vertices, CharacterEditorSelection mode) {

        if (mode == CharacterEditorSelection.REPLACE)
            setAll(document, false);

        for (int i = 0; i < vertices.size(); i++)
            apply(document, vertices.getInt(i), mode);

        changed(document);
    }

    // A picked vertex goes in or out whole
    private void apply(CharacterMeshDocumentInstance document, int vertexIndex, CharacterEditorSelection mode) {

        BooleanArrayList selection = document.getSelection();

        selection.set(vertexIndex, switch (mode) {
            case REPLACE, ADD -> true;
            case SUBTRACT -> false;
            case TOGGLE -> !selection.getBoolean(vertexIndex);
        });
        document.getSelectionWeights().set(vertexIndex, 1f);
    }

    // Stroke \\

    // A brush stroke combining with the selection as asked, a stroke that replaces it clearing it first
    void beginStroke(CharacterMeshDocumentInstance document, CharacterEditorSelection mode) {

        this.strokeSelection = mode == CharacterEditorSelection.SUBTRACT
                ? CharacterEditorSelection.SUBTRACT
                : CharacterEditorSelection.ADD;

        if (mode != CharacterEditorSelection.REPLACE)
            return;

        setAll(document, false);
        changed(document);
    }

    // One stamp: each vertex under the tip selected as much as it covers it, or left no more than it leaves uncovered
    void stroke(CharacterMeshDocumentInstance document, IntArrayList vertices, FloatArrayList strengths) {

        BooleanArrayList selection = document.getSelection();
        FloatArrayList weights = document.getSelectionWeights();
        boolean adding = strokeSelection == CharacterEditorSelection.ADD;

        for (int i = 0; i < vertices.size(); i++) {

            int vertexIndex = vertices.getInt(i);
            float strength = strengths.getFloat(i);
            float weight = document.getSelectionWeight(vertexIndex);
            float next = adding ? Math.max(weight, strength) : Math.min(weight, 1f - strength);

            selection.set(vertexIndex, next > 0f);
            weights.set(vertexIndex, next > 0f ? next : 1f);
        }

        document.markSelectionChanged();
    }

    // The stroke's selection handed on once it ends, so the panels list it once rather than on every stamp
    void endStroke() {
        this.strokeSelection = null;
        characterEditorManager.notifyChanged();
    }

    boolean isStroking() {
        return strokeSelection != null;
    }

    // Whole \\

    // Everything when nothing is selected, nothing otherwise
    void toggleAll(CharacterMeshDocumentInstance document) {

        setAll(document, getSelectedCount(document) == 0);
        changed(document);
    }

    void selectAll(CharacterMeshDocumentInstance document, boolean selected) {

        setAll(document, selected);
        changed(document);
    }

    void invert(CharacterMeshDocumentInstance document) {

        BooleanArrayList selection = document.getSelection();

        for (int i = 0; i < selection.size(); i++)
            selection.set(i, !selection.getBoolean(i));

        setWhole(document);
        changed(document);
    }

    private void setAll(CharacterMeshDocumentInstance document, boolean selected) {

        BooleanArrayList selection = document.getSelection();

        for (int i = 0; i < selection.size(); i++)
            selection.set(i, selected);

        setWhole(document);
    }

    // Every vertex held whole, the selection keeping only which vertices it holds
    private void setWhole(CharacterMeshDocumentInstance document) {

        FloatArrayList weights = document.getSelectionWeights();

        for (int i = 0; i < weights.size(); i++)
            weights.set(i, 1f);
    }

    // Grow \\

    // Every vertex joined to the selection through faces, grown face by face until nothing more joins
    void selectLinked(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();
        BooleanArrayList selection = document.getSelection();
        boolean grew = true;

        while (grew) {

            grew = false;

            for (int f = 0; f < mesh.getFaceCount(); f++)
                if (touchesSelection(mesh, f, selection))
                    grew |= selectFace(mesh, f, selection);
        }

        changed(document);
    }

    private boolean touchesSelection(RiggedMeshStruct mesh, int faceIndex, BooleanArrayList selection) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (selection.getBoolean(mesh.getFaceVertex(faceIndex, c)))
                return true;

        return false;
    }

    private boolean selectFace(RiggedMeshStruct mesh, int faceIndex, BooleanArrayList selection) {

        boolean grew = false;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int vertexIndex = mesh.getFaceVertex(faceIndex, c);

            if (!selection.getBoolean(vertexIndex)) {
                selection.set(vertexIndex, true);
                grew = true;
            }
        }

        return grew;
    }

    // Adds each selected vertex's mirror across x
    void selectMirror(CharacterMeshDocumentInstance document) {

        extendMirror(document);
        changed(document);
    }

    void extendMirror(CharacterMeshDocumentInstance document) {

        IntArrayList mirrorVertices = resolveMirror(document);
        BooleanArrayList selection = document.getSelection();
        FloatArrayList weights = document.getSelectionWeights();

        for (int i = 0; i < selection.size(); i++) {

            int mirror = mirrorVertices.getInt(i);

            if (!selection.getBoolean(i) || mirror == EngineSetting.INDEX_NOT_FOUND)
                continue;

            weights.set(mirror, Math.max(document.getSelectionWeight(mirror), weights.getFloat(i)));
            selection.set(mirror, true);
        }
    }

    // Exactly the vertices a bone weighs on at least the selection threshold
    void selectBone(CharacterMeshDocumentInstance document, int boneIndex) {

        RiggedMeshStruct mesh = document.getMesh();
        BooleanArrayList selection = document.getSelection();

        for (int i = 0; i < mesh.getVertexCount(); i++)
            selection.set(i, mesh.getBoneWeight(i, boneIndex) >= EditorSetting.CHARACTER_MESH_BONE_SELECT_WEIGHT);

        setWhole(document);
        changed(document);
    }

    // A changed selection kept to the region of the tab worked in last, then marked changed
    private void changed(CharacterMeshDocumentInstance document) {

        BooleanArrayList selection = document.getSelection();

        if (!characterEditorManager.getRegion().isWhole())
            for (int i = 0; i < selection.size(); i++)
                if (selection.getBoolean(i) && !characterEditorManager.isInRegion(document, i))
                    selection.set(i, false);

        document.markSelectionChanged();
        characterEditorManager.notifyChanged();
    }

    // Mirror \\

    // The vertex each vertex mirrors onto across x, or INDEX_NOT_FOUND when none stands there
    IntArrayList resolveMirror(CharacterMeshDocumentInstance document) {

        IntArrayList mirrorVertices = document.getMirrorVertices();

        if (!document.isMirrorStale())
            return mirrorVertices;

        RiggedMeshStruct mesh = document.getMesh();
        int vertexCount = mesh.getVertexCount();

        position2Vertex.clear();
        mirrorVertices.clear();

        for (int i = 0; i < vertexCount; i++)
            position2Vertex.putIfAbsent(
                    CharacterMeshUtility.toPositionKey(mesh.getX(i), mesh.getY(i), mesh.getZ(i)), i);

        for (int i = 0; i < vertexCount; i++)
            mirrorVertices.add(position2Vertex.get(
                    CharacterMeshUtility.toPositionKey(-mesh.getX(i), mesh.getY(i), mesh.getZ(i))));

        document.markMirrorResolved();
        return mirrorVertices;
    }

    // Measure \\

    int getSelectedCount(CharacterMeshDocumentInstance document) {

        BooleanArrayList selection = document.getSelection();
        int count = 0;

        for (int i = 0; i < selection.size(); i++)
            if (selection.getBoolean(i))
                count++;

        return count;
    }

    // The position of the selected vertices averaged by how much of each is selected; false when none is selected
    boolean resolveCenter(CharacterMeshDocumentInstance document, Vector3 center) {

        RiggedMeshStruct mesh = document.getMesh();
        BooleanArrayList selection = document.getSelection();
        float total = 0f;

        center.set(0f, 0f, 0f);

        for (int i = 0; i < selection.size(); i++) {

            float weight = document.getSelectionWeight(i);

            if (weight <= 0f)
                continue;

            center.add(mesh.getX(i) * weight, mesh.getY(i) * weight, mesh.getZ(i) * weight);
            total += weight;
        }

        if (total <= 0f)
            return false;

        center.divide(total);
        return true;
    }

    // The box the selected vertices span; false when none is selected
    boolean resolveBounds(CharacterMeshDocumentInstance document, Vector3 min, Vector3 max) {

        RiggedMeshStruct mesh = document.getMesh();
        BooleanArrayList selection = document.getSelection();
        boolean found = false;

        for (int i = 0; i < selection.size(); i++) {

            if (!selection.getBoolean(i))
                continue;

            float x = mesh.getX(i);
            float y = mesh.getY(i);
            float z = mesh.getZ(i);

            if (!found) {
                min.set(x, y, z);
                max.set(x, y, z);
                found = true;
                continue;
            }

            min.set(Math.min(min.x, x), Math.min(min.y, y), Math.min(min.z, z));
            max.set(Math.max(max.x, x), Math.max(max.y, y), Math.max(max.z, z));
        }

        return found;
    }
}
