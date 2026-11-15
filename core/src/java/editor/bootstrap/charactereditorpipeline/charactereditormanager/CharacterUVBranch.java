package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSelection;
import editor.bootstrap.charactereditorpipeline.util.CharacterUnwrapUtility;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterUVBranch extends BranchPackage {

    /*
     * Every UV edit of the part open, as Blender's UV Editing workspace
     * makes them. The faces the UV Editor shows are those whose four
     * vertices are selected; its own selection is of face corners, a pick
     * taking every corner of the same vertex at the same place. Seams are
     * marked on the edges between selected vertices, or found from the UV
     * islands already laid. Unwrap lays each island of the faces shown, cut
     * apart by the seams, flat by least squares conformal mapping, then
     * evens their scale and packs them into the tile. Pack, Average Scale,
     * Stitch (an island carried onto its neighbour across the seam and its
     * corners welded there), Pin, Unpin and the flips work on the islands
     * or corners selected; the Move, Rotate and Scale tools drag the
     * selected corners about their centre. Every change to the mesh is one
     * undo step of the part's history; selecting is not.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterHistoryBranch characterHistoryBranch;
    private CharacterMeshBranch characterMeshBranch;

    // Scratch — and the centre of the corners selected, as last found
    private IntArrayList faceScratch;
    private IntArrayList cornerScratch;
    private float pivotU;
    private float pivotV;

    // Transform — the corners dragged with their UVs as they started, the centre they turn and scale about, the point
    // the drag started at and the tool dragging
    private CharacterMeshDocumentInstance transformDocument;
    private IntArrayList transformCorners;
    private FloatArrayList transformStart;
    private float transformPivotU;
    private float transformPivotV;
    private float grabU;
    private float grabV;
    private EditorTool transformTool;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.faceScratch = new IntArrayList();
        this.cornerScratch = new IntArrayList();

        // Transform
        this.transformCorners = new IntArrayList();
        this.transformStart = new FloatArrayList();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterHistoryBranch = get(CharacterHistoryBranch.class);
        this.characterMeshBranch = get(CharacterMeshBranch.class);
    }

    // Keys \\

    boolean handles(String keyName) {
        return switch (keyName) {
            case EditorSetting.UV_KEY_MARK_SEAM,
                    EditorSetting.UV_KEY_CLEAR_SEAM,
                    EditorSetting.UV_KEY_SEAMS_FROM_ISLANDS,
                    EditorSetting.UV_KEY_UNWRAP,
                    EditorSetting.UV_KEY_PACK,
                    EditorSetting.UV_KEY_AVERAGE_SCALE,
                    EditorSetting.UV_KEY_STITCH,
                    EditorSetting.UV_KEY_PIN,
                    EditorSetting.UV_KEY_UNPIN,
                    EditorSetting.UV_KEY_FLIP_U,
                    EditorSetting.UV_KEY_FLIP_V,
                    EditorSetting.UV_KEY_SELECT_ALL,
                    EditorSetting.UV_KEY_SELECT_LINKED,
                    EditorSetting.UV_KEY_CENTER -> true;
            default -> false;
        };
    }

    // An action by its key on the part open, never while a drag is under way
    void act(String key) {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();

        if (document == null || isTransforming())
            return;

        switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.UV_KEY_MARK_SEAM -> markSeams(document, true);
            case EditorSetting.UV_KEY_CLEAR_SEAM -> markSeams(document, false);
            case EditorSetting.UV_KEY_SEAMS_FROM_ISLANDS -> seamsFromIslands(document);
            case EditorSetting.UV_KEY_UNWRAP -> unwrap(document);
            case EditorSetting.UV_KEY_PACK -> pack(document);
            case EditorSetting.UV_KEY_AVERAGE_SCALE -> averageScale(document);
            case EditorSetting.UV_KEY_STITCH -> stitch(document);
            case EditorSetting.UV_KEY_PIN -> pin(document, true);
            case EditorSetting.UV_KEY_UNPIN -> pin(document, false);
            case EditorSetting.UV_KEY_FLIP_U -> flip(document, 0);
            case EditorSetting.UV_KEY_FLIP_V -> flip(document, 1);
            case EditorSetting.UV_KEY_SELECT_ALL -> toggleAll(document);
            case EditorSetting.UV_KEY_SELECT_LINKED -> selectLinked(document);
            default -> {
            }
        }
    }

    // Properties \\

    // The UVs selected in the UV Editor as the Properties tab fine-tunes them: the U and V of their centre
    void buildPropertyRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();

        if (document == null || !resolveCenter(document)) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.UV_MESSAGE_NO_CORNERS));
            return;
        }

        rows.add(CharacterViewRowStruct.header(EditorSetting.UV_HEADER_SELECTED + cornerScratch.size()));

        for (int axis = 0; axis < EditorSetting.UV_CENTER_LABELS.length; axis++) {

            String key = CharacterViewUtility.toKey(EditorSetting.UV_KEY_CENTER, axis);
            rows.add(CharacterViewRowStruct.number(key, EditorSetting.UV_CENTER_LABELS[axis], getValueText(key)));
        }
    }

    String getValueText(String key) {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();

        if (document == null || !resolveCenter(document))
            return "";

        return CharacterViewUtility.formatNumber(CharacterViewUtility.getKeyIndex(key) == 0 ? pivotU : pivotV);
    }

    boolean isValueValid(String key, String text) {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();

        return document != null && !isTransforming() && CharacterViewUtility.isNumber(text) && resolveCenter(document);
    }

    void setValue(String key, String text) {

        if (isValueValid(key, text))
            moveCenter(CharacterViewUtility.getKeyIndex(key), CharacterViewUtility.parseNumber(text));
    }

    void step(String key, int direction) {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();

        if (document == null || isTransforming() || !resolveCenter(document))
            return;

        int axis = CharacterViewUtility.getKeyIndex(key);

        moveCenter(axis, (axis == 0 ? pivotU : pivotV) + direction * EditorSetting.UV_CENTER_STEP);
    }

    // The selected corners moved together so their centre stands at a value along u (axis 0) or v (axis 1)
    private void moveCenter(int axis, float value) {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();
        RiggedMeshStruct mesh = document.getMesh();
        float offset = value - (axis == 0 ? pivotU : pivotV);

        characterHistoryBranch.begin(document);

        for (int i = 0; i < cornerScratch.size(); i++) {

            int corner = cornerScratch.getInt(i);

            setUV(mesh, corner, getU(mesh, corner) + (axis == 0 ? offset : 0f),
                    getV(mesh, corner) + (axis == 1 ? offset : 0f));
        }

        characterMeshBranch.finishEdit(document);
    }

    // The selected corners gathered and their centre found as the pivot; false with none selected
    private boolean resolveCenter(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        collectSelectedCorners(document, cornerScratch);

        if (cornerScratch.isEmpty())
            return false;

        this.pivotU = 0f;
        this.pivotV = 0f;

        for (int i = 0; i < cornerScratch.size(); i++) {
            pivotU += getU(mesh, cornerScratch.getInt(i));
            pivotV += getV(mesh, cornerScratch.getInt(i));
        }

        this.pivotU /= cornerScratch.size();
        this.pivotV /= cornerScratch.size();

        return true;
    }

    // Faces \\

    // The faces the UV Editor shows: every face whose four vertices are selected and shown
    void collectShownFaces(CharacterMeshDocumentInstance document, IntArrayList faces) {

        RiggedMeshStruct mesh = document.getMesh();

        faces.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (isShown(document, f))
                faces.add(f);
    }

    boolean isShown(CharacterMeshDocumentInstance document, int faceIndex) {

        RiggedMeshStruct mesh = document.getMesh();

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int vertexIndex = mesh.getFaceVertex(faceIndex, c);

            if (!document.isSelected(vertexIndex) || document.isHidden(vertexIndex))
                return false;
        }

        return true;
    }

    // Seams \\

    // Every edge between two selected vertices marked as a seam or cleared
    private void markSeams(CharacterMeshDocumentInstance document, boolean seam) {

        RiggedMeshStruct mesh = document.getMesh();
        int changed = 0;

        characterHistoryBranch.begin(document);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int vertexA = mesh.getFaceVertex(f, c);
                int vertexB = mesh.getFaceVertex(f, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);

                if (!isEdgeSelected(document, vertexA, vertexB) || mesh.isSeam(vertexA, vertexB) == seam)
                    continue;

                mesh.setSeam(vertexA, vertexB, seam);
                changed++;
            }

        finish(document, changed, EditorSetting.UV_MESSAGE_NO_EDGES,
                seam ? EditorSetting.UV_MESSAGE_SEAMS_MARKED : EditorSetting.UV_MESSAGE_SEAMS_CLEARED);
    }

    private boolean isEdgeSelected(CharacterMeshDocumentInstance document, int vertexA, int vertexB) {
        return document.isSelected(vertexA) && document.isSelected(vertexB) && !document.isHidden(vertexA)
                && !document.isHidden(vertexB);
    }

    // A seam on every edge where two faces shown meet in different UV islands
    private void seamsFromIslands(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        collectShownFaces(document, faceScratch);

        int[] islands = new int[faceScratch.size()];
        Long2IntOpenHashMap edge2Face = new Long2IntOpenHashMap();
        int changed = 0;

        resolveIslands(mesh, faceScratch, true, islands);
        edge2Face.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        characterHistoryBranch.begin(document);

        for (int i = 0; i < faceScratch.size(); i++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int vertexA = mesh.getFaceVertex(faceScratch.getInt(i), c);
                int vertexB = mesh.getFaceVertex(faceScratch.getInt(i), (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);
                long edgeKey = RiggedMeshStruct.toEdgeKey(vertexA, vertexB);
                int other = edge2Face.putIfAbsent(edgeKey, i);

                if (other == EngineSetting.INDEX_NOT_FOUND || islands[other] == islands[i]
                        || mesh.isSeam(vertexA, vertexB))
                    continue;

                mesh.setSeam(vertexA, vertexB, true);
                changed++;
            }

        finish(document, changed, EditorSetting.UV_MESSAGE_NO_ISLAND_EDGES, EditorSetting.UV_MESSAGE_SEAMS_MARKED);
    }

    // Unwrap \\

    // Each island of the faces shown, cut by the seams, laid flat, then evened in scale and packed into the tile
    private void unwrap(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        collectShownFaces(document, faceScratch);

        if (faceScratch.isEmpty()) {
            characterEditorManager.setStatusMessage(EditorSetting.UV_MESSAGE_NO_FACES);
            return;
        }

        int[] islands = new int[faceScratch.size()];
        ObjectArrayList<IntArrayList> islandFaces = groupIslands(
                faceScratch, islands, resolveIslands(mesh, faceScratch, false, islands));
        int failed = 0;

        characterHistoryBranch.begin(document);

        for (int i = 0; i < islandFaces.size(); i++)
            if (!CharacterUnwrapUtility.unwrap(mesh, islandFaces.get(i)))
                failed++;

        averageIslands(mesh, islandFaces);
        packIslands(mesh, islandFaces);

        if (characterMeshBranch.finishEdit(document))
            characterEditorManager.setStatusMessage(failed == 0
                    ? EditorSetting.UV_MESSAGE_UNWRAPPED + islandFaces.size()
                    : EditorSetting.UV_MESSAGE_UNWRAP_FAILED + failed);
    }

    // Islands \\

    // The islands of a set of faces, each face's island written in order of first appearance: faces join across an
    // edge they share where its two corners meet in UV, or, unwrapping, where it is no seam; the island count
    private int resolveIslands(RiggedMeshStruct mesh, IntArrayList faces, boolean byUV, int[] islands) {

        Long2IntOpenHashMap edge2Corner = new Long2IntOpenHashMap();
        int[] parents = new int[faces.size()];

        edge2Corner.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        for (int i = 0; i < parents.length; i++)
            parents[i] = i;

        for (int i = 0; i < faces.size(); i++) {

            int faceIndex = faces.getInt(i);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int next = (c + 1) % EngineSetting.QUAD_VERTEX_COUNT;
                int vertexA = mesh.getFaceVertex(faceIndex, c);
                int vertexB = mesh.getFaceVertex(faceIndex, next);
                int other = edge2Corner.putIfAbsent(
                        RiggedMeshStruct.toEdgeKey(vertexA, vertexB), i * EngineSetting.QUAD_VERTEX_COUNT + c);

                if (other == EngineSetting.INDEX_NOT_FOUND)
                    continue;

                int otherLocal = other / EngineSetting.QUAD_VERTEX_COUNT;
                boolean joined = byUV
                        ? meetsInUV(mesh, faceIndex, faces.getInt(otherLocal), vertexA, vertexB)
                        : !mesh.isSeam(vertexA, vertexB);

                if (joined)
                    union(parents, i, otherLocal);
            }
        }

        int[] root2Island = new int[faces.size()];
        int islandCount = 0;

        Arrays.fill(root2Island, EngineSetting.INDEX_NOT_FOUND);

        for (int i = 0; i < faces.size(); i++) {

            int root = find(parents, i);

            if (root2Island[root] == EngineSetting.INDEX_NOT_FOUND)
                root2Island[root] = islandCount++;

            islands[i] = root2Island[root];
        }

        return islandCount;
    }

    // Whether two faces place both ends of the edge they share at the same UVs
    private boolean meetsInUV(RiggedMeshStruct mesh, int faceA, int faceB, int vertexA, int vertexB) {
        return sameUV(mesh, faceA, findCorner(mesh, faceA, vertexA), faceB, findCorner(mesh, faceB, vertexA))
                && sameUV(mesh, faceA, findCorner(mesh, faceA, vertexB), faceB, findCorner(mesh, faceB, vertexB));
    }

    private boolean sameUV(RiggedMeshStruct mesh, int faceA, int cornerA, int faceB, int cornerB) {
        return Math.abs(mesh.getFaceU(faceA, cornerA) - mesh.getFaceU(faceB, cornerB))
                <= EditorSetting.UV_WELD_EPSILON
                && Math.abs(mesh.getFaceV(faceA, cornerA) - mesh.getFaceV(faceB, cornerB))
                        <= EditorSetting.UV_WELD_EPSILON;
    }

    private int findCorner(RiggedMeshStruct mesh, int faceIndex, int vertexIndex) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (mesh.getFaceVertex(faceIndex, c) == vertexIndex)
                return c;

        return 0;
    }

    private int find(int[] parents, int i) {

        while (parents[i] != i) {
            parents[i] = parents[parents[i]];
            i = parents[i];
        }

        return i;
    }

    private void union(int[] parents, int a, int b) {
        parents[find(parents, a)] = find(parents, b);
    }

    // The faces of each island, island by island
    private ObjectArrayList<IntArrayList> groupIslands(IntArrayList faces, int[] islands, int islandCount) {

        ObjectArrayList<IntArrayList> islandFaces = new ObjectArrayList<>(islandCount);

        for (int i = 0; i < islandCount; i++)
            islandFaces.add(new IntArrayList());

        for (int i = 0; i < faces.size(); i++)
            islandFaces.get(islands[i]).add(faces.getInt(i));

        return islandFaces;
    }

    // The UV islands holding a selected corner among the faces shown
    private ObjectArrayList<IntArrayList> resolveSelectedIslands(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        collectShownFaces(document, faceScratch);

        int[] islands = new int[faceScratch.size()];
        int islandCount = resolveIslands(mesh, faceScratch, true, islands);
        boolean[] selected = new boolean[islandCount];

        for (int i = 0; i < faceScratch.size(); i++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                selected[islands[i]] |= document.isCornerSelected(faceScratch.getInt(i), c);

        ObjectArrayList<IntArrayList> islandFaces = groupIslands(faceScratch, islands, islandCount);

        for (int i = islandCount - 1; i >= 0; i--)
            if (!selected[i])
                islandFaces.remove(i);

        return islandFaces;
    }

    // Pack \\

    private void pack(CharacterMeshDocumentInstance document) {

        ObjectArrayList<IntArrayList> islandFaces = resolveSelectedIslands(document);

        if (islandFaces.isEmpty()) {
            characterEditorManager.setStatusMessage(EditorSetting.UV_MESSAGE_NO_CORNERS);
            return;
        }

        characterHistoryBranch.begin(document);
        packIslands(document.getMesh(), islandFaces);
        characterMeshBranch.finishEdit(document);
    }

    // Every island laid into the tile in shelves, tallest first, a margin between them, scaled together to fit
    private void packIslands(RiggedMeshStruct mesh, ObjectArrayList<IntArrayList> islandFaces) {

        int count = islandFaces.size();
        float[] bounds = new float[count * 4];

        for (int i = 0; i < count; i++)
            resolveBounds(mesh, islandFaces.get(i), bounds, i * 4);

        float[] placed = new float[count * 2];
        float extent = shelve(bounds, placed, 0f);
        float padding = extent * EditorSetting.UV_PACK_MARGIN;

        extent = shelve(bounds, placed, padding) + padding;

        if (extent <= EditorSetting.UV_UNWRAP_EPSILON)
            return;

        float scale = 1f / extent;

        for (int i = 0; i < count; i++)
            transformIsland(
                    mesh,
                    islandFaces.get(i),
                    bounds[i * 4],
                    bounds[i * 4 + 1],
                    placed[i * 2],
                    placed[i * 2 + 1],
                    scale);
    }

    // Shelves of boxes, tallest first, as wide as the boxes' area suggests: each box's place written, the square
    // side the shelves fill returned
    private float shelve(float[] bounds, float[] placed, float padding) {

        int count = placed.length / 2;
        int[] order = new int[count];
        float area = 0f;
        float widest = 0f;

        for (int i = 0; i < count; i++) {
            order[i] = i;
            area += (width(bounds, i) + padding) * (height(bounds, i) + padding);
            widest = Math.max(widest, width(bounds, i) + padding);
        }

        IntArrays.quickSort(order, (a, b) -> Float.compare(height(bounds, b), height(bounds, a)));

        float shelfWidth = Math.max(widest, (float) Math.sqrt(area) * EditorSetting.UV_PACK_SHELF_WIDTH);
        float x = padding;
        float y = padding;
        float shelfHeight = 0f;
        float usedWidth = 0f;

        for (int i = 0; i < count; i++) {

            int island = order[i];
            float w = width(bounds, island) + padding;
            float h = height(bounds, island) + padding;

            if (x > padding && x + w > shelfWidth + padding) {
                y += shelfHeight;
                x = padding;
                shelfHeight = 0f;
            }

            placed[island * 2] = x;
            placed[island * 2 + 1] = y;
            x += w;
            shelfHeight = Math.max(shelfHeight, h);
            usedWidth = Math.max(usedWidth, x);
        }

        return Math.max(usedWidth, y + shelfHeight);
    }

    private float width(float[] bounds, int island) {
        return bounds[island * 4 + 2] - bounds[island * 4];
    }

    private float height(float[] bounds, int island) {
        return bounds[island * 4 + 3] - bounds[island * 4 + 1];
    }

    // An island's corners moved so its box's lower corner stands at a place, then scaled from the tile's origin
    private void transformIsland(
            RiggedMeshStruct mesh,
            IntArrayList faces,
            float fromU,
            float fromV,
            float toU,
            float toV,
            float scale) {

        for (int i = 0; i < faces.size(); i++) {

            int faceIndex = faces.getInt(i);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                mesh.setFaceUV(
                        faceIndex,
                        c,
                        (mesh.getFaceU(faceIndex, c) - fromU + toU) * scale,
                        (mesh.getFaceV(faceIndex, c) - fromV + toV) * scale);
        }
    }

    // The box an island's corners span: min u, min v, max u, max v
    private void resolveBounds(RiggedMeshStruct mesh, IntArrayList faces, float[] bounds, int base) {

        bounds[base] = Float.MAX_VALUE;
        bounds[base + 1] = Float.MAX_VALUE;
        bounds[base + 2] = -Float.MAX_VALUE;
        bounds[base + 3] = -Float.MAX_VALUE;

        for (int i = 0; i < faces.size(); i++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                float u = mesh.getFaceU(faces.getInt(i), c);
                float v = mesh.getFaceV(faces.getInt(i), c);

                bounds[base] = Math.min(bounds[base], u);
                bounds[base + 1] = Math.min(bounds[base + 1], v);
                bounds[base + 2] = Math.max(bounds[base + 2], u);
                bounds[base + 3] = Math.max(bounds[base + 3], v);
            }
    }

    // Average Scale \\

    private void averageScale(CharacterMeshDocumentInstance document) {

        ObjectArrayList<IntArrayList> islandFaces = resolveSelectedIslands(document);

        if (islandFaces.isEmpty()) {
            characterEditorManager.setStatusMessage(EditorSetting.UV_MESSAGE_NO_CORNERS);
            return;
        }

        characterHistoryBranch.begin(document);
        averageIslands(document.getMesh(), islandFaces);
        characterMeshBranch.finishEdit(document);
    }

    // Every island scaled about its centre so each gives the same UV area to the same surface area, the UV area they
    // cover together kept
    private void averageIslands(RiggedMeshStruct mesh, ObjectArrayList<IntArrayList> islandFaces) {

        int count = islandFaces.size();
        double[] surfaceAreas = new double[count];
        double[] uvAreas = new double[count];
        double surfaceTotal = 0.0;
        double uvTotal = 0.0;

        for (int i = 0; i < count; i++) {

            IntArrayList faces = islandFaces.get(i);

            for (int f = 0; f < faces.size(); f++) {
                surfaceAreas[i] += resolveSurfaceArea(mesh, faces.getInt(f));
                uvAreas[i] += Math.abs(resolveUVArea(mesh, faces.getInt(f)));
            }

            surfaceTotal += surfaceAreas[i];
            uvTotal += uvAreas[i];
        }

        if (surfaceTotal <= EditorSetting.UV_UNWRAP_EPSILON || uvTotal <= EditorSetting.UV_UNWRAP_EPSILON)
            return;

        float[] bounds = new float[4];

        for (int i = 0; i < count; i++) {

            if (uvAreas[i] <= EditorSetting.UV_UNWRAP_EPSILON)
                continue;

            float scale = (float) Math.sqrt(surfaceAreas[i] * uvTotal / surfaceTotal / uvAreas[i]);

            resolveBounds(mesh, islandFaces.get(i), bounds, 0);
            scaleIsland(mesh, islandFaces.get(i), (bounds[0] + bounds[2]) * 0.5f, (bounds[1] + bounds[3]) * 0.5f,
                    scale);
        }
    }

    private void scaleIsland(RiggedMeshStruct mesh, IntArrayList faces, float centerU, float centerV, float scale) {

        for (int i = 0; i < faces.size(); i++) {

            int faceIndex = faces.getInt(i);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                mesh.setFaceUV(
                        faceIndex,
                        c,
                        centerU + (mesh.getFaceU(faceIndex, c) - centerU) * scale,
                        centerV + (mesh.getFaceV(faceIndex, c) - centerV) * scale);
        }
    }

    // A quad's area on the surface, its two triangles
    private double resolveSurfaceArea(RiggedMeshStruct mesh, int faceIndex) {
        return resolveTriangleArea(mesh, mesh.getFaceVertex(faceIndex, 0), mesh.getFaceVertex(faceIndex, 1),
                mesh.getFaceVertex(faceIndex, 2))
                + resolveTriangleArea(mesh, mesh.getFaceVertex(faceIndex, 0), mesh.getFaceVertex(faceIndex, 2),
                        mesh.getFaceVertex(faceIndex, 3));
    }

    private double resolveTriangleArea(RiggedMeshStruct mesh, int a, int b, int c) {

        double abx = mesh.getX(b) - mesh.getX(a);
        double aby = mesh.getY(b) - mesh.getY(a);
        double abz = mesh.getZ(b) - mesh.getZ(a);
        double acx = mesh.getX(c) - mesh.getX(a);
        double acy = mesh.getY(c) - mesh.getY(a);
        double acz = mesh.getZ(c) - mesh.getZ(a);
        double nx = aby * acz - abz * acy;
        double ny = abz * acx - abx * acz;
        double nz = abx * acy - aby * acx;

        return Math.sqrt(nx * nx + ny * ny + nz * nz) * 0.5;
    }

    // A quad's signed area in UV, positive while its corners wind anticlockwise
    private double resolveUVArea(RiggedMeshStruct mesh, int faceIndex) {

        double area = 0.0;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int next = (c + 1) % EngineSetting.QUAD_VERTEX_COUNT;

            area += mesh.getFaceU(faceIndex, c) * mesh.getFaceV(faceIndex, next)
                    - mesh.getFaceU(faceIndex, next) * mesh.getFaceV(faceIndex, c);
        }

        return area * 0.5;
    }

    // Stitch \\

    // Each island holding selected corners carried onto the neighbours their vertices also stand on, by the turn,
    // scale and move that best lays them there, then those corners welded where the neighbours hold them
    private void stitch(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        collectShownFaces(document, faceScratch);

        int[] islands = new int[faceScratch.size()];
        int islandCount = resolveIslands(mesh, faceScratch, true, islands);
        ObjectArrayList<IntArrayList> islandFaces = groupIslands(faceScratch, islands, islandCount);
        int stitched = 0;

        characterHistoryBranch.begin(document);

        for (int island = 0; island < islandCount; island++)
            if (stitchIsland(document, islandFaces.get(island), islands, island))
                stitched++;

        finish(document, stitched, EditorSetting.UV_MESSAGE_NO_STITCH, EditorSetting.UV_MESSAGE_STITCHED);
    }

    // One island laid onto its neighbours through its selected corners; false when none has a neighbour to meet
    private boolean stitchIsland(
            CharacterMeshDocumentInstance document,
            IntArrayList faces,
            int[] islands,
            int island) {

        RiggedMeshStruct mesh = document.getMesh();
        FloatArrayList pairs = new FloatArrayList();

        cornerScratch.clear();

        for (int i = 0; i < faces.size(); i++) {

            int faceIndex = faces.getInt(i);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                if (!document.isCornerSelected(faceIndex, c))
                    continue;

                int vertexIndex = mesh.getFaceVertex(faceIndex, c);
                float targetU = 0f;
                float targetV = 0f;
                int targets = 0;

                for (int k = 0; k < faceScratch.size(); k++) {

                    if (islands[k] == island || !mesh.usesVertex(faceScratch.getInt(k), vertexIndex))
                        continue;

                    int corner = findCorner(mesh, faceScratch.getInt(k), vertexIndex);
                    targetU += mesh.getFaceU(faceScratch.getInt(k), corner);
                    targetV += mesh.getFaceV(faceScratch.getInt(k), corner);
                    targets++;
                }

                if (targets == 0)
                    continue;

                pairs.add(mesh.getFaceU(faceIndex, c));
                pairs.add(mesh.getFaceV(faceIndex, c));
                pairs.add(targetU / targets);
                pairs.add(targetV / targets);
                cornerScratch.add(faceIndex * EngineSetting.QUAD_VERTEX_COUNT + c);
            }
        }

        if (cornerScratch.isEmpty())
            return false;

        layOnto(mesh, faces, pairs);

        for (int i = 0; i < cornerScratch.size(); i++) {

            int corner = cornerScratch.getInt(i);

            mesh.setFaceUV(
                    corner / EngineSetting.QUAD_VERTEX_COUNT,
                    corner % EngineSetting.QUAD_VERTEX_COUNT,
                    pairs.getFloat(i * 4 + 2),
                    pairs.getFloat(i * 4 + 3));
        }

        return true;
    }

    // The island carried by the similarity (turn, uniform scale and move) laying the pairs' sources nearest their
    // targets in the least squares sense; a single pair only moves it
    private void layOnto(RiggedMeshStruct mesh, IntArrayList faces, FloatArrayList pairs) {

        int count = pairs.size() / 4;
        double sourceU = 0.0;
        double sourceV = 0.0;
        double targetU = 0.0;
        double targetV = 0.0;

        for (int i = 0; i < count; i++) {
            sourceU += pairs.getFloat(i * 4);
            sourceV += pairs.getFloat(i * 4 + 1);
            targetU += pairs.getFloat(i * 4 + 2);
            targetV += pairs.getFloat(i * 4 + 3);
        }

        sourceU /= count;
        sourceV /= count;
        targetU /= count;
        targetV /= count;

        double real = 0.0;
        double imaginary = 0.0;
        double norm = 0.0;

        for (int i = 0; i < count; i++) {

            double su = pairs.getFloat(i * 4) - sourceU;
            double sv = pairs.getFloat(i * 4 + 1) - sourceV;
            double tu = pairs.getFloat(i * 4 + 2) - targetU;
            double tv = pairs.getFloat(i * 4 + 3) - targetV;

            real += su * tu + sv * tv;
            imaginary += su * tv - sv * tu;
            norm += su * su + sv * sv;
        }

        double a = norm > EditorSetting.UV_UNWRAP_EPSILON ? real / norm : 1.0;
        double b = norm > EditorSetting.UV_UNWRAP_EPSILON ? imaginary / norm : 0.0;

        for (int i = 0; i < faces.size(); i++) {

            int faceIndex = faces.getInt(i);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                double u = mesh.getFaceU(faceIndex, c) - sourceU;
                double v = mesh.getFaceV(faceIndex, c) - sourceV;

                mesh.setFaceUV(faceIndex, c, (float) (a * u - b * v + targetU), (float) (b * u + a * v + targetV));
            }
        }
    }

    // Pins and Flips \\

    private void pin(CharacterMeshDocumentInstance document, boolean pinned) {

        RiggedMeshStruct mesh = document.getMesh();

        collectSelectedCorners(document, cornerScratch);
        characterHistoryBranch.begin(document);

        for (int i = 0; i < cornerScratch.size(); i++) {

            int corner = cornerScratch.getInt(i);
            mesh.setCornerPinned(
                    corner / EngineSetting.QUAD_VERTEX_COUNT, corner % EngineSetting.QUAD_VERTEX_COUNT, pinned);
        }

        finish(document, cornerScratch.size(), EditorSetting.UV_MESSAGE_NO_CORNERS,
                pinned ? EditorSetting.UV_MESSAGE_PINNED : EditorSetting.UV_MESSAGE_UNPINNED);
    }

    // The selected corners mirrored across their centre along u (axis 0) or v (axis 1)
    private void flip(CharacterMeshDocumentInstance document, int axis) {

        RiggedMeshStruct mesh = document.getMesh();

        if (!resolveCenter(document)) {
            characterEditorManager.setStatusMessage(EditorSetting.UV_MESSAGE_NO_CORNERS);
            return;
        }

        characterHistoryBranch.begin(document);

        for (int i = 0; i < cornerScratch.size(); i++) {

            int corner = cornerScratch.getInt(i);
            float u = getU(mesh, corner);
            float v = getV(mesh, corner);

            setUV(mesh, corner, axis == 0 ? 2f * pivotU - u : u, axis == 1 ? 2f * pivotV - v : v);
        }

        characterMeshBranch.finishEdit(document);
    }

    // Selection \\

    // Every selected corner of the faces shown, each as its face times four plus its corner
    void collectSelectedCorners(CharacterMeshDocumentInstance document, IntArrayList corners) {

        collectShownFaces(document, faceScratch);
        corners.clear();

        for (int i = 0; i < faceScratch.size(); i++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                if (document.isCornerSelected(faceScratch.getInt(i), c))
                    corners.add(faceScratch.getInt(i) * EngineSetting.QUAD_VERTEX_COUNT + c);
    }

    // A corner picked, with every corner of the same vertex at the same place, combined with the selection by mode
    void pick(CharacterMeshDocumentInstance document, int faceIndex, int corner, CharacterEditorSelection mode) {

        RiggedMeshStruct mesh = document.getMesh();
        int vertexIndex = faceIndex != EngineSetting.INDEX_NOT_FOUND
                ? mesh.getFaceVertex(faceIndex, corner)
                : EngineSetting.INDEX_NOT_FOUND;

        collectShownFaces(document, faceScratch);

        if (mode == CharacterEditorSelection.REPLACE)
            document.clearCornerSelection();

        if (vertexIndex != EngineSetting.INDEX_NOT_FOUND)
            for (int i = 0; i < faceScratch.size(); i++) {

                int shown = faceScratch.getInt(i);

                if (!mesh.usesVertex(shown, vertexIndex))
                    continue;

                int shownCorner = findCorner(mesh, shown, vertexIndex);

                if (sameUV(mesh, shown, shownCorner, faceIndex, corner))
                    apply(document, shown, shownCorner, mode);
            }

        selectionChanged(document);
    }

    // Every corner of the faces shown inside a box in UV, combined with the selection by mode
    void box(
            CharacterMeshDocumentInstance document,
            float minU,
            float minV,
            float maxU,
            float maxV,
            CharacterEditorSelection mode) {

        RiggedMeshStruct mesh = document.getMesh();

        collectShownFaces(document, faceScratch);

        if (mode == CharacterEditorSelection.REPLACE)
            document.clearCornerSelection();

        for (int i = 0; i < faceScratch.size(); i++) {

            int faceIndex = faceScratch.getInt(i);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                float u = mesh.getFaceU(faceIndex, c);
                float v = mesh.getFaceV(faceIndex, c);

                if (u >= minU && u <= maxU && v >= minV && v <= maxV)
                    apply(document, faceIndex, c, mode);
            }
        }

        selectionChanged(document);
    }

    private void apply(
            CharacterMeshDocumentInstance document,
            int faceIndex,
            int corner,
            CharacterEditorSelection mode) {

        document.setCornerSelected(faceIndex, corner, switch (mode) {
            case REPLACE, ADD -> true;
            case SUBTRACT -> false;
            case TOGGLE -> !document.isCornerSelected(faceIndex, corner);
        });
    }

    // Every corner shown selected when none is, none otherwise
    private void toggleAll(CharacterMeshDocumentInstance document) {

        collectSelectedCorners(document, cornerScratch);

        boolean select = cornerScratch.isEmpty();

        document.clearCornerSelection();

        if (select)
            for (int i = 0; i < faceScratch.size(); i++)
                for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                    document.setCornerSelected(faceScratch.getInt(i), c, true);

        selectionChanged(document);
    }

    // Every corner of each UV island holding a selected corner
    private void selectLinked(CharacterMeshDocumentInstance document) {

        ObjectArrayList<IntArrayList> islandFaces = resolveSelectedIslands(document);

        for (int i = 0; i < islandFaces.size(); i++) {

            IntArrayList faces = islandFaces.get(i);

            for (int f = 0; f < faces.size(); f++)
                for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                    document.setCornerSelected(faces.getInt(f), c, true);
        }

        selectionChanged(document);
    }

    private void selectionChanged(CharacterMeshDocumentInstance document) {
        document.markSelectionChanged();
        characterEditorManager.notifyChanged();
    }

    // Transform \\

    // The selected corners taken by a drag from a point in UV, their centre the pivot; false with none selected
    boolean beginTransform(CharacterMeshDocumentInstance document, EditorTool tool, float u, float v) {

        RiggedMeshStruct mesh = document.getMesh();

        collectSelectedCorners(document, transformCorners);

        if (transformCorners.isEmpty())
            return false;

        transformStart.clear();
        this.transformPivotU = 0f;
        this.transformPivotV = 0f;

        for (int i = 0; i < transformCorners.size(); i++) {

            float cornerU = getU(mesh, transformCorners.getInt(i));
            float cornerV = getV(mesh, transformCorners.getInt(i));

            transformStart.add(cornerU);
            transformStart.add(cornerV);
            transformPivotU += cornerU;
            transformPivotV += cornerV;
        }

        this.transformPivotU /= transformCorners.size();
        this.transformPivotV /= transformCorners.size();
        this.grabU = u;
        this.grabV = v;
        this.transformTool = tool;
        this.transformDocument = document;
        characterHistoryBranch.begin(document);

        return true;
    }

    // The drag carried to a point in UV: moved by the pointer's travel, turned by its angle about the pivot, or
    // scaled by its distance from it, from where every corner started
    void updateTransform(float u, float v) {

        if (!isTransforming())
            return;

        RiggedMeshStruct mesh = transformDocument.getMesh();
        double startAngle = Math.atan2(grabV - transformPivotV, grabU - transformPivotU);
        double angle = Math.atan2(v - transformPivotV, u - transformPivotU) - startAngle;
        double startDistance = Math.hypot(grabU - transformPivotU, grabV - transformPivotV);
        float scale = startDistance > EditorSetting.UV_UNWRAP_EPSILON
                ? (float) (Math.hypot(u - transformPivotU, v - transformPivotV) / startDistance)
                : 1f;
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        for (int i = 0; i < transformCorners.size(); i++) {

            float startU = transformStart.getFloat(i * 2);
            float startV = transformStart.getFloat(i * 2 + 1);
            float du = startU - transformPivotU;
            float dv = startV - transformPivotV;

            switch (transformTool) {
                case ROTATE -> setUV(mesh, transformCorners.getInt(i), transformPivotU + du * cos - dv * sin,
                        transformPivotV + du * sin + dv * cos);
                case SCALE -> setUV(mesh, transformCorners.getInt(i), transformPivotU + du * scale,
                        transformPivotV + dv * scale);
                default -> setUV(mesh, transformCorners.getInt(i), startU + u - grabU, startV + v - grabV);
            }
        }

        characterMeshBranch.preview(transformDocument);
    }

    void endTransform() {

        if (!isTransforming())
            return;

        CharacterMeshDocumentInstance document = transformDocument;

        this.transformDocument = null;
        characterMeshBranch.finishEdit(document);
    }

    void cancelTransform() {

        if (!isTransforming())
            return;

        CharacterMeshDocumentInstance document = transformDocument;

        this.transformDocument = null;
        characterHistoryBranch.cancel(document);
        characterMeshBranch.upload(document);
        characterEditorManager.notifyChanged();
    }

    boolean isTransforming() {
        return transformDocument != null;
    }

    // Utility \\

    // An edit's step committed when it changed something, else closed and the reason given
    private void finish(CharacterMeshDocumentInstance document, int changed, String nothing, String done) {

        if (changed == 0) {
            characterHistoryBranch.cancel(document);
            characterEditorManager.setStatusMessage(nothing);
            return;
        }

        if (characterMeshBranch.finishEdit(document))
            characterEditorManager.setStatusMessage(done + changed);
    }

    private float getU(RiggedMeshStruct mesh, int corner) {
        return mesh.getFaceU(corner / EngineSetting.QUAD_VERTEX_COUNT, corner % EngineSetting.QUAD_VERTEX_COUNT);
    }

    private float getV(RiggedMeshStruct mesh, int corner) {
        return mesh.getFaceV(corner / EngineSetting.QUAD_VERTEX_COUNT, corner % EngineSetting.QUAD_VERTEX_COUNT);
    }

    private void setUV(RiggedMeshStruct mesh, int corner, float u, float v) {
        mesh.setFaceUV(corner / EngineSetting.QUAD_VERTEX_COUNT, corner % EngineSetting.QUAD_VERTEX_COUNT, u, v);
    }
}
