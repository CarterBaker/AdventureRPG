package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterStampStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

class CharacterTopologyBranch extends BranchPackage {

    /*
     * The topology brush: more faces where it strokes, or fewer with the
     * stroke inverted, the mesh kept all quads either way, evened toward the
     * Detail size as Blender's dynamic topology evens its edges: a plain
     * stroke splits only the faces it covers that are larger than Detail by
     * half again, an inverted one merges only those smaller than three
     * quarters of it, so a stroke leaves faces near Detail alone and strokes
     * over an area settle it at that size instead of changing it on and on.
     * A stroke opens one undo step and gathers how far the brush covers each
     * vertex; once a frame the faces it covers are changed and the part
     * shown again.
     * Refining follows Schneiders' 3-refinement, as adaptive all-quad meshing
     * does: each covered face splits into nine on a lattice of thirds, and
     * every face beside one is split by the transition template its refined
     * edges call for (one edge, two meeting at a corner, two opposite), a
     * face left with three or four refined edges joining the refined set
     * until none is, so no edge is left with a hanging vertex. Reducing
     * follows quad-mesh simplification (Tarini et al.): the smallest covered
     * faces collapse across the diagonal that leaves the vertices about them
     * nearest four faces each (the shorter on a tie) into its middle, refused
     * where the collapse would pinch the surface, fold a face, or cross an
     * open edge or a UV seam, and a vertex left between just two faces that
     * share both its edges is dissolved with them. New vertices take their
     * position, weights, masks and shape-key deltas blended from the corners
     * they lie between, every face its UVs interpolated across its own
     * corners, its texture and its zone; a refined seam stays a seam. With
     * mirror on, the brush covers each vertex's twin as well. Faces made by a
     * stroke are not refined again by it, and the vertices a collapse leaves
     * unused go when the stroke ends. Reducing walks the faces about each
     * vertex through an adjacency made once a frame, never the whole mesh,
     * and the vertices locked on open edges and seams are found once a stroke
     * until a refine adds more.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterMeshBranch characterMeshBranch;
    private CharacterHistoryBranch characterHistoryBranch;
    private CharacterSelectionBranch characterSelectionBranch;
    private CharacterSculptBranch characterSculptBranch;
    private CharacterWeightBranch characterWeightBranch;
    private MeshManager meshManager;

    // Stroke
    private CharacterMeshDocumentInstance strokeDocument;
    private FloatArrayList coverage;
    private BooleanArrayList freshFaces;
    private boolean pending;
    private boolean inverted;
    private boolean changed;
    private float detail;

    // Faces — the refined or removed set, each face's refined edges, the refined edges and the first of the two
    // vertices each is cut at, the one nearer its lower vertex
    private BooleanArrayList faceFlags;
    private IntArrayList faceMasks;
    private LongOpenHashSet splitEdges;
    private Long2IntOpenHashMap edge2Point;

    // Lattice — the vertex at each point of the face being laid, its corners and their UVs turned to the template
    private int[] latticeVertices;
    private int[] corners;
    private float[] cornerUVs;
    private int[] cornerPins;

    // Blend
    private IntArrayList blendVertices;
    private FloatArrayList blendShares;
    private float[] maskScratch;

    // Reduce
    private IntArrayList candidates;
    private FloatArrayList faceAreas;
    private IntComparator areaOrder;
    private IntArrayList ringA;
    private IntArrayList ringB;
    private IntOpenHashSet linkVertices;
    private BooleanArrayList lockedVertices;
    private boolean lockedStale;
    private Long2IntOpenHashMap edgeCounts;

    // Adjacency — the faces about each vertex, chained an entry per corner; a collapse that moves a corner adds an
    // entry, and an entry whose face is gone or no longer stands on its vertex is passed over
    private IntArrayList vertex2FirstEntry;
    private IntArrayList entryFaces;
    private IntArrayList entryNext;

    // Scratch
    private Vector3 merged;
    private Vector3 before;
    private Vector3 after;
    private Vector3 diagonalA;
    private Vector3 diagonalB;
    private Vector3[] quadCorners;

    // Base \\

    @Override
    protected void create() {

        int lattice = EditorSetting.CHARACTER_TOPOLOGY_LATTICE + 1;

        // Stroke
        this.coverage = new FloatArrayList();
        this.freshFaces = new BooleanArrayList();

        // Faces
        this.faceFlags = new BooleanArrayList();
        this.faceMasks = new IntArrayList();
        this.splitEdges = new LongOpenHashSet();
        this.edge2Point = new Long2IntOpenHashMap();
        edge2Point.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        // Lattice
        this.latticeVertices = new int[lattice * lattice];
        this.corners = new int[EngineSetting.QUAD_VERTEX_COUNT];
        this.cornerUVs = new float[EngineSetting.QUAD_VERTEX_COUNT * 2];
        this.cornerPins = new int[EngineSetting.QUAD_VERTEX_COUNT];

        // Blend
        this.blendVertices = new IntArrayList();
        this.blendShares = new FloatArrayList();
        this.maskScratch = new float[EngineSetting.RIGGED_MESH_MASK_CHANNELS.length];

        // Reduce
        this.candidates = new IntArrayList();
        this.faceAreas = new FloatArrayList();
        this.areaOrder = (first, second) -> Float.compare(faceAreas.getFloat(first), faceAreas.getFloat(second));
        this.ringA = new IntArrayList();
        this.ringB = new IntArrayList();
        this.linkVertices = new IntOpenHashSet();
        this.lockedVertices = new BooleanArrayList();
        this.edgeCounts = new Long2IntOpenHashMap();

        // Adjacency
        this.vertex2FirstEntry = new IntArrayList();
        this.entryFaces = new IntArrayList();
        this.entryNext = new IntArrayList();

        // Scratch
        this.merged = new Vector3();
        this.before = new Vector3();
        this.after = new Vector3();
        this.diagonalA = new Vector3();
        this.diagonalB = new Vector3();
        this.quadCorners = new Vector3[EngineSetting.QUAD_VERTEX_COUNT];

        for (int c = 0; c < quadCorners.length; c++)
            quadCorners[c] = new Vector3();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterMeshBranch = get(CharacterMeshBranch.class);
        this.characterHistoryBranch = get(CharacterHistoryBranch.class);
        this.characterSelectionBranch = get(CharacterSelectionBranch.class);
        this.characterSculptBranch = get(CharacterSculptBranch.class);
        this.characterWeightBranch = get(CharacterWeightBranch.class);
        this.meshManager = get(MeshManager.class);
    }

    // Stroke \\

    // Opens the stroke's undo step, refused while a shape key is sculpted, since its deltas are laid over the base
    void beginStroke(CharacterMeshDocumentInstance document) {

        if (document.isEditingShape()) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_TOPOLOGY_SHAPE);
            return;
        }

        this.strokeDocument = document;
        this.pending = false;
        this.changed = false;
        this.lockedStale = true;

        characterHistoryBranch.begin(document);
        coverage.clear();
        coverage.size(document.getMesh().getVertexCount());
        freshFaces.clear();
        freshFaces.size(document.getMesh().getFaceCount());
    }

    boolean isStroking() {
        return strokeDocument != null;
    }

    // One stamp's cover of each vertex under the brush, and of its twin with mirror on, kept at its most until the
    // frame's change
    void stroke(
            CharacterMeshDocumentInstance document,
            IntArrayList vertices,
            FloatArrayList strengths,
            CharacterStampStruct stamp) {

        if (document != strokeDocument)
            return;

        IntArrayList mirrors = characterEditorManager.isMirror()
                ? characterSelectionBranch.resolveMirror(document)
                : null;

        coverage.size(document.getMesh().getVertexCount());

        for (int i = 0; i < vertices.size(); i++) {

            int vertexIndex = vertices.getInt(i);
            float strength = strengths.getFloat(i);

            cover(vertexIndex, strength);

            if (mirrors != null && vertexIndex < mirrors.size())
                cover(mirrors.getInt(vertexIndex), strength);
        }

        this.inverted = stamp.isInverted();
        this.pending = true;
    }

    private void cover(int vertexIndex, float strength) {

        if (vertexIndex != EngineSetting.INDEX_NOT_FOUND && strength > coverage.getFloat(vertexIndex))
            coverage.set(vertexIndex, strength);
    }

    // The frame's change to the faces the brush covered since the last, true where the mesh changed
    boolean apply() {

        if (!pending || strokeDocument == null)
            return false;

        CharacterMeshDocumentInstance document = strokeDocument;
        RiggedMeshStruct mesh = document.getMesh();

        this.pending = false;
        this.detail = characterEditorManager.getTopologyDetail();
        collectCovered(mesh);
        coverage.clear();
        coverage.size(mesh.getVertexCount());

        if (candidates.isEmpty())
            return false;

        boolean applied = inverted ? reduce(document) : refine(document);

        if (applied) {
            this.changed = true;
            document.syncSelection();
        }

        return applied;
    }

    void endStroke(CharacterMeshDocumentInstance document) {

        if (document == strokeDocument && document.hasOpenStep()) {

            if (changed) {
                characterSculptBranch.removeOrphans(document);
                characterMeshBranch.finishEdit(document);
            } else
                characterHistoryBranch.cancel(document);
        }

        this.strokeDocument = null;
        this.pending = false;
        coverage.clear();
        freshFaces.clear();
    }

    // Every face whose corners the brush all covers, at least the face coverage on average, that this stroke has not
    // made and that is off the Detail size the way the stroke changes it: larger for a plain stroke, smaller inverted
    private void collectCovered(RiggedMeshStruct mesh) {

        candidates.clear();
        freshFaces.size(mesh.getFaceCount());

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            if (freshFaces.getBoolean(f) || mesh.isTriangle(f))
                continue;

            float total = 0f;
            boolean whole = true;

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT && whole; c++) {

                float covered = coverage.getFloat(mesh.getFaceVertex(f, c));

                whole = covered > 0f;
                total += covered;
            }

            if (whole && total >= EditorSetting.CHARACTER_TOPOLOGY_FACE_COVERAGE * EngineSetting.QUAD_VERTEX_COUNT
                    && isOffDetail(mesh, f))
                candidates.add(f);
        }
    }

    // A face's size, the side of a square of its area, against the Detail size the stroke evens toward
    private boolean isOffDetail(RiggedMeshStruct mesh, int faceIndex) {

        resolveFaceNormal(mesh, faceIndex, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND, before);

        float size = (float) Math.sqrt(before.length());

        return inverted
                ? size < detail * EditorSetting.CHARACTER_TOPOLOGY_MERGE_BELOW
                : size > detail * EditorSetting.CHARACTER_TOPOLOGY_SPLIT_ABOVE;
    }

    // Refine \\

    // Splits the covered faces into nine and the faces beside them by their transition templates, refused where the
    // result would pass the mesh limit
    private boolean refine(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();
        int faceCount = mesh.getFaceCount();

        faceFlags.clear();
        faceFlags.size(faceCount);

        for (int i = 0; i < candidates.size(); i++)
            faceFlags.set(candidates.getInt(i), true);

        closeRefinement(mesh);

        if (touchesTriangle(mesh)) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_TRIANGLE);
            return false;
        }

        if (!meshManager.fitsRiggedMeshLimit(faceCount + countAddedFaces(faceCount))) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_MESH_LIMIT);
            return false;
        }

        cutEdges(mesh);

        for (int f = 0; f < faceCount; f++)
            if (faceMasks.getInt(f) != 0)
                layFace(mesh, f);

        this.lockedStale = true;
        return true;
    }

    // Grows the refined set until every face outside it has a transition template for its refined edges: one, two
    // meeting at a corner, or two opposite
    private void closeRefinement(RiggedMeshStruct mesh) {

        int faceCount = mesh.getFaceCount();
        boolean grown = true;

        faceMasks.clear();
        faceMasks.size(faceCount);

        while (grown) {

            grown = false;
            collectSplitEdges(mesh);

            for (int f = 0; f < faceCount; f++) {

                int mask = resolveMask(mesh, f);

                faceMasks.set(f, mask);

                if (!faceFlags.getBoolean(f) && Integer.bitCount(mask) > 2) {
                    faceFlags.set(f, true);
                    grown = true;
                }
            }
        }
    }

    // Whether the refinement would split an edge of a triangle, which the quad templates cannot lay
    private boolean touchesTriangle(RiggedMeshStruct mesh) {

        for (int f = 0; f < faceMasks.size(); f++)
            if (faceMasks.getInt(f) != 0 && mesh.isTriangle(f))
                return true;

        return false;
    }

    private void collectSplitEdges(RiggedMeshStruct mesh) {

        splitEdges.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (faceFlags.getBoolean(f))
                for (int c = 0; c < mesh.getFaceCornerCount(f); c++)
                    splitEdges.add(RiggedMeshStruct.toEdgeKey(
                            mesh.getFaceVertex(f, c),
                            mesh.getFaceVertex(f, mesh.getNextCorner(f, c))));
    }

    // A face's refined edges, a bit per edge from corner 0's, all four for a face refined whole
    private int resolveMask(RiggedMeshStruct mesh, int faceIndex) {

        if (faceFlags.getBoolean(faceIndex))
            return EditorSetting.CHARACTER_TOPOLOGY_MASK_FULL;

        int mask = 0;

        for (int c = 0; c < mesh.getFaceCornerCount(faceIndex); c++)
            if (splitEdges.contains(RiggedMeshStruct.toEdgeKey(
                    mesh.getFaceVertex(faceIndex, c),
                    mesh.getFaceVertex(faceIndex, mesh.getNextCorner(faceIndex, c)))))
                mask |= 1 << c;

        return mask;
    }

    private int countAddedFaces(int faceCount) {

        int added = 0;

        for (int f = 0; f < faceCount; f++) {

            int mask = faceMasks.getInt(f);

            if (mask != 0)
                added += resolveTemplate(mask).length / (EngineSetting.QUAD_VERTEX_COUNT * 2) - 1;
        }

        return added;
    }

    // Two new vertices on every refined edge, at its thirds, made in turn so the second follows the first, each
    // measured from its nearer end so an edge and its mirror make the same two points; each piece a seam where the
    // edge was a seam
    private void cutEdges(RiggedMeshStruct mesh) {

        edge2Point.clear();

        for (LongIterator iterator = splitEdges.iterator(); iterator.hasNext();) {

            long edgeKey = iterator.nextLong();
            int low = RiggedMeshStruct.getSeamVertex(edgeKey, 0);
            int high = RiggedMeshStruct.getSeamVertex(edgeKey, 1);
            int first = addBetween(mesh, low, high, 1f / EditorSetting.CHARACTER_TOPOLOGY_LATTICE);
            int second = addBetween(mesh, high, low, 1f / EditorSetting.CHARACTER_TOPOLOGY_LATTICE);

            edge2Point.put(edgeKey, first);

            if (!mesh.isSeam(low, high))
                continue;

            mesh.setSeam(low, high, false);
            mesh.setSeam(low, first, true);
            mesh.setSeam(first, second, true);
            mesh.setSeam(second, high, true);
        }
    }

    // A new vertex the given share of the way from one vertex to another
    int addBetween(RiggedMeshStruct mesh, int from, int to, float share) {

        blendVertices.clear();
        blendShares.clear();
        blendVertices.add(from);
        blendShares.add(1f - share);
        blendVertices.add(to);
        blendShares.add(share);

        int vertexIndex = mesh.addVertex(0f, 0f, 0f);

        blendVertex(mesh, vertexIndex);
        return vertexIndex;
    }

    // A face replaced by the quads of the template its refined edges call for, turned so they lead
    private void layFace(RiggedMeshStruct mesh, int faceIndex) {

        int mask = faceMasks.getInt(faceIndex);
        int turn = resolveTurn(mask);
        int[] template = resolveTemplate(mask);
        int texture = mesh.getFaceTexture(faceIndex);
        int pins = mesh.getFacePins(faceIndex);

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int source = (c + turn) % EngineSetting.QUAD_VERTEX_COUNT;

            corners[c] = mesh.getFaceVertex(faceIndex, source);
            cornerUVs[c * 2] = mesh.getFaceU(faceIndex, source);
            cornerUVs[c * 2 + 1] = mesh.getFaceV(faceIndex, source);
            cornerPins[c] = (pins >> source) & 1;
        }

        for (int i = 0; i < latticeVertices.length; i++)
            latticeVertices[i] = EngineSetting.INDEX_NOT_FOUND;

        int quadFloats = EngineSetting.QUAD_VERTEX_COUNT * 2;

        for (int q = 0; q < template.length / quadFloats; q++) {

            int target = q == 0 ? faceIndex : mesh.duplicateFace(faceIndex);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int x = template[q * quadFloats + c * 2];
                int y = template[q * quadFloats + c * 2 + 1];

                mesh.setFaceVertex(target, c, resolveLatticeVertex(mesh, x, y));
                writeLatticeUV(mesh, target, c, x, y);
            }

            mesh.setFaceTexture(target, texture);
            mesh.setFacePins(target, resolveLatticePins(template, q));
            markFresh(target);
        }
    }

    // The quarter turn that brings a face's refined edges to the front of its template
    private int resolveTurn(int mask) {

        for (int turn = 0; turn < EngineSetting.QUAD_VERTEX_COUNT; turn++)
            if (isTemplateMask(rotateMask(mask, turn)))
                return turn;

        return throwException("No topology template for refined edges " + Integer.toBinaryString(mask));
    }

    private int[] resolveTemplate(int mask) {

        int turned = rotateMask(mask, resolveTurn(mask));

        if (turned == EditorSetting.CHARACTER_TOPOLOGY_MASK_FULL)
            return EditorSetting.CHARACTER_TOPOLOGY_TEMPLATE_FULL;

        if (turned == EditorSetting.CHARACTER_TOPOLOGY_MASK_SIDE)
            return EditorSetting.CHARACTER_TOPOLOGY_TEMPLATE_SIDE;

        if (turned == EditorSetting.CHARACTER_TOPOLOGY_MASK_CORNER)
            return EditorSetting.CHARACTER_TOPOLOGY_TEMPLATE_CORNER;

        return EditorSetting.CHARACTER_TOPOLOGY_TEMPLATE_STRIP;
    }

    private boolean isTemplateMask(int mask) {
        return mask == EditorSetting.CHARACTER_TOPOLOGY_MASK_FULL
                || mask == EditorSetting.CHARACTER_TOPOLOGY_MASK_SIDE
                || mask == EditorSetting.CHARACTER_TOPOLOGY_MASK_CORNER
                || mask == EditorSetting.CHARACTER_TOPOLOGY_MASK_STRIP;
    }

    // The mask as seen from the corner a turn starts at: its edge 0 is the face's edge at that corner
    private int rotateMask(int mask, int turn) {

        int rotated = 0;

        for (int e = 0; e < EngineSetting.QUAD_VERTEX_COUNT; e++)
            if ((mask & (1 << ((e + turn) % EngineSetting.QUAD_VERTEX_COUNT))) != 0)
                rotated |= 1 << e;

        return rotated;
    }

    // The vertex at a lattice point: a corner, a third of an edge, or a point within made once for the face
    private int resolveLatticeVertex(RiggedMeshStruct mesh, int x, int y) {

        int last = EditorSetting.CHARACTER_TOPOLOGY_LATTICE;
        int slot = y * (last + 1) + x;

        if (latticeVertices[slot] != EngineSetting.INDEX_NOT_FOUND)
            return latticeVertices[slot];

        int vertexIndex;

        if (y == 0)
            vertexIndex = x == 0 ? corners[0] : x == last ? corners[1] : findEdgePoint(corners[0], corners[1], x);
        else if (x == last)
            vertexIndex = y == last ? corners[2] : findEdgePoint(corners[1], corners[2], y);
        else if (y == last)
            vertexIndex = x == 0 ? corners[3] : findEdgePoint(corners[2], corners[3], last - x);
        else if (x == 0)
            vertexIndex = findEdgePoint(corners[3], corners[0], last - y);
        else
            vertexIndex = addWithin(mesh, (float) x / last, (float) y / last);

        latticeVertices[slot] = vertexIndex;
        return vertexIndex;
    }

    // The point the given thirds along an edge from one of its ends
    private int findEdgePoint(int from, int to, int thirds) {

        int first = edge2Point.get(RiggedMeshStruct.toEdgeKey(from, to));

        if (first == EngineSetting.INDEX_NOT_FOUND)
            throwException("Topology template reached an edge that was not refined");

        return first + (from < to ? thirds - 1 : EditorSetting.CHARACTER_TOPOLOGY_LATTICE - 1 - thirds);
    }

    // A new vertex within the face, blended from its corners at (s, t) across them
    private int addWithin(RiggedMeshStruct mesh, float s, float t) {

        blendVertices.clear();
        blendShares.clear();

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {
            blendVertices.add(corners[c]);
            blendShares.add(resolveBilinear(c, s, t));
        }

        int vertexIndex = mesh.addVertex(0f, 0f, 0f);

        blendVertex(mesh, vertexIndex);
        return vertexIndex;
    }

    // A corner's share of a point at (s, t) across the face, from corner 0 along corner 1 and toward corner 3
    private float resolveBilinear(int corner, float s, float t) {
        return switch (corner) {
            case 0 -> (1f - s) * (1f - t);
            case 1 -> s * (1f - t);
            case 2 -> s * t;
            default -> (1f - s) * t;
        };
    }

    private void writeLatticeUV(RiggedMeshStruct mesh, int faceIndex, int corner, int x, int y) {

        float s = (float) x / EditorSetting.CHARACTER_TOPOLOGY_LATTICE;
        float t = (float) y / EditorSetting.CHARACTER_TOPOLOGY_LATTICE;
        float u = 0f;
        float v = 0f;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            float share = resolveBilinear(c, s, t);

            u += cornerUVs[c * 2] * share;
            v += cornerUVs[c * 2 + 1] * share;
        }

        mesh.setFaceUV(faceIndex, corner, CharacterViewUtility.round(u), CharacterViewUtility.round(v));
    }

    // A quad keeps a pin only at a corner of the face it was laid in, where that corner was pinned
    private int resolveLatticePins(int[] template, int quad) {

        int last = EditorSetting.CHARACTER_TOPOLOGY_LATTICE;
        int quadFloats = EngineSetting.QUAD_VERTEX_COUNT * 2;
        int pins = 0;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int x = template[quad * quadFloats + c * 2];
            int y = template[quad * quadFloats + c * 2 + 1];
            int source = x == 0 ? (y == 0 ? 0 : y == last ? 3 : EngineSetting.INDEX_NOT_FOUND)
                    : x == last ? (y == 0 ? 1 : y == last ? 2 : EngineSetting.INDEX_NOT_FOUND)
                    : EngineSetting.INDEX_NOT_FOUND;

            if (source != EngineSetting.INDEX_NOT_FOUND && cornerPins[source] != 0)
                pins |= 1 << c;
        }

        return pins;
    }

    private void markFresh(int faceIndex) {

        if (faceIndex >= freshFaces.size())
            freshFaces.size(faceIndex + 1);

        freshFaces.set(faceIndex, true);
    }

    // Reduce \\

    // Collapses the smallest covered faces across their shorter diagonal, and each one's twin with mirror on
    private boolean reduce(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();
        IntArrayList mirrors = characterEditorManager.isMirror()
                ? characterSelectionBranch.resolveMirror(document)
                : null;
        int target = Math.min(
                EditorSetting.CHARACTER_TOPOLOGY_REDUCE_MAX,
                Math.max(1, Math.round(candidates.size() * EditorSetting.CHARACTER_TOPOLOGY_REDUCE_SHARE)));
        int collapsed = 0;

        faceFlags.clear();
        faceFlags.size(mesh.getFaceCount());
        resolveLockedVertices(mesh);
        buildAdjacency(mesh);
        resolveAreas(mesh);
        IntArrays.quickSort(candidates.elements(), 0, candidates.size(), areaOrder);

        for (int i = 0; i < candidates.size() && collapsed < target; i++) {

            int faceIndex = candidates.getInt(i);

            if (faceFlags.getBoolean(faceIndex))
                continue;

            int diagonal = resolveBestDiagonal(mesh, faceIndex);

            if (!collapseDiagonal(mesh, faceIndex, diagonal)) {

                diagonal = 1 - diagonal;

                if (!collapseDiagonal(mesh, faceIndex, diagonal))
                    continue;
            }

            collapsed++;

            if (mirrors != null)
                collapseTwin(mesh, mirrors, faceIndex, diagonal);
        }

        if (collapsed == 0)
            return false;

        removeFlaggedFaces(mesh);
        return true;
    }

    // Vertices on an open edge or a UV seam stay where they are, so the outline and the islands keep their shape;
    // found again only once a refine has added vertices, since a collapse never opens an edge or cuts a seam
    private void resolveLockedVertices(RiggedMeshStruct mesh) {

        if (!lockedStale && lockedVertices.size() == mesh.getVertexCount())
            return;

        this.lockedStale = false;
        lockedVertices.clear();
        lockedVertices.size(mesh.getVertexCount());
        edgeCounts.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < mesh.getFaceCornerCount(f); c++)
                edgeCounts.addTo(RiggedMeshStruct.toEdgeKey(
                        mesh.getFaceVertex(f, c),
                        mesh.getFaceVertex(f, mesh.getNextCorner(f, c))), 1);

        for (Long2IntOpenHashMap.Entry entry : edgeCounts.long2IntEntrySet())
            if (entry.getIntValue() == 1)
                lockEdge(entry.getLongKey());

        long[] seams = mesh.getSortedSeams();

        for (int i = 0; i < seams.length; i++)
            lockEdge(seams[i]);
    }

    private void lockEdge(long edgeKey) {
        lockedVertices.set(RiggedMeshStruct.getSeamVertex(edgeKey, 0), true);
        lockedVertices.set(RiggedMeshStruct.getSeamVertex(edgeKey, 1), true);
    }

    private void resolveAreas(RiggedMeshStruct mesh) {

        faceAreas.clear();
        faceAreas.size(mesh.getFaceCount());

        for (int i = 0; i < candidates.size(); i++) {

            int faceIndex = candidates.getInt(i);

            resolveFaceNormal(mesh, faceIndex, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND, before);
            faceAreas.set(faceIndex, before.length());
        }
    }

    // 0 for the diagonal from corner 0 to corner 2, 1 for the one from corner 1 to corner 3: the one whose collapse
    // leaves the merged vertex and the two beside it nearest four faces each, the shorter when both do as well
    private int resolveBestDiagonal(RiggedMeshStruct mesh, int faceIndex) {

        int first = resolveValenceCost(mesh, faceIndex, 0);
        int second = resolveValenceCost(mesh, faceIndex, 1);

        if (first != second)
            return first < second ? 0 : 1;

        return resolveDistanceSquared(mesh, mesh.getFaceVertex(faceIndex, 0), mesh.getFaceVertex(faceIndex, 2))
                <= resolveDistanceSquared(mesh, mesh.getFaceVertex(faceIndex, 1), mesh.getFaceVertex(faceIndex, 3))
                ? 0
                : 1;
    }

    // How far a collapse leaves its vertices from four faces each: the merged vertex takes both rings but the face,
    // each side vertex loses it, and a side left between two faces is dissolved, so it counts as regular
    private int resolveValenceCost(RiggedMeshStruct mesh, int faceIndex, int diagonal) {

        int regular = EngineSetting.QUAD_VERTEX_COUNT;
        int merged = countFaces(mesh, mesh.getFaceVertex(faceIndex, diagonal))
                + countFaces(mesh, mesh.getFaceVertex(faceIndex, diagonal + 2)) - 2;

        return Math.abs(merged - regular)
                + resolveSideCost(countFaces(mesh, mesh.getFaceVertex(faceIndex, diagonal + 1)) - 1)
                + resolveSideCost(countFaces(mesh, mesh.getFaceVertex(faceIndex, (diagonal + 3)
                        % EngineSetting.QUAD_VERTEX_COUNT)) - 1);
    }

    private int resolveSideCost(int faces) {
        return faces == EditorSetting.CHARACTER_TOPOLOGY_DOUBLET_FACES
                ? 0
                : Math.abs(faces - EngineSetting.QUAD_VERTEX_COUNT);
    }

    private int countFaces(RiggedMeshStruct mesh, int vertexIndex) {

        collectRing(mesh, vertexIndex, EngineSetting.INDEX_NOT_FOUND, ringA);
        return ringA.size();
    }

    private float resolveDistanceSquared(RiggedMeshStruct mesh, int a, int b) {

        float dx = mesh.getX(a) - mesh.getX(b);
        float dy = mesh.getY(a) - mesh.getY(b);
        float dz = mesh.getZ(a) - mesh.getZ(b);

        return dx * dx + dy * dy + dz * dz;
    }

    // The face across x from one just collapsed — the live face standing on the twins of its diagonal's two corners,
    // opposite each other — collapsed along that diagonal; nothing for a face the centre line runs through
    private void collapseTwin(RiggedMeshStruct mesh, IntArrayList mirrors, int faceIndex, int diagonal) {

        int twinKeep = findTwin(mirrors, mesh.getFaceVertex(faceIndex, diagonal));
        int twinDrop = findTwin(mirrors, mesh.getFaceVertex(faceIndex, diagonal + 2));

        if (twinKeep == EngineSetting.INDEX_NOT_FOUND || twinDrop == EngineSetting.INDEX_NOT_FOUND
                || mesh.usesVertex(faceIndex, twinKeep) || mesh.usesVertex(faceIndex, twinDrop))
            return;

        collectRing(mesh, twinKeep, EngineSetting.INDEX_NOT_FOUND, ringA);

        for (int i = 0; i < ringA.size(); i++) {

            int f = ringA.getInt(i);
            int corner = findCorner(mesh, f, twinKeep);

            if (mesh.getFaceVertex(f, (corner + 2) % EngineSetting.QUAD_VERTEX_COUNT) != twinDrop)
                continue;

            collapseDiagonal(mesh, f, corner);
            return;
        }
    }

    private int findTwin(IntArrayList mirrors, int vertexIndex) {
        return vertexIndex < mirrors.size() ? mirrors.getInt(vertexIndex) : EngineSetting.INDEX_NOT_FOUND;
    }

    // Merges a face's corner and the one opposite it into the vertex kept, at their middle, and drops the face, true
    // where the collapse keeps the surface a clean quad mesh; the vertices beside it left between just two faces
    // dissolve
    private boolean collapseDiagonal(RiggedMeshStruct mesh, int faceIndex, int keepCorner) {

        int keep = mesh.getFaceVertex(faceIndex, keepCorner);
        int drop = mesh.getFaceVertex(faceIndex, (keepCorner + 2) % EngineSetting.QUAD_VERTEX_COUNT);
        int sideA = mesh.getFaceVertex(faceIndex, mesh.getNextCorner(faceIndex, keepCorner));
        int sideB = mesh.getFaceVertex(faceIndex, (keepCorner + 3) % EngineSetting.QUAD_VERTEX_COUNT);

        if (keep == drop || lockedVertices.getBoolean(keep) || lockedVertices.getBoolean(drop))
            return false;

        collectRing(mesh, keep, faceIndex, ringA);
        collectRing(mesh, drop, faceIndex, ringB);

        if (!isLinkClear(mesh, keep, drop, sideA, sideB))
            return false;

        merged.set(
                (mesh.getX(keep) + mesh.getX(drop)) * 0.5f,
                (mesh.getY(keep) + mesh.getY(drop)) * 0.5f,
                (mesh.getZ(keep) + mesh.getZ(drop)) * 0.5f);

        if (folds(mesh, ringA, keep, drop) || folds(mesh, ringB, keep, drop))
            return false;

        shiftRingUVs(mesh, faceIndex, keep, drop);

        blendVertices.clear();
        blendShares.clear();
        blendVertices.add(keep);
        blendShares.add(0.5f);
        blendVertices.add(drop);
        blendShares.add(0.5f);
        blendVertex(mesh, keep);

        for (int i = 0; i < ringB.size(); i++) {
            replaceCorner(mesh, ringB.getInt(i), drop, keep);
            addEntry(keep, ringB.getInt(i));
        }

        faceFlags.set(faceIndex, true);
        dissolveDoublet(mesh, sideA);
        dissolveDoublet(mesh, sideB);

        return true;
    }

    // Every live face but one that stands on a vertex, in face order
    private void collectRing(RiggedMeshStruct mesh, int vertexIndex, int skippedFace, IntArrayList ring) {

        ring.clear();

        for (int e = vertex2FirstEntry.getInt(vertexIndex);
                e != EngineSetting.INDEX_NOT_FOUND;
                e = entryNext.getInt(e)) {

            int f = entryFaces.getInt(e);

            if (f != skippedFace && !faceFlags.getBoolean(f) && mesh.usesVertex(f, vertexIndex) && !ring.contains(f))
                ring.add(f);
        }

        IntArrays.quickSort(ring.elements(), 0, ring.size());
    }

    // The faces about every vertex, chained once for the frame's collapses
    private void buildAdjacency(RiggedMeshStruct mesh) {

        vertex2FirstEntry.clear();
        vertex2FirstEntry.size(mesh.getVertexCount());
        entryFaces.clear();
        entryNext.clear();

        for (int v = 0; v < mesh.getVertexCount(); v++)
            vertex2FirstEntry.set(v, EngineSetting.INDEX_NOT_FOUND);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < mesh.getFaceCornerCount(f); c++)
                addEntry(mesh.getFaceVertex(f, c), f);
    }

    private void addEntry(int vertexIndex, int faceIndex) {

        entryNext.add(vertex2FirstEntry.getInt(vertexIndex));
        entryFaces.add(faceIndex);
        vertex2FirstEntry.set(vertexIndex, entryFaces.size() - 1);
    }

    // The two vertices' rings may meet only at the face's other two corners and never share another face, or the
    // collapse would pinch the surface
    private boolean isLinkClear(RiggedMeshStruct mesh, int keep, int drop, int sideA, int sideB) {

        linkVertices.clear();

        for (int i = 0; i < ringA.size(); i++) {

            int faceIndex = ringA.getInt(i);

            if (mesh.usesVertex(faceIndex, drop))
                return false;

            for (int c = 0; c < mesh.getFaceCornerCount(faceIndex); c++)
                linkVertices.add(mesh.getFaceVertex(faceIndex, c));
        }

        for (int i = 0; i < ringB.size(); i++)
            for (int c = 0; c < mesh.getFaceCornerCount(ringB.getInt(i)); c++) {

                int vertexIndex = mesh.getFaceVertex(ringB.getInt(i), c);

                if (vertexIndex != drop && vertexIndex != sideA && vertexIndex != sideB
                        && vertexIndex != keep && linkVertices.contains(vertexIndex))
                    return false;
            }

        return true;
    }

    // Whether moving both vertices to their middle turns any face of a ring too far or shrinks it too small
    private boolean folds(RiggedMeshStruct mesh, IntArrayList ring, int keep, int drop) {

        for (int i = 0; i < ring.size(); i++) {

            int faceIndex = ring.getInt(i);

            resolveFaceNormal(mesh, faceIndex, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND, before);
            resolveFaceNormal(mesh, faceIndex, keep, drop, after);

            if (isFolded(before, after))
                return true;
        }

        return false;
    }

    // Whether a face's area normal, once changed, turns past the least cosine or keeps less than the least share of its
    // area
    private boolean isFolded(Vector3 original, Vector3 changed) {

        float originalLength = original.length();
        float changedLength = changed.length();

        return changedLength <= 0f
                || changedLength < originalLength * EditorSetting.CHARACTER_TOPOLOGY_AREA_SHARE_MIN
                || original.dot(changed)
                        <= EditorSetting.CHARACTER_TOPOLOGY_FLIP_COSINE * originalLength * changedLength;
    }

    // A face's normal scaled by its area, across its diagonals; with two vertices given, both read at the merge point
    private void resolveFaceNormal(RiggedMeshStruct mesh, int faceIndex, int keep, int drop, Vector3 out) {

        for (int c = 0; c < mesh.getFaceCornerCount(faceIndex); c++)
            resolveCorner(mesh, mesh.getFaceVertex(faceIndex, c), keep, drop, quadCorners[c]);

        resolveQuadNormal(out);
    }

    private void resolveCorner(RiggedMeshStruct mesh, int vertexIndex, int keep, int drop, Vector3 out) {

        if (vertexIndex == keep || vertexIndex == drop)
            out.set(merged);
        else
            out.set(mesh.getX(vertexIndex), mesh.getY(vertexIndex), mesh.getZ(vertexIndex));
    }

    // The area normal of the quad whose corners are laid out, half the cross product of its diagonals
    private void resolveQuadNormal(Vector3 out) {

        diagonalA.set(quadCorners[2]).subtract(quadCorners[0]);
        diagonalB.set(quadCorners[3]).subtract(quadCorners[1]);

        out.set(
                diagonalA.y * diagonalB.z - diagonalA.z * diagonalB.y,
                diagonalA.z * diagonalB.x - diagonalA.x * diagonalB.z,
                diagonalA.x * diagonalB.y - diagonalA.y * diagonalB.x).multiply(0.5f);
    }

    // The UVs of every face about the two vertices moved as far as their corners move toward the middle, measured
    // across the collapsed face, so the texture holds still
    private void shiftRingUVs(RiggedMeshStruct mesh, int faceIndex, int keep, int drop) {

        int keepCorner = findCorner(mesh, faceIndex, keep);
        int dropCorner = findCorner(mesh, faceIndex, drop);
        float du = (mesh.getFaceU(faceIndex, dropCorner) - mesh.getFaceU(faceIndex, keepCorner)) * 0.5f;
        float dv = (mesh.getFaceV(faceIndex, dropCorner) - mesh.getFaceV(faceIndex, keepCorner)) * 0.5f;

        shiftCornerUVs(mesh, ringA, keep, du, dv);
        shiftCornerUVs(mesh, ringB, drop, -du, -dv);
    }

    private void shiftCornerUVs(RiggedMeshStruct mesh, IntArrayList ring, int vertexIndex, float du, float dv) {

        for (int i = 0; i < ring.size(); i++) {

            int faceIndex = ring.getInt(i);

            if (!mesh.hasAuthoredUVs(faceIndex))
                continue;

            int corner = findCorner(mesh, faceIndex, vertexIndex);

            mesh.setFaceUV(
                    faceIndex,
                    corner,
                    CharacterViewUtility.round(mesh.getFaceU(faceIndex, corner) + du),
                    CharacterViewUtility.round(mesh.getFaceV(faceIndex, corner) + dv));
        }
    }

    private int findCorner(RiggedMeshStruct mesh, int faceIndex, int vertexIndex) {

        for (int c = 0; c < mesh.getFaceCornerCount(faceIndex); c++)
            if (mesh.getFaceVertex(faceIndex, c) == vertexIndex)
                return c;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private void replaceCorner(RiggedMeshStruct mesh, int faceIndex, int from, int to) {

        int corner = findCorner(mesh, faceIndex, from);

        if (corner != EngineSetting.INDEX_NOT_FOUND)
            mesh.setFaceVertex(faceIndex, corner, to);
    }

    // A vertex left between just two faces that share both its edges is dissolved, the two faces joined into one
    private void dissolveDoublet(RiggedMeshStruct mesh, int vertexIndex) {

        if (lockedVertices.getBoolean(vertexIndex))
            return;

        collectRing(mesh, vertexIndex, EngineSetting.INDEX_NOT_FOUND, ringA);

        if (ringA.size() != 2)
            return;

        int first = ringA.getInt(0);
        int second = ringA.getInt(1);
        int at = findCorner(mesh, first, vertexIndex);
        int next = mesh.getFaceVertex(first, mesh.getNextCorner(first, at));
        int far = mesh.getFaceVertex(first, (at + 2) % EngineSetting.QUAD_VERTEX_COUNT);
        int previous = mesh.getFaceVertex(first, (at + 3) % EngineSetting.QUAD_VERTEX_COUNT);
        int otherAt = findCorner(mesh, second, vertexIndex);

        if (mesh.getFaceVertex(second, mesh.getNextCorner(second, otherAt)) != previous
                || mesh.getFaceVertex(second, (otherAt + 3) % EngineSetting.QUAD_VERTEX_COUNT) != next)
            return;

        int opposite = (otherAt + 2) % EngineSetting.QUAD_VERTEX_COUNT;
        int oppositeVertex = mesh.getFaceVertex(second, opposite);

        if (foldsJoin(mesh, first, second, next, far, previous, oppositeVertex))
            return;

        float oppositeU = mesh.getFaceU(second, opposite);
        float oppositeV = mesh.getFaceV(second, opposite);

        for (int c = 0; c < mesh.getFaceCornerCount(first); c++) {

            int source = (at + 1 + c) % EngineSetting.QUAD_VERTEX_COUNT;

            cornerUVs[c * 2] = mesh.getFaceU(first, source);
            cornerUVs[c * 2 + 1] = mesh.getFaceV(first, source);
        }

        mesh.setFaceVertex(first, 0, next);
        mesh.setFaceVertex(first, 1, far);
        mesh.setFaceVertex(first, 2, previous);
        mesh.setFaceVertex(first, 3, oppositeVertex);
        addEntry(oppositeVertex, first);

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT - 1; c++)
            mesh.setFaceUV(first, c, cornerUVs[c * 2], cornerUVs[c * 2 + 1]);

        mesh.setFaceUV(first, EngineSetting.QUAD_VERTEX_COUNT - 1, oppositeU, oppositeV);
        mesh.setFacePins(first, 0);
        faceFlags.set(second, true);
    }

    // Whether the face two faces join into turns too far from the two or keeps too little of their area
    private boolean foldsJoin(
            RiggedMeshStruct mesh,
            int first,
            int second,
            int a,
            int b,
            int c,
            int d) {

        resolveFaceNormal(mesh, first, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND, before);
        resolveFaceNormal(mesh, second, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND, after);
        before.add(after);
        resolveCorner(mesh, a, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND, quadCorners[0]);
        resolveCorner(mesh, b, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND, quadCorners[1]);
        resolveCorner(mesh, c, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND, quadCorners[2]);
        resolveCorner(mesh, d, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND, quadCorners[3]);
        resolveQuadNormal(after);

        return isFolded(before, after);
    }

    // Every face flagged removed taken out of the mesh, the stroke's own faces kept in step
    private void removeFlaggedFaces(RiggedMeshStruct mesh) {

        int kept = 0;

        freshFaces.size(faceFlags.size());

        for (int f = 0; f < faceFlags.size(); f++)
            if (!faceFlags.getBoolean(f))
                freshFaces.set(kept++, freshFaces.getBoolean(f));

        freshFaces.size(kept);
        mesh.removeFaces(faceFlags);
    }

    // Blend \\

    // A vertex given the blend of the vertices listed, each by its share: position, weights, masks and the delta of
    // every shape key any of them carries; the vertex written may be one of those blended. The position sums in
    // double, so the order the corners are listed in (a face and its mirror list them the other way) cannot move it
    private void blendVertex(RiggedMeshStruct mesh, int vertexIndex) {

        double x = 0.0;
        double y = 0.0;
        double z = 0.0;

        for (int ch = 0; ch < maskScratch.length; ch++)
            maskScratch[ch] = 0f;

        for (int i = 0; i < blendVertices.size(); i++) {

            int source = blendVertices.getInt(i);
            float share = blendShares.getFloat(i);

            x += (double) mesh.getX(source) * share;
            y += (double) mesh.getY(source) * share;
            z += (double) mesh.getZ(source) * share;

            for (int ch = 0; ch < maskScratch.length; ch++)
                maskScratch[ch] += mesh.getMask(source, ch) * share;
        }

        blendMorphs(mesh, vertexIndex);
        characterWeightBranch.blend(mesh, blendVertices, blendShares, vertexIndex);
        CharacterMeshUtility.setRoundedPosition(mesh, vertexIndex, (float) x, (float) y, (float) z);

        for (int ch = 0; ch < maskScratch.length; ch++)
            mesh.setMask(vertexIndex, ch, maskScratch[ch]);
    }

    private void blendMorphs(RiggedMeshStruct mesh, int vertexIndex) {

        for (int m = 0; m < mesh.getMorphCount(); m++) {

            float dx = 0f;
            float dy = 0f;
            float dz = 0f;
            boolean carried = false;

            for (int i = 0; i < blendVertices.size(); i++) {

                int entry = mesh.findMorphEntry(m, blendVertices.getInt(i));

                if (entry == EngineSetting.INDEX_NOT_FOUND)
                    continue;

                float share = blendShares.getFloat(i);

                dx += mesh.getMorphDelta(m, entry, 0) * share;
                dy += mesh.getMorphDelta(m, entry, 1) * share;
                dz += mesh.getMorphDelta(m, entry, 2) * share;
                carried = true;
            }

            if (carried || mesh.findMorphEntry(m, vertexIndex) != EngineSetting.INDEX_NOT_FOUND)
                mesh.setMorphDelta(m, vertexIndex, dx, dy, dz);
        }
    }
}
