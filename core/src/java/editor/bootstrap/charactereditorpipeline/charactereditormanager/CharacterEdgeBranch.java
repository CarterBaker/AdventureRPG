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
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

class CharacterEdgeBranch extends BranchPackage {

    /*
     * The Topology tab's edge tools and clean-up, the retopology toolset of
     * Blender's and Maya's modelling kits over the editor's quads (a triangle
     * is a quad with its last corner doubled). Flip Edge is Maya's Spin Edge
     * (Blender's Rotate Edge): the two faces beside the edge clicked are one
     * polygon, and the edge between them turns one corner round it either
     * way, so a "/" becomes a "\" and two quads stay two quads. Loop Cut is
     * Blender's Loop Cut and Maya's Insert Edge Loop: the ring of quads
     * across the edge clicked, walked through each quad's opposite edge
     * until it closes, meets an open edge or a triangle, is cut through the
     * middle of every edge it crosses. Dissolve Loop is Maya's Delete Edge
     * Loop: the edge loop through the edge clicked, walked straight on
     * through every vertex four faces meet at, is taken away and its
     * vertices with it, the faces either side joined in pairs, so the mesh
     * stays all quads; a loop must close or run to open edges, since a pole
     * would leave a face of more than four corners. Each honours Mirror,
     * making the same edit to the twin edge across the body wound the other
     * way. Clean-up merges vertices standing within the merge distance,
     * turns faces over, deletes faces with no area or fewer than three
     * corners, and selects what breaks a quad flow: poles (inner vertices
     * not four faces meet at), triangles and non-manifold edges (open, or
     * shared by more than two faces). New vertices blend position, weights,
     * masks and shape-key deltas from the ends of the edge they cut; faces
     * keep their texture and zone and carry their UVs; a cut seam stays a
     * seam, and a seam edge is never flipped or dissolved, since its faces
     * lie in different islands. Every edit is one undo step. The faces about
     * each edge are found once an edit, never per face.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterMeshBranch characterMeshBranch;
    private CharacterHistoryBranch characterHistoryBranch;
    private CharacterSelectionBranch characterSelectionBranch;
    private CharacterSculptBranch characterSculptBranch;
    private CharacterTopologyBranch characterTopologyBranch;

    // Adjacency — how many faces stand on each edge, the first two packed into one long, and how many on each vertex
    private Long2IntOpenHashMap edgeCounts;
    private Long2LongOpenHashMap edgeFaces;
    private IntArrayList vertexFaceCounts;
    private BooleanArrayList boundaryVertices;

    // Polygon — the two faces of a flip walked as one, each corner's vertex and the face and corner it came from
    private IntArrayList cycleA;
    private IntArrayList cycleB;
    private IntArrayList cornersA;
    private IntArrayList cornersB;
    private IntArrayList polygon;
    private IntArrayList polygonFaces;
    private IntArrayList polygonCorners;

    // Flip — the turn checked last: the face winding along the edge, the one winding back, and the polygon places the
    // new edge runs between
    private int flipFaceA;
    private int flipFaceB;
    private int flipFrom;
    private int flipTo;

    // Ring and Loop — the faces a loop cut crosses with the corner its cut edges start at, the edges a dissolve takes
    private IntArrayList ringFaces;
    private IntArrayList ringCorners;
    private IntOpenHashSet ringFaceSet;
    private int ringFloor;
    private boolean ringOverlap;
    private Long2IntOpenHashMap edge2Middle;
    private LongArrayList loopEdges;
    private LongOpenHashSet loopEdgeSet;
    private IntOpenHashSet loopFaceSet;
    private int loopFloor;

    // Merge
    private Long2IntOpenHashMap cell2Vertex;
    private IntArrayList cellNext;
    private IntArrayList vertexRemap;

    // Stats — the counts the Properties tab shows, made again only when the mesh moves
    private CharacterMeshDocumentInstance statsDocument;
    private int statsRevision;
    private int statsQuads;
    private int statsTriangles;
    private int statsPoles;

    // Scratch
    private BooleanArrayList faceFlags;
    private IntArrayList vertexList;
    private FloatArrayList cornerUVs;
    private int[] quadScratch;
    private float[] quadUVScratch;
    private Vector3 normal;

    // Base \\

    @Override
    protected void create() {

        // Adjacency
        this.edgeCounts = new Long2IntOpenHashMap();
        this.edgeFaces = new Long2LongOpenHashMap();
        this.vertexFaceCounts = new IntArrayList();
        this.boundaryVertices = new BooleanArrayList();

        // Polygon
        this.cycleA = new IntArrayList();
        this.cycleB = new IntArrayList();
        this.cornersA = new IntArrayList();
        this.cornersB = new IntArrayList();
        this.polygon = new IntArrayList();
        this.polygonFaces = new IntArrayList();
        this.polygonCorners = new IntArrayList();

        // Ring and Loop
        this.ringFaces = new IntArrayList();
        this.ringCorners = new IntArrayList();
        this.ringFaceSet = new IntOpenHashSet();
        this.edge2Middle = new Long2IntOpenHashMap();
        edge2Middle.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.loopEdges = new LongArrayList();
        this.loopEdgeSet = new LongOpenHashSet();
        this.loopFaceSet = new IntOpenHashSet();

        // Merge
        this.cell2Vertex = new Long2IntOpenHashMap();
        cell2Vertex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.cellNext = new IntArrayList();
        this.vertexRemap = new IntArrayList();

        // Stats
        this.statsRevision = EngineSetting.INDEX_NOT_FOUND;

        // Scratch
        this.faceFlags = new BooleanArrayList();
        this.vertexList = new IntArrayList();
        this.cornerUVs = new FloatArrayList();
        this.quadScratch = new int[EngineSetting.QUAD_VERTEX_COUNT];
        this.quadUVScratch = new float[EngineSetting.QUAD_VERTEX_COUNT * 2];
        this.normal = new Vector3();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterMeshBranch = get(CharacterMeshBranch.class);
        this.characterHistoryBranch = get(CharacterHistoryBranch.class);
        this.characterSelectionBranch = get(CharacterSelectionBranch.class);
        this.characterSculptBranch = get(CharacterSculptBranch.class);
        this.characterTopologyBranch = get(CharacterTopologyBranch.class);
    }

    // Flip Edge \\

    // The edge from a face's corner to the next turned one corner round its two faces, forward along the face's own
    // winding or backward, and with mirror on its twin the mirrored way
    void flip(CharacterMeshDocumentInstance document, int faceIndex, int corner, boolean backward) {

        RiggedMeshStruct mesh = document.getMesh();
        int a = mesh.getFaceVertex(faceIndex, corner);
        int b = mesh.getFaceVertex(faceIndex, (corner + 1) % EngineSetting.QUAD_VERTEX_COUNT);

        buildEdgeFaces(mesh);

        String refusal = checkFlip(mesh, a, b, backward);

        if (refusal != null) {
            characterEditorManager.setStatusMessage(refusal);
            return;
        }

        int faceA = flipFaceA;
        int faceB = flipFaceB;
        int twinA = EngineSetting.INDEX_NOT_FOUND;
        int twinB = EngineSetting.INDEX_NOT_FOUND;
        IntArrayList mirrors = characterEditorManager.getMirrorVertices();

        if (mirrors != null && a < mirrors.size() && b < mirrors.size()) {
            twinA = mirrors.getInt(b);
            twinB = mirrors.getInt(a);
        }

        characterHistoryBranch.begin(document);
        layFlip(mesh);

        if (isSeparateEdge(a, b, twinA, twinB, faceA, faceB)) {

            buildEdgeFaces(mesh);

            if (checkFlip(mesh, twinA, twinB, !backward) == null)
                layFlip(mesh);
        }

        if (characterMeshBranch.finishEdit(document))
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_FLIPPED);
    }

    // Whether a twin edge is another edge, neither of whose faces is one of the two just flipped, read from the faces
    // about each edge as they stood before the flip
    private boolean isSeparateEdge(int a, int b, int twinA, int twinB, int faceA, int faceB) {

        if (twinA == EngineSetting.INDEX_NOT_FOUND || twinB == EngineSetting.INDEX_NOT_FOUND || twinA == twinB)
            return false;

        if (RiggedMeshStruct.toEdgeKey(a, b) == RiggedMeshStruct.toEdgeKey(twinA, twinB))
            return false;

        long key = RiggedMeshStruct.toEdgeKey(twinA, twinB);

        if (edgeCounts.get(key) != 2)
            return false;

        long packed = edgeFaces.get(key);
        int first = unpackFirst(packed);
        int second = unpackSecond(packed);

        return first != faceA && first != faceB && second != faceA && second != faceB;
    }

    // Why the edge from a to b cannot turn, or null with its polygon walked and the turn's two faces laid out: the
    // face winding a to b first, then the one winding b to a
    private String checkFlip(RiggedMeshStruct mesh, int a, int b, boolean backward) {

        long key = RiggedMeshStruct.toEdgeKey(a, b);

        if (a == b || edgeCounts.get(key) != 2)
            return EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_OPEN_EDGE;

        if (mesh.isSeam(a, b))
            return EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_SEAM;

        int faceA = findWindingFace(mesh, key, a, b);
        int faceB = findWindingFace(mesh, key, b, a);

        if (faceA == EngineSetting.INDEX_NOT_FOUND || faceB == EngineSetting.INDEX_NOT_FOUND)
            return EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_WINDING;

        collectCycle(mesh, faceA, cycleA, cornersA);
        collectCycle(mesh, faceB, cycleB, cornersB);
        rotateTo(cycleA, cornersA, a);
        rotateTo(cycleB, cornersB, b);

        // The polygon round both faces: a, the second face's corners past a, b, then the first face's past b
        polygon.clear();
        polygonFaces.clear();
        polygonCorners.clear();
        addPolygonCorner(a, faceA, cornersA.getInt(0));

        for (int i = 2; i < cycleB.size(); i++)
            addPolygonCorner(cycleB.getInt(i), faceB, cornersB.getInt(i));

        addPolygonCorner(b, faceA, cornersA.getInt(1));

        for (int i = 2; i < cycleA.size(); i++)
            addPolygonCorner(cycleA.getInt(i), faceA, cornersA.getInt(i));

        int count = polygon.size();
        int shift = backward ? -1 : 1;
        int from = Math.floorMod(shift, count);
        int to = Math.floorMod(cycleB.size() - 1 + shift, count);
        int fromVertex = polygon.getInt(from);
        int toVertex = polygon.getInt(to);

        if (fromVertex == toVertex || edgeCounts.containsKey(RiggedMeshStruct.toEdgeKey(fromVertex, toVertex)))
            return EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_JOINED;

        this.flipFaceA = faceA;
        this.flipFaceB = faceB;
        this.flipFrom = from;
        this.flipTo = to;

        return null;
    }

    private void addPolygonCorner(int vertexIndex, int faceIndex, int corner) {
        polygon.add(vertexIndex);
        polygonFaces.add(faceIndex);
        polygonCorners.add(corner);
    }

    // The two faces of the checked turn written over the polygon: the first face from the new edge's one end round to
    // the other, the second back again, each corner carrying the UV it had
    private void layFlip(RiggedMeshStruct mesh) {

        boolean authored = mesh.hasAuthoredUVs(flipFaceA) || mesh.hasAuthoredUVs(flipFaceB);

        layPolygonFace(mesh, flipFaceA, flipFrom, flipTo, authored);
        layPolygonFace(mesh, flipFaceB, flipTo, flipFrom, authored);
    }

    // A face over the polygon's corners from one place round to another, its last corner doubled for a triangle
    private void layPolygonFace(RiggedMeshStruct mesh, int faceIndex, int from, int to, boolean authored) {

        int count = polygon.size();
        int corners = Math.floorMod(to - from, count) + 1;

        vertexList.clear();
        cornerUVs.clear();

        for (int i = 0; i < corners; i++) {

            int place = (from + i) % count;
            int sourceFace = polygonFaces.getInt(place);
            int sourceCorner = polygonCorners.getInt(place);

            vertexList.add(polygon.getInt(place));
            cornerUVs.add(mesh.getFaceU(sourceFace, sourceCorner));
            cornerUVs.add(mesh.getFaceV(sourceFace, sourceCorner));
        }

        writeFace(mesh, faceIndex, vertexList, cornerUVs, authored);
    }

    // Loop Cut \\

    // A new edge loop through the middle of the ring of quads across the edge from a face's corner to the next, and
    // with mirror on through its twin's ring; the new vertices become the selection
    void cutLoop(CharacterMeshDocumentInstance document, int faceIndex, int corner) {

        RiggedMeshStruct mesh = document.getMesh();

        if (countDistinct(mesh, faceIndex) != EngineSetting.QUAD_VERTEX_COUNT) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_RING_TRIANGLE);
            return;
        }

        buildEdgeFaces(mesh);
        ringFaces.clear();
        ringCorners.clear();
        ringFaceSet.clear();
        this.ringFloor = 0;
        collectRing(mesh, faceIndex, corner);
        addTwinRing(mesh, faceIndex, corner);

        characterHistoryBranch.begin(document);
        edge2Middle.clear();
        vertexList.clear();

        int faceCount = ringFaces.size();

        for (int i = 0; i < faceCount; i++)
            cutRingFace(mesh, ringFaces.getInt(i), ringCorners.getInt(i));

        document.syncSelection();
        characterSelectionBranch.select(document, vertexList, CharacterEditorSelection.REPLACE);

        if (characterMeshBranch.finishEdit(document))
            characterEditorManager.setStatusMessage(faceCount + EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_CUT);
    }

    // The twin edge's ring added while mirroring, unless it is the ring already walked or runs into one of its faces
    private void addTwinRing(RiggedMeshStruct mesh, int faceIndex, int corner) {

        IntArrayList mirrors = characterEditorManager.getMirrorVertices();

        if (mirrors == null)
            return;

        int a = mesh.getFaceVertex(faceIndex, corner);
        int b = mesh.getFaceVertex(faceIndex, (corner + 1) % EngineSetting.QUAD_VERTEX_COUNT);

        if (a >= mirrors.size() || b >= mirrors.size())
            return;

        int twinA = mirrors.getInt(a);
        int twinB = mirrors.getInt(b);

        if (twinA == EngineSetting.INDEX_NOT_FOUND || twinB == EngineSetting.INDEX_NOT_FOUND || twinA == twinB)
            return;

        long key = RiggedMeshStruct.toEdgeKey(twinA, twinB);

        if (!edgeCounts.containsKey(key))
            return;

        int twinFace = unpackFirst(edgeFaces.get(key));
        int twinCorner = findEdgeCorner(mesh, twinFace, twinA, twinB);

        if (ringFaceSet.contains(twinFace) || twinCorner == EngineSetting.INDEX_NOT_FOUND
                || countDistinct(mesh, twinFace) != EngineSetting.QUAD_VERTEX_COUNT)
            return;

        int kept = ringFaces.size();

        this.ringFloor = kept;
        this.ringOverlap = false;
        collectRing(mesh, twinFace, twinCorner);

        if (!ringOverlap)
            return;

        ringFaces.size(kept);
        ringCorners.size(kept);
    }

    // Every quad of the ring through a face's two opposite edges, the one at the corner given and the one across it,
    // walked out both ways until the ring closes or meets an open edge or a triangle
    private void collectRing(RiggedMeshStruct mesh, int faceIndex, int corner) {

        addRingFace(faceIndex, corner);
        walkRing(mesh, faceIndex, (corner + 2) % EngineSetting.QUAD_VERTEX_COUNT);
        walkRing(mesh, faceIndex, corner);
    }

    private void walkRing(RiggedMeshStruct mesh, int faceIndex, int exitCorner) {

        int face = faceIndex;
        int exit = exitCorner;

        while (true) {

            int x = mesh.getFaceVertex(face, exit);
            int y = mesh.getFaceVertex(face, (exit + 1) % EngineSetting.QUAD_VERTEX_COUNT);
            int next = findOtherFace(RiggedMeshStruct.toEdgeKey(x, y), face);

            if (next != EngineSetting.INDEX_NOT_FOUND && ringFaceSet.contains(next)) {
                this.ringOverlap |= ringFaces.indexOf(next) < ringFloor;
                return;
            }

            if (next == EngineSetting.INDEX_NOT_FOUND || countDistinct(mesh, next) != EngineSetting.QUAD_VERTEX_COUNT)
                return;

            int entry = findEdgeCorner(mesh, next, x, y);

            if (entry == EngineSetting.INDEX_NOT_FOUND)
                return;

            addRingFace(next, entry);
            face = next;
            exit = (entry + 2) % EngineSetting.QUAD_VERTEX_COUNT;
        }
    }

    private void addRingFace(int faceIndex, int corner) {
        ringFaces.add(faceIndex);
        ringCorners.add(corner);
        ringFaceSet.add(faceIndex);
    }

    // A ring quad split in two across the middles of its edge at the corner and its opposite edge, each middle
    // shared with the quad across, every new vertex kept for the selection
    private void cutRingFace(RiggedMeshStruct mesh, int faceIndex, int corner) {

        int[] quad = quadScratch;
        float[] uvs = quadUVScratch;
        boolean authored = mesh.hasAuthoredUVs(faceIndex);

        for (int i = 0; i < EngineSetting.QUAD_VERTEX_COUNT; i++) {

            int source = (corner + i) % EngineSetting.QUAD_VERTEX_COUNT;

            quad[i] = mesh.getFaceVertex(faceIndex, source);
            uvs[i * 2] = mesh.getFaceU(faceIndex, source);
            uvs[i * 2 + 1] = mesh.getFaceV(faceIndex, source);
        }

        int first = resolveMiddle(mesh, quad[0], quad[1]);
        int second = resolveMiddle(mesh, quad[2], quad[3]);
        float firstU = (uvs[0] + uvs[2]) * 0.5f;
        float firstV = (uvs[1] + uvs[3]) * 0.5f;
        float secondU = (uvs[4] + uvs[6]) * 0.5f;
        float secondV = (uvs[5] + uvs[7]) * 0.5f;
        int added = mesh.addFace(first, quad[1], quad[2], second, mesh.getFaceTexture(faceIndex));

        mesh.setFaceZone(added, mesh.getFaceZone(faceIndex));
        mesh.setFaceVertex(faceIndex, 0, quad[0]);
        mesh.setFaceVertex(faceIndex, 1, first);
        mesh.setFaceVertex(faceIndex, 2, second);
        mesh.setFaceVertex(faceIndex, 3, quad[3]);
        mesh.setFacePins(faceIndex, 0);

        if (!authored)
            return;

        mesh.setFaceUV(faceIndex, 0, uvs[0], uvs[1]);
        mesh.setFaceUV(faceIndex, 1, firstU, firstV);
        mesh.setFaceUV(faceIndex, 2, secondU, secondV);
        mesh.setFaceUV(faceIndex, 3, uvs[6], uvs[7]);
        mesh.setFaceUV(added, 0, firstU, firstV);
        mesh.setFaceUV(added, 1, uvs[2], uvs[3]);
        mesh.setFaceUV(added, 2, uvs[4], uvs[5]);
        mesh.setFaceUV(added, 3, secondU, secondV);
    }

    // The new vertex halfway along an edge, made once for both faces beside it, measured from its lower vertex so an
    // edge and its twin find the same point; a cut seam stays a seam on both halves
    private int resolveMiddle(RiggedMeshStruct mesh, int a, int b) {

        long key = RiggedMeshStruct.toEdgeKey(a, b);
        int middle = edge2Middle.get(key);

        if (middle != EngineSetting.INDEX_NOT_FOUND)
            return middle;

        int low = Math.min(a, b);
        int high = Math.max(a, b);

        middle = characterTopologyBranch.addBetween(mesh, low, high, 0.5f);
        edge2Middle.put(key, middle);
        vertexList.add(middle);

        if (mesh.isSeam(low, high)) {
            mesh.setSeam(low, high, false);
            mesh.setSeam(low, middle, true);
            mesh.setSeam(middle, high, true);
        }

        return middle;
    }

    // Dissolve Loop \\

    // The edge loop through the edge from a face's corner to the next taken away, the faces either side of each of its
    // edges joined into one, and with mirror on its twin loop too
    void dissolveLoop(CharacterMeshDocumentInstance document, int faceIndex, int corner) {

        RiggedMeshStruct mesh = document.getMesh();
        int a = mesh.getFaceVertex(faceIndex, corner);
        int b = mesh.getFaceVertex(faceIndex, (corner + 1) % EngineSetting.QUAD_VERTEX_COUNT);

        buildEdgeFaces(mesh);
        buildVertexFaces(mesh);
        loopEdges.clear();
        loopEdgeSet.clear();
        loopFaceSet.clear();
        this.loopFloor = 0;

        String refusal = collectLoop(mesh, a, b);

        if (refusal != null) {
            characterEditorManager.setStatusMessage(refusal);
            return;
        }

        addTwinLoop(mesh, a, b);
        characterHistoryBranch.begin(document);
        faceFlags.clear();
        faceFlags.size(mesh.getFaceCount());

        for (int i = 0; i < loopEdges.size(); i++)
            joinAcross(mesh, loopEdges.getLong(i));

        int dissolved = loopEdges.size();

        mesh.removeFaces(faceFlags);
        characterSculptBranch.removeOrphans(document);

        if (characterMeshBranch.finishEdit(document))
            characterEditorManager.setStatusMessage(dissolved + EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_DISSOLVED);
    }

    // The twin edge's loop added while mirroring, unless it is the loop already walked, touches its faces, or cannot be
    // dissolved itself
    private void addTwinLoop(RiggedMeshStruct mesh, int a, int b) {

        IntArrayList mirrors = characterEditorManager.getMirrorVertices();

        if (mirrors == null || a >= mirrors.size() || b >= mirrors.size())
            return;

        int twinA = mirrors.getInt(b);
        int twinB = mirrors.getInt(a);

        if (twinA == EngineSetting.INDEX_NOT_FOUND || twinB == EngineSetting.INDEX_NOT_FOUND || twinA == twinB
                || loopEdgeSet.contains(RiggedMeshStruct.toEdgeKey(twinA, twinB)))
            return;

        int kept = loopEdges.size();
        IntOpenHashSet faces = new IntOpenHashSet(loopFaceSet);

        this.loopFloor = kept;

        if (collectLoop(mesh, twinA, twinB) == null) {

            boolean overlaps = false;

            for (int i = kept; i < loopEdges.size() && !overlaps; i++) {

                long packed = edgeFaces.get(loopEdges.getLong(i));
                overlaps = faces.contains(unpackFirst(packed)) || faces.contains(unpackSecond(packed));
            }

            if (!overlaps)
                return;
        }

        loopEdges.size(kept);
    }

    // Every edge of the loop through a to b, walked on both ways; why it cannot be dissolved, or null
    private String collectLoop(RiggedMeshStruct mesh, int a, int b) {

        String refusal = addLoopEdge(mesh, a, b);

        if (refusal != null)
            return refusal;

        int forward = walkLoop(mesh, a, b);

        if (forward == EditorSetting.CHARACTER_TOPOLOGY_LOOP_CLOSED)
            return null;

        if (forward == EditorSetting.CHARACTER_TOPOLOGY_LOOP_POLE)
            return EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_POLE;

        return walkLoop(mesh, b, a) == EditorSetting.CHARACTER_TOPOLOGY_LOOP_POLE
                ? EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_POLE
                : null;
    }

    // Walks the loop on past the end vertex of the edge from one vertex to another, straight through every vertex four
    // quads meet at, until it closes, ends on an open edge's vertex two faces meet at, or meets anything else
    private int walkLoop(RiggedMeshStruct mesh, int from, int to) {

        int previous = from;
        int current = to;

        while (true) {

            if (vertexFaceCounts.getInt(current) == 2 && boundaryVertices.getBoolean(current))
                return EditorSetting.CHARACTER_TOPOLOGY_LOOP_OPEN;

            int next = findStraightOn(mesh, previous, current);

            if (next == EngineSetting.INDEX_NOT_FOUND)
                return EditorSetting.CHARACTER_TOPOLOGY_LOOP_POLE;

            long nextKey = RiggedMeshStruct.toEdgeKey(current, next);

            if (loopEdgeSet.contains(nextKey))
                return loopEdges.indexOf(nextKey) >= loopFloor
                        ? EditorSetting.CHARACTER_TOPOLOGY_LOOP_CLOSED
                        : EditorSetting.CHARACTER_TOPOLOGY_LOOP_POLE;

            if (addLoopEdge(mesh, current, next) != null)
                return EditorSetting.CHARACTER_TOPOLOGY_LOOP_POLE;

            previous = current;
            current = next;
        }
    }

    // The vertex the loop runs on to past a vertex four quads meet at: across the two quads beside the edge it came
    // by, the edge their other neighbours share; INDEX_NOT_FOUND at a pole or a triangle
    private int findStraightOn(RiggedMeshStruct mesh, int previous, int current) {

        if (vertexFaceCounts.getInt(current) != EngineSetting.QUAD_VERTEX_COUNT || boundaryVertices.getBoolean(current))
            return EngineSetting.INDEX_NOT_FOUND;

        long packed = edgeFaces.get(RiggedMeshStruct.toEdgeKey(previous, current));
        int first = unpackFirst(packed);
        int second = unpackSecond(packed);
        int sideA = findNeighbour(mesh, first, current, previous);
        int sideB = findNeighbour(mesh, second, current, previous);

        if (sideA == EngineSetting.INDEX_NOT_FOUND || sideB == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        int third = findOtherFace(RiggedMeshStruct.toEdgeKey(current, sideA), first);
        int fourth = findOtherFace(RiggedMeshStruct.toEdgeKey(current, sideB), second);

        if (third == EngineSetting.INDEX_NOT_FOUND || fourth == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        int onA = findNeighbour(mesh, third, current, sideA);
        int onB = findNeighbour(mesh, fourth, current, sideB);

        return onA == onB ? onA : EngineSetting.INDEX_NOT_FOUND;
    }

    // One edge of the loop kept, with its two faces; why it cannot be dissolved, or null
    private String addLoopEdge(RiggedMeshStruct mesh, int a, int b) {

        long key = RiggedMeshStruct.toEdgeKey(a, b);

        if (a == b || edgeCounts.get(key) != 2)
            return EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_OPEN_EDGE;

        if (mesh.isSeam(a, b))
            return EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_SEAM;

        long packed = edgeFaces.get(key);
        int first = unpackFirst(packed);
        int second = unpackSecond(packed);

        if (countDistinct(mesh, first) != EngineSetting.QUAD_VERTEX_COUNT
                || countDistinct(mesh, second) != EngineSetting.QUAD_VERTEX_COUNT)
            return EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_POLE;

        if (findWindingFace(mesh, key, a, b) == EngineSetting.INDEX_NOT_FOUND
                || findWindingFace(mesh, key, b, a) == EngineSetting.INDEX_NOT_FOUND)
            return EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_WINDING;

        loopEdges.add(key);
        loopEdgeSet.add(key);
        loopFaceSet.add(first);
        loopFaceSet.add(second);

        return null;
    }

    // The two quads beside a loop edge joined: the one winding along it keeps its far corners and takes the other's
    // far corners in place of the edge's ends, with their UVs, and the other goes
    private void joinAcross(RiggedMeshStruct mesh, long edgeKey) {

        int a = RiggedMeshStruct.getSeamVertex(edgeKey, 0);
        int b = RiggedMeshStruct.getSeamVertex(edgeKey, 1);
        int kept = findWindingFace(mesh, edgeKey, a, b);
        int removed = findWindingFace(mesh, edgeKey, b, a);
        int farA = findNeighbour(mesh, removed, a, b);
        int farB = findNeighbour(mesh, removed, b, a);
        int keptA = findCorner(mesh, kept, a);
        int keptB = findCorner(mesh, kept, b);
        int removedA = findCorner(mesh, removed, farA);
        int removedB = findCorner(mesh, removed, farB);
        boolean authored = mesh.hasAuthoredUVs(kept) || mesh.hasAuthoredUVs(removed);
        float uA = mesh.getFaceU(removed, removedA);
        float vA = mesh.getFaceV(removed, removedA);
        float uB = mesh.getFaceU(removed, removedB);
        float vB = mesh.getFaceV(removed, removedB);

        mesh.setFaceVertex(kept, keptA, farA);
        mesh.setFaceVertex(kept, keptB, farB);
        mesh.setFacePins(kept, 0);
        faceFlags.set(removed, true);

        if (!authored)
            return;

        mesh.setFaceUV(kept, keptA, uA, vA);
        mesh.setFaceUV(kept, keptB, uB, vB);
    }

    // Clean Up \\

    // Every selected vertex, or every vertex when none is, joined into the first one standing within the merge
    // distance of it; faces left with fewer than three corners go, seams follow their vertices
    void mergeByDistance(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();
        boolean selectedOnly = characterSelectionBranch.getSelectedCount(document) > 0;
        float reach = EditorSetting.CHARACTER_TOPOLOGY_MERGE_DISTANCE;
        int vertexCount = mesh.getVertexCount();
        int merged = 0;

        cell2Vertex.clear();
        cellNext.clear();
        cellNext.size(vertexCount);
        vertexRemap.clear();
        vertexRemap.size(vertexCount);

        for (int v = 0; v < vertexCount; v++) {

            vertexRemap.set(v, v);

            if (selectedOnly && !document.isSelected(v) || document.isHidden(v))
                continue;

            int near = findNearVertex(mesh, v, reach);

            if (near != EngineSetting.INDEX_NOT_FOUND) {
                vertexRemap.set(v, vertexRemap.getInt(near));
                merged++;
                continue;
            }

            long cell = CharacterMeshUtility.toCellKey(mesh.getX(v), mesh.getY(v), mesh.getZ(v), reach);
            cellNext.set(v, cell2Vertex.put(cell, v));
        }

        if (merged == 0) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_MERGED_NONE);
            return;
        }

        characterHistoryBranch.begin(document);
        carrySeams(mesh);
        faceFlags.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                mesh.setFaceVertex(f, c, vertexRemap.getInt(mesh.getFaceVertex(f, c)));

            faceFlags.add(countDistinct(mesh, f) < EngineSetting.QUAD_VERTEX_COUNT - 1);
        }

        mesh.removeFaces(faceFlags);
        characterSculptBranch.removeOrphans(document);

        if (characterMeshBranch.finishEdit(document))
            characterEditorManager.setStatusMessage(merged + EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_MERGED);
    }

    // The first vertex already binned that stands within the reach of a vertex, or INDEX_NOT_FOUND
    private int findNearVertex(RiggedMeshStruct mesh, int vertexIndex, float reach) {

        float x = mesh.getX(vertexIndex);
        float y = mesh.getY(vertexIndex);
        float z = mesh.getZ(vertexIndex);
        int cellX = (int) Math.floor(x / reach);
        int cellY = (int) Math.floor(y / reach);
        int cellZ = (int) Math.floor(z / reach);
        float reachSquared = reach * reach;

        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    for (int v = cell2Vertex.get(CharacterMeshUtility.packCell(cellX + dx, cellY + dy, cellZ + dz));
                            v != EngineSetting.INDEX_NOT_FOUND;
                            v = cellNext.getInt(v))
                        if (CharacterMeshUtility.distanceSquared(mesh, v, vertexIndex) <= reachSquared)
                            return v;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Every seam on a vertex merged away marked again between the vertices it now joins
    private void carrySeams(RiggedMeshStruct mesh) {

        long[] seams = mesh.getSortedSeams();

        for (int i = 0; i < seams.length; i++) {

            int a = RiggedMeshStruct.getSeamVertex(seams[i], 0);
            int b = RiggedMeshStruct.getSeamVertex(seams[i], 1);
            int mergedA = vertexRemap.getInt(a);
            int mergedB = vertexRemap.getInt(b);

            if ((mergedA != a || mergedB != b) && mergedA != mergedB)
                mesh.setSeam(mergedA, mergedB, true);
        }
    }

    // Every wholly selected face turned over, its corners wound the other way with their UVs and pins, and with
    // mirror on its twin as well
    void flipFaces(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        if (characterEditorManager.isMirror())
            characterSelectionBranch.extendMirror(document);

        int flipped = 0;

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (CharacterMeshUtility.isFaceSelected(mesh, f, document.getSelection()))
                flipped++;

        if (flipped == 0) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_NO_FACES);
            return;
        }

        characterHistoryBranch.begin(document);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (CharacterMeshUtility.isFaceSelected(mesh, f, document.getSelection()))
                reverseFace(mesh, f);

        if (characterMeshBranch.finishEdit(document))
            characterEditorManager.setStatusMessage(flipped + EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_FACES_FLIPPED);
    }

    // A face's distinct corners walked back from its first, its UVs and pins with them
    private void reverseFace(RiggedMeshStruct mesh, int faceIndex) {

        int pins = mesh.getFacePins(faceIndex);
        int reversedPins = 0;
        boolean authored = mesh.hasAuthoredUVs(faceIndex);

        collectCycle(mesh, faceIndex, cycleA, cornersA);
        vertexList.clear();
        cornerUVs.clear();

        for (int i = 0; i < cycleA.size(); i++) {

            int place = Math.floorMod(-i, cycleA.size());
            int corner = cornersA.getInt(place);

            vertexList.add(cycleA.getInt(place));
            cornerUVs.add(mesh.getFaceU(faceIndex, corner));
            cornerUVs.add(mesh.getFaceV(faceIndex, corner));

            if ((pins & 1 << corner) != 0)
                reversedPins |= 1 << i;
        }

        writeFace(mesh, faceIndex, vertexList, cornerUVs, authored);
        mesh.setFacePins(faceIndex, reversedPins);
    }

    // Every face with fewer than three corners or with no area removed, with the vertices it leaves unused
    void deleteDegenerate(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();
        int removed = 0;

        faceFlags.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            boolean degenerate = countDistinct(mesh, f) < EngineSetting.QUAD_VERTEX_COUNT - 1 || !hasArea(mesh, f);

            faceFlags.add(degenerate);

            if (degenerate)
                removed++;
        }

        if (removed == 0) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_DEGENERATE_NONE);
            return;
        }

        characterHistoryBranch.begin(document);
        mesh.removeFaces(faceFlags);
        characterSculptBranch.removeOrphans(document);

        if (characterMeshBranch.finishEdit(document))
            characterEditorManager.setStatusMessage(removed + EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_DEGENERATE);
    }

    // Whether a face's Newell normal, twice its area, measures more than the least area
    private boolean hasArea(RiggedMeshStruct mesh, int faceIndex) {

        normal.set(0f, 0f, 0f);

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int a = mesh.getFaceVertex(faceIndex, c);
            int b = mesh.getFaceVertex(faceIndex, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);

            normal.add(
                    (mesh.getY(a) - mesh.getY(b)) * (mesh.getZ(a) + mesh.getZ(b)),
                    (mesh.getZ(a) - mesh.getZ(b)) * (mesh.getX(a) + mesh.getX(b)),
                    (mesh.getX(a) - mesh.getX(b)) * (mesh.getY(a) + mesh.getY(b)));
        }

        return normal.lengthSquared() > EditorSetting.CHARACTER_TOPOLOGY_AREA_EPSILON;
    }

    // Exactly the inner vertices not four faces meet at, where an edge flow turns or ends
    void selectPoles(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        buildEdgeFaces(mesh);
        buildVertexFaces(mesh);
        vertexList.clear();

        for (int v = 0; v < mesh.getVertexCount(); v++)
            if (isPole(v))
                vertexList.add(v);

        selectFound(document);
    }

    // Exactly the corners of every face with three
    void selectTriangles(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        vertexList.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (countDistinct(mesh, f) == EngineSetting.QUAD_VERTEX_COUNT - 1)
                for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                    vertexList.add(mesh.getFaceVertex(f, c));

        selectFound(document);
    }

    // Exactly the ends of every edge one face or more than two stand on: holes, open rims and fins
    void selectNonManifold(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        buildEdgeFaces(mesh);
        vertexList.clear();

        for (Long2IntMap.Entry entry : edgeCounts.long2IntEntrySet())
            if (entry.getIntValue() != 2) {
                vertexList.add(RiggedMeshStruct.getSeamVertex(entry.getLongKey(), 0));
                vertexList.add(RiggedMeshStruct.getSeamVertex(entry.getLongKey(), 1));
            }

        selectFound(document);
    }

    private void selectFound(CharacterMeshDocumentInstance document) {

        characterSelectionBranch.select(document, vertexList, CharacterEditorSelection.REPLACE);
        characterEditorManager.setStatusMessage(characterSelectionBranch.getSelectedCount(document)
                + EditorSetting.CHARACTER_TOPOLOGY_MESSAGE_SELECTED);
    }

    private boolean isPole(int vertexIndex) {

        int faces = vertexFaceCounts.getInt(vertexIndex);

        return faces > 0 && faces != EngineSetting.QUAD_VERTEX_COUNT && !boundaryVertices.getBoolean(vertexIndex);
    }

    // Stats \\

    // The Properties tab's count of the mesh's quads, triangles and poles, counted again only when it moved
    String getStatsText(CharacterMeshDocumentInstance document) {

        if (document != statsDocument || document.getRevision() != statsRevision) {
            countStats(document.getMesh());
            this.statsDocument = document;
            this.statsRevision = document.getRevision();
        }

        return statsQuads + EditorSetting.CHARACTER_TOPOLOGY_LABEL_QUADS
                + EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR
                + statsTriangles + EditorSetting.CHARACTER_TOPOLOGY_LABEL_TRIANGLES
                + EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR
                + statsPoles + EditorSetting.CHARACTER_TOPOLOGY_LABEL_POLES;
    }

    private void countStats(RiggedMeshStruct mesh) {

        this.statsQuads = 0;
        this.statsTriangles = 0;
        this.statsPoles = 0;

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            int distinct = countDistinct(mesh, f);

            if (distinct == EngineSetting.QUAD_VERTEX_COUNT)
                statsQuads++;
            else if (distinct == EngineSetting.QUAD_VERTEX_COUNT - 1)
                statsTriangles++;
        }

        buildEdgeFaces(mesh);
        buildVertexFaces(mesh);

        for (int v = 0; v < mesh.getVertexCount(); v++)
            if (isPole(v))
                statsPoles++;
    }

    // Adjacency \\

    // How many faces stand on each edge and the first two of them, every face counted once on each of its edges
    private void buildEdgeFaces(RiggedMeshStruct mesh) {

        edgeCounts.clear();
        edgeFaces.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            collectCycle(mesh, f, cycleA, cornersA);

            if (cycleA.size() < EngineSetting.QUAD_VERTEX_COUNT - 1)
                continue;

            for (int i = 0; i < cycleA.size(); i++) {

                long key = RiggedMeshStruct.toEdgeKey(cycleA.getInt(i), cycleA.getInt((i + 1) % cycleA.size()));
                int before = edgeCounts.addTo(key, 1);

                if (before == 0)
                    edgeFaces.put(key, packFaces(f, EngineSetting.INDEX_NOT_FOUND));
                else if (before == 1)
                    edgeFaces.put(key, packFaces(unpackFirst(edgeFaces.get(key)), f));
            }
        }
    }

    // How many faces stand on each vertex, and which vertices stand on an edge only one face has
    private void buildVertexFaces(RiggedMeshStruct mesh) {

        vertexFaceCounts.clear();
        vertexFaceCounts.size(mesh.getVertexCount());
        boundaryVertices.clear();
        boundaryVertices.size(mesh.getVertexCount());

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            collectCycle(mesh, f, cycleA, cornersA);

            for (int i = 0; i < cycleA.size(); i++) {

                int vertexIndex = cycleA.getInt(i);

                vertexFaceCounts.set(vertexIndex, vertexFaceCounts.getInt(vertexIndex) + 1);
            }
        }

        for (Long2IntMap.Entry entry : edgeCounts.long2IntEntrySet())
            if (entry.getIntValue() == 1) {
                boundaryVertices.set(RiggedMeshStruct.getSeamVertex(entry.getLongKey(), 0), true);
                boundaryVertices.set(RiggedMeshStruct.getSeamVertex(entry.getLongKey(), 1), true);
            }
    }

    private static long packFaces(int first, int second) {
        return (long) first << Integer.SIZE | second & EditorSetting.CHARACTER_TOPOLOGY_FACE_MASK;
    }

    private static int unpackFirst(long packed) {
        return (int) (packed >> Integer.SIZE);
    }

    private static int unpackSecond(long packed) {
        return (int) packed;
    }

    // The face across an edge from the one given, or INDEX_NOT_FOUND where the edge is open or more than two share it
    private int findOtherFace(long edgeKey, int faceIndex) {

        if (edgeCounts.get(edgeKey) != 2)
            return EngineSetting.INDEX_NOT_FOUND;

        long packed = edgeFaces.get(edgeKey);
        int first = unpackFirst(packed);

        return first == faceIndex ? unpackSecond(packed) : first;
    }

    // The face of an edge whose corners run from a to b, or INDEX_NOT_FOUND
    private int findWindingFace(RiggedMeshStruct mesh, long edgeKey, int a, int b) {

        long packed = edgeFaces.get(edgeKey);
        int first = unpackFirst(packed);
        int second = unpackSecond(packed);

        if (runs(mesh, first, a, b))
            return first;

        return second != EngineSetting.INDEX_NOT_FOUND && runs(mesh, second, a, b)
                ? second
                : EngineSetting.INDEX_NOT_FOUND;
    }

    // Whether a face's corners run from a straight on to b
    private boolean runs(RiggedMeshStruct mesh, int faceIndex, int a, int b) {

        collectCycle(mesh, faceIndex, cycleB, cornersB);

        int at = cycleB.indexOf(a);

        return at != EngineSetting.INDEX_NOT_FOUND && cycleB.getInt((at + 1) % cycleB.size()) == b;
    }

    // The corner of a quad its edge to x or y starts at, whichever way it runs, or INDEX_NOT_FOUND
    private int findEdgeCorner(RiggedMeshStruct mesh, int faceIndex, int x, int y) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int from = mesh.getFaceVertex(faceIndex, c);
            int to = mesh.getFaceVertex(faceIndex, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);

            if (from == x && to == y || from == y && to == x)
                return c;
        }

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // The neighbour of a vertex round a face's corners that is not the one given, or INDEX_NOT_FOUND
    private int findNeighbour(RiggedMeshStruct mesh, int faceIndex, int vertexIndex, int other) {

        collectCycle(mesh, faceIndex, cycleB, cornersB);

        int at = cycleB.indexOf(vertexIndex);

        if (at == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        int before = cycleB.getInt(Math.floorMod(at - 1, cycleB.size()));
        int after = cycleB.getInt((at + 1) % cycleB.size());

        if (before == other)
            return after;

        return after == other ? before : EngineSetting.INDEX_NOT_FOUND;
    }

    private int findCorner(RiggedMeshStruct mesh, int faceIndex, int vertexIndex) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (mesh.getFaceVertex(faceIndex, c) == vertexIndex)
                return c;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Faces \\

    // A face's distinct corners in winding order, a doubled corner kept once, with the corner each was read from
    private void collectCycle(RiggedMeshStruct mesh, int faceIndex, IntArrayList vertices, IntArrayList corners) {

        vertices.clear();
        corners.clear();

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int vertexIndex = mesh.getFaceVertex(faceIndex, c);

            if (vertices.contains(vertexIndex))
                continue;

            vertices.add(vertexIndex);
            corners.add(c);
        }
    }

    private int countDistinct(RiggedMeshStruct mesh, int faceIndex) {

        collectCycle(mesh, faceIndex, cycleB, cornersB);
        return cycleB.size();
    }

    // A cycle and its corners turned so the vertex given leads
    private void rotateTo(IntArrayList vertices, IntArrayList corners, int vertexIndex) {

        int at = vertices.indexOf(vertexIndex);

        for (int i = 0; i < at; i++) {
            vertices.add(vertices.removeInt(0));
            corners.add(corners.removeInt(0));
        }
    }

    // A face written over three or four corners in order, a triangle's last corner doubled, its UVs set when its
    // faces authored theirs and its pins cleared
    private void writeFace(
            RiggedMeshStruct mesh,
            int faceIndex,
            IntArrayList vertices,
            FloatArrayList uvs,
            boolean authored) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int place = Math.min(c, vertices.size() - 1);

            mesh.setFaceVertex(faceIndex, c, vertices.getInt(place));

            if (authored)
                mesh.setFaceUV(faceIndex, c, uvs.getFloat(place * 2), uvs.getFloat(place * 2 + 1));
        }

        mesh.setFacePins(faceIndex, 0);
    }
}
