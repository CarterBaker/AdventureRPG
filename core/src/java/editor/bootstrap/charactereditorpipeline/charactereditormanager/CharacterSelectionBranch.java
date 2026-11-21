package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSelection;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.bootstrap.charactereditorpipeline.util.CharacterRegionStruct;
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
     * selection by faces, the selection grown or shrunk by one ring of faces,
     * the edge loop running through an edge, as Blender walks one across
     * quads, each selected vertex's mirror, and every vertex a bone weighs
     * on, each taken whole. A brush stroke selects as much of
     * each vertex as the tip covers it, the most any stamp gave, and taking
     * away leaves no more than the tip left uncovered; the transforms and
     * weight edits then act on each vertex as far as it is selected. Also
     * finds the vertex each one mirrors onto across x — the one at its
     * position with x negated on the editor's decimal grid, itself on the
     * centre plane, and for a vertex with no such twin the nearest one
     * likewise left alone across x within the mirror reach, so a mesh not
     * modelled exactly symmetric still mirrors every edit — again whenever
     * the geometry has changed. Hides the selected or the unselected
     * vertices out of the way and reveals them again, as a modelling tool
     * hides geometry to reach into tight spaces, and a whole region at once
     * by its eye in the Layers tab; a hidden vertex never stays selected.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;

    // Stroke
    private CharacterEditorSelection strokeSelection;

    // Scratch — the selection as it stood before a ring was grown or shrunk, a loop's vertices, and every vertex's
    // faces laid out one run after another
    private BooleanArrayList ringScratch;
    private IntArrayList loopScratch;
    private IntArrayList vertexFaceStarts;
    private IntArrayList vertexFaces;

    // Scratch — vertices on one point of the decimal grid chained in index order, each one's place in its chain
    private Long2IntOpenHashMap position2Vertex;
    private IntArrayList positionNext;
    private Long2IntOpenHashMap position2Count;
    private Long2IntOpenHashMap cell2Vertex;
    private IntArrayList cellNext;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.ringScratch = new BooleanArrayList();
        this.loopScratch = new IntArrayList();
        this.vertexFaceStarts = new IntArrayList();
        this.vertexFaces = new IntArrayList();
        this.position2Vertex = new Long2IntOpenHashMap();
        this.position2Vertex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.positionNext = new IntArrayList();
        this.position2Count = new Long2IntOpenHashMap();
        this.cell2Vertex = new Long2IntOpenHashMap();
        this.cell2Vertex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.cellNext = new IntArrayList();
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

    // One ring of faces about the selection joined to it, or its rim let go, as Blender's Select More and Less
    void growRing(CharacterMeshDocumentInstance document, boolean grow) {

        RiggedMeshStruct mesh = document.getMesh();
        BooleanArrayList selection = document.getSelection();

        ringScratch.size(selection.size());

        for (int i = 0; i < selection.size(); i++)
            ringScratch.set(i, selection.getBoolean(i));

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            if (grow ? !touchesSelection(mesh, f, ringScratch) : !touchesUnselected(mesh, f, ringScratch))
                continue;

            for (int c = 0; c < mesh.getFaceCornerCount(f); c++)
                selection.set(mesh.getFaceVertex(f, c), grow);
        }

        setWhole(document);
        changed(document);
    }

    private boolean touchesUnselected(RiggedMeshStruct mesh, int faceIndex, BooleanArrayList selection) {

        for (int c = 0; c < mesh.getFaceCornerCount(faceIndex); c++)
            if (!selection.getBoolean(mesh.getFaceVertex(faceIndex, c)))
                return true;

        return false;
    }

    private boolean touchesSelection(RiggedMeshStruct mesh, int faceIndex, BooleanArrayList selection) {

        for (int c = 0; c < mesh.getFaceCornerCount(faceIndex); c++)
            if (selection.getBoolean(mesh.getFaceVertex(faceIndex, c)))
                return true;

        return false;
    }

    private boolean selectFace(RiggedMeshStruct mesh, int faceIndex, BooleanArrayList selection) {

        boolean grew = false;

        for (int c = 0; c < mesh.getFaceCornerCount(faceIndex); c++) {

            int vertexIndex = mesh.getFaceVertex(faceIndex, c);

            if (!selection.getBoolean(vertexIndex)) {
                selection.set(vertexIndex, true);
                grew = true;
            }
        }

        return grew;
    }

    // Loop \\

    // The edge loop through an edge, walked both ways from it as Blender walks one: across each vertex four quads
    // meet at, on to the edge sharing no face with the one it came by, until it reaches a pole, the rim or itself
    void selectLoop(CharacterMeshDocumentInstance document, int vertexA, int vertexB, CharacterEditorSelection mode) {

        RiggedMeshStruct mesh = document.getMesh();

        indexVertexFaces(mesh);
        loopScratch.clear();
        loopScratch.add(vertexA);
        loopScratch.add(vertexB);
        walkLoop(mesh, vertexA, vertexB);
        walkLoop(mesh, vertexB, vertexA);
        select(document, loopScratch, mode);
    }

    private void walkLoop(RiggedMeshStruct mesh, int from, int at) {

        int previous = from;
        int current = at;

        while (true) {

            int next = resolveLoopNext(mesh, previous, current);

            if (next == EngineSetting.INDEX_NOT_FOUND || loopScratch.contains(next))
                return;

            loopScratch.add(next);
            previous = current;
            current = next;
        }
    }

    // Across a vertex four quads meet at, the one neighbour sharing no face with the vertex it was reached from
    private int resolveLoopNext(RiggedMeshStruct mesh, int previous, int vertex) {

        int start = vertexFaceStarts.getInt(vertex);
        int end = vertexFaceStarts.getInt(vertex + 1);

        if (end - start != EditorSetting.CHARACTER_LOOP_VALENCE)
            return EngineSetting.INDEX_NOT_FOUND;

        int found = EngineSetting.INDEX_NOT_FOUND;

        for (int i = start; i < end; i++) {

            int face = vertexFaces.getInt(i);
            int corners = mesh.getFaceCornerCount(face);

            if (corners != EditorSetting.CHARACTER_LOOP_VALENCE)
                return EngineSetting.INDEX_NOT_FOUND;

            if (holdsVertex(mesh, face, previous))
                continue;

            int corner = findCorner(mesh, face, vertex);

            for (int side = -1; side <= 1; side += 2) {

                int neighbour = mesh.getFaceVertex(face, Math.floorMod(corner + side, corners));

                if (!sharesFaceWith(mesh, vertex, neighbour, previous))
                    found = neighbour;
            }
        }

        return found;
    }

    // Whether any face about a vertex holds both of two others
    private boolean sharesFaceWith(RiggedMeshStruct mesh, int vertex, int a, int b) {

        for (int i = vertexFaceStarts.getInt(vertex); i < vertexFaceStarts.getInt(vertex + 1); i++) {

            int face = vertexFaces.getInt(i);

            if (holdsVertex(mesh, face, a) && holdsVertex(mesh, face, b))
                return true;
        }

        return false;
    }

    private boolean holdsVertex(RiggedMeshStruct mesh, int faceIndex, int vertexIndex) {
        return findCorner(mesh, faceIndex, vertexIndex) != EngineSetting.INDEX_NOT_FOUND;
    }

    private int findCorner(RiggedMeshStruct mesh, int faceIndex, int vertexIndex) {

        for (int c = 0; c < mesh.getFaceCornerCount(faceIndex); c++)
            if (mesh.getFaceVertex(faceIndex, c) == vertexIndex)
                return c;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Every vertex's faces, one run per vertex in index order, its run starting where vertexFaceStarts says
    private void indexVertexFaces(RiggedMeshStruct mesh) {

        int vertexCount = mesh.getVertexCount();

        vertexFaceStarts.size(vertexCount + 1);

        for (int v = 0; v <= vertexCount; v++)
            vertexFaceStarts.set(v, 0);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < mesh.getFaceCornerCount(f); c++)
                vertexFaceStarts.set(mesh.getFaceVertex(f, c) + 1,
                        vertexFaceStarts.getInt(mesh.getFaceVertex(f, c) + 1) + 1);

        for (int v = 0; v < vertexCount; v++)
            vertexFaceStarts.set(v + 1, vertexFaceStarts.getInt(v + 1) + vertexFaceStarts.getInt(v));

        vertexFaces.size(vertexFaceStarts.getInt(vertexCount));

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < mesh.getFaceCornerCount(f); c++) {

                int vertex = mesh.getFaceVertex(f, c);
                int slot = vertexFaceStarts.getInt(vertex);

                vertexFaces.set(slot, f);
                vertexFaceStarts.set(vertex, slot + 1);
            }

        for (int v = vertexCount; v > 0; v--)
            vertexFaceStarts.set(v, vertexFaceStarts.getInt(v - 1));

        vertexFaceStarts.set(0, 0);
    }

    // Mirror Selection \\

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
    // The selection kept to the region and to the vertices shown, then told to every view
    private void changed(CharacterMeshDocumentInstance document) {

        BooleanArrayList selection = document.getSelection();
        boolean whole = characterEditorManager.getRegion().isWhole();

        if (!whole || document.hasHidden())
            for (int i = 0; i < selection.size(); i++)
                if (selection.getBoolean(i)
                        && (document.isHidden(i) || !whole && !characterEditorManager.isInRegion(document, i)))
                    selection.set(i, false);

        document.markSelectionChanged();
        characterEditorManager.notifyChanged();
    }

    // Hide \\

    // Hides every selected vertex, or every unselected one, so long as something stays shown; false, after saying
    // why, when nothing would be hidden or nothing left
    boolean hide(CharacterMeshDocumentInstance document, boolean selected) {

        int vertexCount = document.getMesh().getVertexCount();
        int hiding = 0;

        for (int i = 0; i < vertexCount; i++)
            if (!document.isHidden(i) && document.isSelected(i) == selected)
                hiding++;

        if (hiding == 0) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_HIDE_NONE);
            return false;
        }

        if (document.getHiddenCount() + hiding >= vertexCount) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_HIDE_ALL);
            return false;
        }

        for (int i = 0; i < vertexCount; i++)
            if (!document.isHidden(i) && document.isSelected(i) == selected)
                document.setHidden(i, true);

        changed(document);
        return true;
    }

    // Whether a region holds vertices and every one of them is hidden, as its eye in the Layers tab shows it shut
    boolean isRegionHidden(CharacterMeshDocumentInstance document, CharacterRegionStruct region) {

        boolean holds = false;

        for (int i = 0; i < document.getMesh().getVertexCount(); i++) {

            if (!characterEditorManager.isInRegion(document, region, i))
                continue;

            if (!document.isHidden(i))
                return false;

            holds = true;
        }

        return holds;
    }

    // A region's vertices hidden all together, or every one shown again, as its eye in the Layers tab turns; never the
    // last vertices shown. False when nothing changed
    boolean setRegionHidden(CharacterMeshDocumentInstance document, CharacterRegionStruct region, boolean hidden) {

        int vertexCount = document.getMesh().getVertexCount();
        int changing = 0;

        for (int i = 0; i < vertexCount; i++)
            if (document.isHidden(i) != hidden && characterEditorManager.isInRegion(document, region, i))
                changing++;

        if (changing == 0)
            return false;

        if (hidden && document.getHiddenCount() + changing >= vertexCount) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_HIDE_ALL);
            return false;
        }

        for (int i = 0; i < vertexCount; i++)
            if (document.isHidden(i) != hidden && characterEditorManager.isInRegion(document, region, i))
                document.setHidden(i, hidden);

        changed(document);
        return true;
    }

    // Shows every hidden vertex again, selected, as it comes back; false when nothing was hidden
    boolean reveal(CharacterMeshDocumentInstance document) {

        if (!document.hasHidden())
            return false;

        BooleanArrayList selection = document.getSelection();

        setAll(document, false);

        for (int i = 0; i < selection.size(); i++)
            if (document.isHidden(i)) {
                document.setHidden(i, false);
                selection.set(i, true);
            }

        changed(document);
        return true;
    }

    // Mirror \\

    // The vertex each vertex mirrors onto across x, or INDEX_NOT_FOUND when none stands there. Vertices standing on
    // one point (an extrusion or a copy before it moves) pair in index order with those on the mirrored point, so an
    // original pairs with the original and a copy with the copy
    IntArrayList resolveMirror(CharacterMeshDocumentInstance document) {

        IntArrayList mirrorVertices = document.getMirrorVertices();

        if (!document.isMirrorStale())
            return mirrorVertices;

        RiggedMeshStruct mesh = document.getMesh();
        int vertexCount = mesh.getVertexCount();

        position2Vertex.clear();
        position2Count.clear();
        positionNext.clear();
        positionNext.size(vertexCount);
        mirrorVertices.clear();

        for (int i = vertexCount - 1; i >= 0; i--)
            positionNext.set(i, position2Vertex.put(
                    CharacterMeshUtility.toPositionKey(mesh.getX(i), mesh.getY(i), mesh.getZ(i)), i));

        for (int i = 0; i < vertexCount; i++) {

            int rank = position2Count.addTo(
                    CharacterMeshUtility.toPositionKey(mesh.getX(i), mesh.getY(i), mesh.getZ(i)), 1);
            int twin = position2Vertex.get(
                    CharacterMeshUtility.toPositionKey(-mesh.getX(i), mesh.getY(i), mesh.getZ(i)));

            for (int r = 0; r < rank && twin != EngineSetting.INDEX_NOT_FOUND; r++)
                twin = positionNext.getInt(twin);

            mirrorVertices.add(twin);
        }

        pairNearest(mesh, mirrorVertices);
        document.markMirrorResolved();
        return mirrorVertices;
    }

    // Every vertex off the centre plane the exact pass left without a twin paired with the nearest one across x also
    // left without one, within the mirror reach; vertices are binned by reach-sized cells so each looks only about its
    // mirrored point, and every pair is made both ways
    private void pairNearest(RiggedMeshStruct mesh, IntArrayList mirrorVertices) {

        float reach = EditorSetting.CHARACTER_MESH_MIRROR_REACH;
        float tolerance = EditorSetting.CHARACTER_MESH_CENTER_TOLERANCE;

        cell2Vertex.clear();
        cellNext.clear();
        cellNext.size(mesh.getVertexCount());

        for (int v = 0; v < mesh.getVertexCount(); v++)
            if (mirrorVertices.getInt(v) == EngineSetting.INDEX_NOT_FOUND && mesh.getX(v) < -tolerance)
                cellNext.set(v, cell2Vertex.put(
                        CharacterMeshUtility.toCellKey(mesh.getX(v), mesh.getY(v), mesh.getZ(v), reach), v));

        for (int v = 0; v < mesh.getVertexCount(); v++) {

            if (mirrorVertices.getInt(v) != EngineSetting.INDEX_NOT_FOUND || mesh.getX(v) <= tolerance)
                continue;

            int twin = findNearestUnpaired(mesh, mirrorVertices, -mesh.getX(v), mesh.getY(v), mesh.getZ(v), reach);

            if (twin == EngineSetting.INDEX_NOT_FOUND)
                continue;

            mirrorVertices.set(v, twin);
            mirrorVertices.set(twin, v);
        }

        for (int v = 0; v < mesh.getVertexCount(); v++)
            if (mirrorVertices.getInt(v) == EngineSetting.INDEX_NOT_FOUND && Math.abs(mesh.getX(v)) <= tolerance)
                mirrorVertices.set(v, v);
    }

    private int findNearestUnpaired(
            RiggedMeshStruct mesh,
            IntArrayList mirrorVertices,
            float x,
            float y,
            float z,
            float reach) {

        int cellX = (int) Math.floor(x / reach);
        int cellY = (int) Math.floor(y / reach);
        int cellZ = (int) Math.floor(z / reach);
        float nearest = reach * reach;
        int found = EngineSetting.INDEX_NOT_FOUND;

        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    for (int v = cell2Vertex.get(CharacterMeshUtility.packCell(cellX + dx, cellY + dy, cellZ + dz));
                            v != EngineSetting.INDEX_NOT_FOUND;
                            v = cellNext.getInt(v)) {

                        if (mirrorVertices.getInt(v) != EngineSetting.INDEX_NOT_FOUND)
                            continue;

                        float ex = mesh.getX(v) - x;
                        float ey = mesh.getY(v) - y;
                        float ez = mesh.getZ(v) - z;
                        float distance = ex * ex + ey * ey + ez * ez;

                        if (distance <= nearest) {
                            nearest = distance;
                            found = v;
                        }
                    }

        return found;
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
