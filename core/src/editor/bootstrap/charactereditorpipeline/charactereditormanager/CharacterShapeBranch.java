package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterStampStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

class CharacterShapeBranch extends BranchPackage {

    /*
     * Reshapes a character mesh's surface under the sculpt brush. A stroke
     * opens one undo step and takes an unrounded copy of every position, the
     * faces and neighbours each vertex touches and the vertex each mirrors
     * onto, all fixed while it lasts, so countless small stamps add up
     * exactly and the mirror holds however far the surface moves. Each stamp
     * works out every vertex's move from the surface as it stood, then
     * applies them together: Brush draws the area out along its normal,
     * Inflate swells each vertex along its own, Smooth relaxes it toward its
     * neighbours, Pinch draws it toward the brush's centre across the line
     * of sight, Flatten levels it onto the area's plane and Nudge carries it
     * with the pointer; an inverted stroke does the opposite. With mirror
     * on, every move is handed across x to the vertex's mirror and the two
     * share it where both lie under the brush. Positions are written rounded
     * to the editor's precision; the manager shows them once a frame however
     * many stamps the frame held.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterMeshBranch characterMeshBranch;
    private CharacterHistoryBranch characterHistoryBranch;
    private CharacterSelectionBranch characterSelectionBranch;

    // Stroke
    private RiggedMeshStruct mesh;
    private CharacterMeshDocumentInstance strokeDocument;
    private FloatArrayList positions;
    private IntArrayList mirrors;
    private boolean mirrored;

    // Topology — each vertex's faces and neighbours, packed one run per vertex
    private IntArrayList faceStarts;
    private IntArrayList vertexFaces;
    private IntArrayList neighbourStarts;
    private IntArrayList neighbours;
    private LongOpenHashSet edges;

    // Moves
    private FloatArrayList moves;
    private FloatArrayList accumulated;
    private FloatArrayList shares;
    private IntArrayList touched;

    // Scratch
    private Vector3 normal;
    private Vector3 areaNormal;
    private Vector3 areaCenter;
    private Vector3 move;
    private Vector3 faceScratch;

    // Base \\

    @Override
    protected void create() {

        // Stroke
        this.positions = new FloatArrayList();
        this.mirrors = new IntArrayList();

        // Topology
        this.faceStarts = new IntArrayList();
        this.vertexFaces = new IntArrayList();
        this.neighbourStarts = new IntArrayList();
        this.neighbours = new IntArrayList();
        this.edges = new LongOpenHashSet();

        // Moves
        this.moves = new FloatArrayList();
        this.accumulated = new FloatArrayList();
        this.shares = new FloatArrayList();
        this.touched = new IntArrayList();

        // Scratch
        this.normal = new Vector3();
        this.areaNormal = new Vector3();
        this.areaCenter = new Vector3();
        this.move = new Vector3();
        this.faceScratch = new Vector3();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterMeshBranch = get(CharacterMeshBranch.class);
        this.characterHistoryBranch = get(CharacterHistoryBranch.class);
        this.characterSelectionBranch = get(CharacterSelectionBranch.class);
    }

    // Stroke \\

    void beginStroke(CharacterMeshDocumentInstance document) {

        this.mesh = document.getMesh();
        this.strokeDocument = document;

        int vertexCount = mesh.getVertexCount();
        int floats = vertexCount * EngineSetting.RIGGED_MESH_POSITION_FLOATS;

        characterHistoryBranch.begin(document);
        positions.clear();
        positions.ensureCapacity(floats);

        for (int v = 0; v < vertexCount; v++) {
            positions.add(mesh.getX(v));
            positions.add(mesh.getY(v));
            positions.add(mesh.getZ(v));
        }

        this.mirrored = characterEditorManager.isMirror();
        mirrors.clear();

        if (mirrored)
            mirrors.addAll(characterSelectionBranch.resolveMirror(document));

        buildTopology();

        accumulated.clear();
        accumulated.size(floats);
        shares.clear();
        shares.size(vertexCount);
    }

    // One stamp of the tool over the vertices under the brush, each at the strength the brush has there
    void stroke(
            CharacterMeshDocumentInstance document,
            EditorTool tool,
            IntArrayList vertices,
            FloatArrayList strengths,
            CharacterStampStruct stamp) {

        if (vertices.isEmpty() || mesh != document.getMesh())
            return;

        resolveArea(vertices, strengths);
        moves.clear();

        for (int i = 0; i < vertices.size(); i++) {
            resolveMove(tool, vertices.getInt(i), strengths.getFloat(i), stamp);
            moves.add(move.x);
            moves.add(move.y);
            moves.add(move.z);
        }

        for (int i = 0; i < vertices.size(); i++)
            share(vertices.getInt(i), moves.getFloat(i * 3), moves.getFloat(i * 3 + 1), moves.getFloat(i * 3 + 2));

        apply();
    }

    void endStroke(CharacterMeshDocumentInstance document) {

        if (document.hasOpenStep())
            characterMeshBranch.finishEdit(document);

        this.mesh = null;
        this.strokeDocument = null;
        positions.clear();
        mirrors.clear();
    }

    // Moves \\

    private void resolveMove(EditorTool tool, int vertexIndex, float strength, CharacterStampStruct stamp) {

        float reach = strength * stamp.getRadius() * stamp.getDirection();
        int base = vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
        float x = positions.getFloat(base);
        float y = positions.getFloat(base + 1);
        float z = positions.getFloat(base + 2);

        switch (tool) {
            case BRUSH -> move.set(areaNormal).multiply(reach * EditorSetting.CHARACTER_SCULPT_DRAW_RATE);
            case INFLATE -> {
                resolveVertexNormal(vertexIndex, normal);
                move.set(normal).multiply(reach * EditorSetting.CHARACTER_SCULPT_INFLATE_RATE);
            }
            case SMOOTH -> {
                resolveNeighbourAverage(vertexIndex, move);
                move.subtract(x, y, z).multiply(strength * EditorSetting.CHARACTER_SCULPT_SMOOTH_RATE);
            }
            case PINCH -> {
                Vector3 view = stamp.getViewDirection();
                Vector3 center = stamp.getCenter();

                move.set(center.x - x, center.y - y, center.z - z);
                float along = move.x * view.x + move.y * view.y + move.z * view.z;
                move.subtract(view.x * along, view.y * along, view.z * along)
                        .multiply(strength * stamp.getDirection() * EditorSetting.CHARACTER_SCULPT_PINCH_RATE);
            }
            case FLATTEN -> {
                float distance = (x - areaCenter.x) * areaNormal.x
                        + (y - areaCenter.y) * areaNormal.y
                        + (z - areaCenter.z) * areaNormal.z;
                move.set(areaNormal).multiply(
                        -distance * strength * stamp.getDirection() * EditorSetting.CHARACTER_SCULPT_FLATTEN_RATE);
            }
            case NUDGE -> move.set(stamp.getDrag()).multiply(strength);
            default -> move.set(0f, 0f, 0f);
        }
    }

    // A vertex's move, and with mirror on its mirror's, added to what each takes from this stamp
    private void share(int vertexIndex, float dx, float dy, float dz) {

        accumulate(vertexIndex, dx, dy, dz);

        if (!mirrored)
            return;

        int mirror = mirrors.getInt(vertexIndex);

        if (mirror != EngineSetting.INDEX_NOT_FOUND && characterEditorManager.isInRegion(strokeDocument, mirror))
            accumulate(mirror, -dx, dy, dz);
    }

    private void accumulate(int vertexIndex, float dx, float dy, float dz) {

        int base = vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS;

        if (shares.getFloat(vertexIndex) == 0f)
            touched.add(vertexIndex);

        accumulated.set(base, accumulated.getFloat(base) + dx);
        accumulated.set(base + 1, accumulated.getFloat(base + 1) + dy);
        accumulated.set(base + 2, accumulated.getFloat(base + 2) + dz);
        shares.set(vertexIndex, shares.getFloat(vertexIndex) + 1f);
    }

    // Every vertex the stamp reached moved by the mean of what it took, written rounded, the stamp's moves cleared
    private void apply() {

        for (int i = 0; i < touched.size(); i++) {

            int vertexIndex = touched.getInt(i);
            int base = vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
            float share = shares.getFloat(vertexIndex);

            for (int axis = 0; axis < EngineSetting.RIGGED_MESH_POSITION_FLOATS; axis++) {
                positions.set(base + axis, positions.getFloat(base + axis) + accumulated.getFloat(base + axis) / share);
                accumulated.set(base + axis, 0f);
            }

            shares.set(vertexIndex, 0f);
            CharacterMeshUtility.setRoundedPosition(
                    mesh,
                    vertexIndex,
                    positions.getFloat(base),
                    positions.getFloat(base + 1),
                    positions.getFloat(base + 2));
        }

        touched.clear();
    }

    // Area \\

    // The brushed area's centre and facing, each vertex counted by the strength the brush has on it
    private void resolveArea(IntArrayList vertices, FloatArrayList strengths) {

        float total = 0f;

        areaCenter.set(0f, 0f, 0f);
        areaNormal.set(0f, 0f, 0f);

        for (int i = 0; i < vertices.size(); i++) {

            int vertexIndex = vertices.getInt(i);
            int base = vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
            float strength = strengths.getFloat(i);

            areaCenter.add(
                    positions.getFloat(base) * strength,
                    positions.getFloat(base + 1) * strength,
                    positions.getFloat(base + 2) * strength);
            resolveVertexNormal(vertexIndex, normal);
            areaNormal.add(normal.x * strength, normal.y * strength, normal.z * strength);
            total += strength;
        }

        if (total > 0f)
            areaCenter.divide(total);

        normalize(areaNormal);
    }

    // The sum of the faces a vertex stands on, each by its own facing, as a unit vector
    private void resolveVertexNormal(int vertexIndex, Vector3 out) {

        out.set(0f, 0f, 0f);

        for (int i = faceStarts.getInt(vertexIndex); i < faceStarts.getInt(vertexIndex + 1); i++) {
            resolveFaceNormal(vertexFaces.getInt(i), faceScratch);
            out.add(faceScratch);
        }

        normalize(out);
    }

    // A quad's facing across its diagonals, as its winding gives it, scaled by its size
    private void resolveFaceNormal(int faceIndex, Vector3 out) {

        int a = mesh.getFaceVertex(faceIndex, 0) * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
        int b = mesh.getFaceVertex(faceIndex, 1) * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
        int c = mesh.getFaceVertex(faceIndex, 2) * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
        int d = mesh.getFaceVertex(faceIndex, 3) * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
        float ux = positions.getFloat(c) - positions.getFloat(a);
        float uy = positions.getFloat(c + 1) - positions.getFloat(a + 1);
        float uz = positions.getFloat(c + 2) - positions.getFloat(a + 2);
        float vx = positions.getFloat(d) - positions.getFloat(b);
        float vy = positions.getFloat(d + 1) - positions.getFloat(b + 1);
        float vz = positions.getFloat(d + 2) - positions.getFloat(b + 2);

        out.set(uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx);
    }

    private void resolveNeighbourAverage(int vertexIndex, Vector3 out) {

        int start = neighbourStarts.getInt(vertexIndex);
        int end = neighbourStarts.getInt(vertexIndex + 1);
        int base = vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS;

        if (start == end) {
            out.set(positions.getFloat(base), positions.getFloat(base + 1), positions.getFloat(base + 2));
            return;
        }

        out.set(0f, 0f, 0f);

        for (int i = start; i < end; i++) {

            int neighbour = neighbours.getInt(i) * EngineSetting.RIGGED_MESH_POSITION_FLOATS;

            out.add(
                    positions.getFloat(neighbour),
                    positions.getFloat(neighbour + 1),
                    positions.getFloat(neighbour + 2));
        }

        out.divide(end - start);
    }

    private void normalize(Vector3 vector) {

        float epsilon = EditorSetting.CHARACTER_MESH_SPAN_EPSILON;

        if (vector.lengthSquared() > epsilon * epsilon)
            vector.normalize();
        else
            vector.set(0f, 0f, 0f);
    }

    // Topology \\

    // Each vertex's faces and its neighbours along their edges, one run per vertex in vertex order
    private void buildTopology() {

        int vertexCount = mesh.getVertexCount();

        fill(faceStarts, vertexCount + 1);
        fill(neighbourStarts, vertexCount + 1);
        edges.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int a = mesh.getFaceVertex(f, c);
                int b = mesh.getFaceVertex(f, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);

                if (!isRepeatedCorner(f, c))
                    faceStarts.set(a + 1, faceStarts.getInt(a + 1) + 1);

                if (a != b && edges.add(toEdgeKey(a, b))) {
                    neighbourStarts.set(a + 1, neighbourStarts.getInt(a + 1) + 1);
                    neighbourStarts.set(b + 1, neighbourStarts.getInt(b + 1) + 1);
                }
            }

        accumulateStarts(faceStarts);
        accumulateStarts(neighbourStarts);
        fillRuns();
    }

    private void fillRuns() {

        int vertexCount = mesh.getVertexCount();

        fill(vertexFaces, faceStarts.getInt(vertexCount));
        fill(neighbours, neighbourStarts.getInt(vertexCount));
        shares.clear();
        shares.size(vertexCount * 2);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                if (!isRepeatedCorner(f, c))
                    placeRun(vertexFaces, faceStarts, mesh.getFaceVertex(f, c), 0, f);

        LongIterator edgeIterator = edges.iterator();

        while (edgeIterator.hasNext()) {

            long edgeKey = edgeIterator.nextLong();
            int a = (int) (edgeKey >>> Integer.SIZE);
            int b = (int) edgeKey;

            placeRun(neighbours, neighbourStarts, a, vertexCount, b);
            placeRun(neighbours, neighbourStarts, b, vertexCount, a);
        }
    }

    // Writes one entry into a vertex's run, counting the run's fill in the shares scratch at an offset
    private void placeRun(IntArrayList runs, IntArrayList starts, int vertexIndex, int offset, int value) {

        int filled = (int) shares.getFloat(offset + vertexIndex);

        runs.set(starts.getInt(vertexIndex) + filled, value);
        shares.set(offset + vertexIndex, filled + 1);
    }

    // A corner standing on the same vertex as an earlier one of its face, as a quad folded into a triangle has
    private boolean isRepeatedCorner(int faceIndex, int corner) {

        for (int c = 0; c < corner; c++)
            if (mesh.getFaceVertex(faceIndex, c) == mesh.getFaceVertex(faceIndex, corner))
                return true;

        return false;
    }

    private void accumulateStarts(IntArrayList starts) {
        for (int i = 1; i < starts.size(); i++)
            starts.set(i, starts.getInt(i) + starts.getInt(i - 1));
    }

    private long toEdgeKey(int a, int b) {
        return (long) Math.min(a, b) << Integer.SIZE | Math.max(a, b);
    }

    private void fill(IntArrayList list, int size) {
        list.clear();
        list.size(size);
    }
}
