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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

class CharacterSelectionBranch extends BranchPackage {

    /*
     * Owns which vertices of a character mesh are selected: picked ones
     * replacing, adding to, taking from or flipping the selection, all or
     * none, the inverse, everything joined to the selection by faces, each
     * selected vertex's mirror, and every vertex a bone weighs on. Also finds
     * the vertex each one mirrors onto across x — the one at its position
     * with x negated on the editor's decimal grid, itself on the centre
     * plane — again whenever the geometry has changed.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;

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
            setAll(document.getSelection(), false);

        if (vertexIndex != EngineSetting.INDEX_NOT_FOUND) {
            apply(document.getSelection(), vertexIndex, mode);
            document.setActiveVertex(vertexIndex);
        }

        changed(document);
    }

    void select(CharacterMeshDocumentInstance document, IntArrayList vertices, CharacterEditorSelection mode) {

        BooleanArrayList selection = document.getSelection();

        if (mode == CharacterEditorSelection.REPLACE)
            setAll(selection, false);

        for (int i = 0; i < vertices.size(); i++)
            apply(selection, vertices.getInt(i), mode);

        changed(document);
    }

    private void apply(BooleanArrayList selection, int vertexIndex, CharacterEditorSelection mode) {
        selection.set(vertexIndex, switch (mode) {
            case REPLACE, ADD -> true;
            case SUBTRACT -> false;
            case TOGGLE -> !selection.getBoolean(vertexIndex);
        });
    }

    // Whole \\

    // Everything when nothing is selected, nothing otherwise
    void toggleAll(CharacterMeshDocumentInstance document) {

        setAll(document.getSelection(), getSelectedCount(document) == 0);
        changed(document);
    }

    void selectAll(CharacterMeshDocumentInstance document, boolean selected) {

        setAll(document.getSelection(), selected);
        changed(document);
    }

    void invert(CharacterMeshDocumentInstance document) {

        BooleanArrayList selection = document.getSelection();

        for (int i = 0; i < selection.size(); i++)
            selection.set(i, !selection.getBoolean(i));

        changed(document);
    }

    private void setAll(BooleanArrayList selection, boolean selected) {

        for (int i = 0; i < selection.size(); i++)
            selection.set(i, selected);
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

        for (int i = 0; i < selection.size(); i++) {

            int mirror = mirrorVertices.getInt(i);

            if (selection.getBoolean(i) && mirror != EngineSetting.INDEX_NOT_FOUND)
                selection.set(mirror, true);
        }
    }

    // Exactly the vertices a bone weighs on at least the selection threshold
    void selectBone(CharacterMeshDocumentInstance document, int boneIndex) {

        RiggedMeshStruct mesh = document.getMesh();
        BooleanArrayList selection = document.getSelection();

        for (int i = 0; i < mesh.getVertexCount(); i++)
            selection.set(i, mesh.getBoneWeight(i, boneIndex) >= EditorSetting.CHARACTER_MESH_BONE_SELECT_WEIGHT);

        changed(document);
    }

    private void changed(CharacterMeshDocumentInstance document) {

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

    // The average position of the selected vertices; false when none is selected
    boolean resolveCenter(CharacterMeshDocumentInstance document, Vector3 center) {

        RiggedMeshStruct mesh = document.getMesh();
        BooleanArrayList selection = document.getSelection();
        int count = 0;

        center.set(0f, 0f, 0f);

        for (int i = 0; i < selection.size(); i++)
            if (selection.getBoolean(i)) {
                center.add(mesh.getX(i), mesh.getY(i), mesh.getZ(i));
                count++;
            }

        if (count == 0)
            return false;

        center.divide(count);
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
