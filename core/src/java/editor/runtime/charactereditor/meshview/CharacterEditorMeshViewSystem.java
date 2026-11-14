package editor.runtime.charactereditor.meshview;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.brushpipeline.util.BrushUtility;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.charactereditor.mode.CharacterEditorModeSystem;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import engine.assets.camera.CameraInstance;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterEditorMeshViewSystem extends SystemPackage {

    /*
     * Places the character's mesh parts where this tab's viewport shows them
     * while a mesh mode is open: every vertex of every worn part skinned on the
     * CPU by the pose the preview strikes, exactly as the shader skins it. From
     * those positions it projects vertices onto the window, finds the active
     * part's vertex nearest the pointer, the ones inside a box or under the
     * brush preset's tip at the strength it has there, and — unless X-Ray is on
     * — leaves out any a worn face hides from the camera. Only the vertices of
     * the tab's region are picked, and a brush reaches each by the share it
     * belongs to the region; a tab whose region's part is not worn edits
     * nothing. Positions are
     * refreshed each frame and again on demand after an edit; their storage
     * only ever grows. Occlusion is answered from the frame's own snapshot:
     * the first test of a frame bins every worn face into the window cells
     * its projection covers, so a vertex is tested only against the faces
     * over its own cell, and each vertex's answer is kept for the rest of
     * the frame, however many brush stamps ask again.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private BrushManager brushManager;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private CharacterEditorModeSystem characterEditorModeSystem;

    // Parts
    private ObjectArrayList<CharacterMeshDocumentInstance> wornDocuments;
    private ObjectArrayList<FloatArrayList> wornPositions;
    private CharacterMeshDocumentInstance activeDocument;
    private FloatArrayList activePositions;

    // Occlusion — each worn vertex in window pixels, the faces over each cell packed one run per cell
    private boolean occlusionBuilt;
    private ObjectArrayList<FloatArrayList> wornScreens;
    private int cellColumns;
    private int cellRows;
    private IntArrayList cellStarts;
    private IntArrayList cellFill;
    private IntArrayList cellDocuments;
    private IntArrayList cellFaces;
    private IntArrayList unbinnedDocuments;
    private IntArrayList unbinnedFaces;
    private ByteArrayList visibility;
    private int faceMinColumn;
    private int faceMaxColumn;
    private int faceMinRow;
    private int faceMaxRow;

    // Hit — how far along the last ray met a triangle, and where on it from its first corner
    private float hitDistance;
    private float hitU;
    private float hitV;

    // Scratch
    private Vector2 screenScratch;
    private Vector2 occlusionScratch;
    private Vector3 rayOrigin;
    private Vector3 rayDirection;

    // Base \\

    @Override
    protected void create() {

        // Parts
        this.wornDocuments = new ObjectArrayList<>();
        this.wornPositions = new ObjectArrayList<>();

        // Occlusion
        this.wornScreens = new ObjectArrayList<>();
        this.cellStarts = new IntArrayList();
        this.cellFill = new IntArrayList();
        this.cellDocuments = new IntArrayList();
        this.cellFaces = new IntArrayList();
        this.unbinnedDocuments = new IntArrayList();
        this.unbinnedFaces = new IntArrayList();
        this.visibility = new ByteArrayList();

        // Scratch
        this.screenScratch = new Vector2();
        this.occlusionScratch = new Vector2();
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.brushManager = get(BrushManager.class);
        this.characterViewPoseSystem = get(CharacterViewPoseSystem.class);
        this.characterEditorModeSystem = get(CharacterEditorModeSystem.class);
    }

    // Update \\

    @Override
    protected void update() {
        refresh();
    }

    // Skins every worn part's vertices by the pose struck this frame
    public void refresh() {

        this.activeDocument = null;
        this.activePositions = null;
        this.occlusionBuilt = false;
        wornDocuments.clear();
        visibility.clear();

        if (!characterEditorManager.getMode().isMeshMode() || !characterViewPoseSystem.hasSubject())
            return;

        characterEditorManager.collectWornMeshDocuments(wornDocuments);
        boolean regionOpen = characterEditorManager.isRegionOpen(characterEditorModeSystem.getRegion());
        CharacterMeshDocumentInstance active = regionOpen ? characterEditorManager.getActiveMeshDocument() : null;

        while (wornPositions.size() < wornDocuments.size())
            wornPositions.add(new FloatArrayList());

        for (int i = 0; i < wornDocuments.size(); i++) {

            skin(wornDocuments.get(i).getMesh(), wornPositions.get(i));

            if (wornDocuments.get(i) == active) {
                this.activeDocument = active;
                this.activePositions = wornPositions.get(i);
            }
        }

        if (activePositions != null)
            visibility.size(getVertexCount());
    }

    // Each vertex moved by its bones' skinning matrices in proportion to their weights
    private void skin(RiggedMeshStruct mesh, FloatArrayList positions) {

        Matrix4[] skinningMatrices = characterViewPoseSystem.getAnimationStateHandle().getSkinningMatrices();
        int vertexCount = mesh.getVertexCount();

        positions.clear();
        positions.ensureCapacity(vertexCount * EngineSetting.RIGGED_MESH_POSITION_FLOATS);

        for (int v = 0; v < vertexCount; v++) {

            float x = mesh.getX(v);
            float y = mesh.getY(v);
            float z = mesh.getZ(v);
            int influenceCount = mesh.getInfluenceCount(v);

            if (influenceCount == 0) {
                positions.add(x);
                positions.add(y);
                positions.add(z);
                continue;
            }

            float sx = 0f;
            float sy = 0f;
            float sz = 0f;

            for (int i = 0; i < influenceCount; i++) {

                float[] m = skinningMatrices[mesh.getInfluenceBone(v, i)].val;
                float w = mesh.getInfluenceWeight(v, i);

                sx += (m[0] * x + m[4] * y + m[8] * z + m[12]) * w;
                sy += (m[1] * x + m[5] * y + m[9] * z + m[13]) * w;
                sz += (m[2] * x + m[6] * y + m[10] * z + m[14]) * w;
            }

            positions.add(sx);
            positions.add(sy);
            positions.add(sz);
        }
    }

    // Screen \\

    // A world point in window pixels, the origin bottom left as the pointer reads; false behind the camera
    public boolean project(float x, float y, float z, Vector2 out) {
        return CharacterPoseUtility.project(context.getWindow(), x, y, z, out);
    }

    // The ray from the camera through a window pixel
    public void resolveRay(float pixelX, float pixelY, Vector3 origin, Vector3 direction) {
        CharacterPoseUtility.resolveRay(context.getWindow(), pixelX, pixelY, origin, direction);
    }

    // Picking \\

    // The active part's visible vertex nearest a pixel within the pick radius, or INDEX_NOT_FOUND
    public int pickVertex(float pixelX, float pixelY) {

        if (activeDocument == null)
            return EngineSetting.INDEX_NOT_FOUND;

        float radius = EditorSetting.CHARACTER_VIEW_PICK_RADIUS_PIXELS;
        float nearest = radius * radius;
        int found = EngineSetting.INDEX_NOT_FOUND;

        for (int v = 0; v < getVertexCount(); v++) {

            if (!project(getX(v), getY(v), getZ(v), screenScratch))
                continue;

            float dx = screenScratch.x - pixelX;
            float dy = screenScratch.y - pixelY;
            float distance = dx * dx + dy * dy;

            if (distance <= nearest && isInRegion(v) && isVisible(v)) {
                nearest = distance;
                found = v;
            }
        }

        return found;
    }

    // Every visible vertex of the active part inside a window rectangle given by two opposite corners
    public void collectBox(float x0, float y0, float x1, float y1, IntArrayList vertices) {

        vertices.clear();

        if (activeDocument == null)
            return;

        float minX = Math.min(x0, x1);
        float maxX = Math.max(x0, x1);
        float minY = Math.min(y0, y1);
        float maxY = Math.max(y0, y1);

        for (int v = 0; v < getVertexCount(); v++) {

            if (!project(getX(v), getY(v), getZ(v), screenScratch))
                continue;

            if (screenScratch.x >= minX && screenScratch.x <= maxX && screenScratch.y >= minY
                    && screenScratch.y <= maxY && isInRegion(v) && isVisible(v))
                vertices.add(v);
        }
    }

    // Every visible vertex of the active part under the preset's tip, each with the strength the tip has there
    public void collectBrush(
            float pixelX,
            float pixelY,
            float radius,
            float strength,
            IntArrayList vertices,
            FloatArrayList strengths) {

        vertices.clear();
        strengths.clear();

        if (activeDocument == null)
            return;

        for (int v = 0; v < getVertexCount(); v++) {

            if (!project(getX(v), getY(v), getZ(v), screenScratch))
                continue;

            float falloff = BrushUtility.resolveCoverage(
                    brushManager.getActiveBrush(), screenScratch.x - pixelX, screenScratch.y - pixelY, radius)
                    * characterEditorManager.getRegionShare(activeDocument, characterEditorModeSystem.getRegion(), v);

            if (falloff > 0f && characterEditorManager.isOnRegionSide(activeDocument, v) && isVisible(v)) {
                vertices.add(v);
                strengths.add(falloff * strength);
            }
        }
    }

    // The active part's face whose surface the ray through a window pixel meets first, its UV there written out;
    // INDEX_NOT_FOUND where the ray meets none of the region's faces
    public int pickFace(float pixelX, float pixelY, Vector2 uvOut) {

        if (activeDocument == null)
            return EngineSetting.INDEX_NOT_FOUND;

        RiggedMeshStruct mesh = activeDocument.getMesh();
        int picked = EngineSetting.INDEX_NOT_FOUND;
        float nearest = Float.POSITIVE_INFINITY;

        resolveRay(pixelX, pixelY, rayOrigin, rayDirection);

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            if (!isInRegion(mesh.getFaceVertex(f, 0)))
                continue;

            for (int t = 0; t < 2; t++) {

                int b = t == 0 ? 1 : 2;
                int c = t == 0 ? 2 : 3;

                if (!hitsTriangle(activePositions, mesh.getFaceVertex(f, 0), mesh.getFaceVertex(f, b),
                        mesh.getFaceVertex(f, c), nearest))
                    continue;

                nearest = hitDistance;
                picked = f;
                uvOut.set(
                        resolveHitUV(mesh, f, b, c, true),
                        resolveHitUV(mesh, f, b, c, false));
            }
        }

        return picked;
    }

    // A face's UV where the last ray met its triangle from the first corner through two others
    private float resolveHitUV(RiggedMeshStruct mesh, int faceIndex, int b, int c, boolean u) {

        float a0 = u ? mesh.getFaceU(faceIndex, 0) : mesh.getFaceV(faceIndex, 0);
        float b0 = u ? mesh.getFaceU(faceIndex, b) : mesh.getFaceV(faceIndex, b);
        float c0 = u ? mesh.getFaceU(faceIndex, c) : mesh.getFaceV(faceIndex, c);

        return a0 + (b0 - a0) * hitU + (c0 - a0) * hitV;
    }

    // Whether a point on the active part shows to the camera, always with X-Ray on
    public boolean isPointShown(float x, float y, float z) {
        return characterEditorManager.isXray() || !isOccluded(x, y, z);
    }

    // Region \\

    // Whether a vertex of the active part is one the tab's region picks and selects, on the side it shows
    public boolean isInRegion(int vertexIndex) {
        return characterEditorManager.isInRegion(activeDocument, characterEditorModeSystem.getRegion(), vertexIndex)
                && characterEditorManager.isOnRegionSide(activeDocument, vertexIndex);
    }

    // Whether a worn face is drawn in the tab, so it hides what lies behind it: every face for the whole character, a
    // face of the active part with a corner the region shows, and the faces of the parts worn with a region that
    // holds a feature beside the body
    private boolean isFaceShown(int documentIndex, int faceIndex) {

        CharacterEditorRegion region = characterEditorModeSystem.getRegion();

        if (region.isWhole())
            return true;

        CharacterMeshDocumentInstance document = wornDocuments.get(documentIndex);

        if (document != activeDocument)
            return region.holdsFeature();

        RiggedMeshStruct mesh = document.getMesh();

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (isInRegion(mesh.getFaceVertex(faceIndex, c)))
                return true;

        return false;
    }

    // Occlusion \\

    // Whether a vertex of the active part shows, tested once a frame and kept for every later stamp
    private boolean isVisible(int vertexIndex) {

        if (characterEditorManager.isXray())
            return true;

        byte state = visibility.getByte(vertexIndex);

        if (state != CharacterEditorSetting.VISIBILITY_UNKNOWN)
            return state == CharacterEditorSetting.VISIBILITY_SHOWN;

        boolean shown = !isOccluded(getX(vertexIndex), getY(vertexIndex), getZ(vertexIndex));

        visibility.set(vertexIndex, shown
                ? CharacterEditorSetting.VISIBILITY_SHOWN
                : CharacterEditorSetting.VISIBILITY_HIDDEN);
        return shown;
    }

    // Whether any worn face stands between the camera and a point, short of the faces meeting at the point itself;
    // only the faces over the point's cell are tested, every face for a point outside the window
    private boolean isOccluded(float x, float y, float z) {

        CameraInstance camera = context.getWindow().getActiveCamera();
        Vector3 eye = camera.getPosition();
        float dx = x - eye.x;
        float dy = y - eye.y;
        float dz = z - eye.z;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance <= 0f)
            return false;

        rayOrigin.set(eye);
        rayDirection.set(dx / distance, dy / distance, dz / distance);

        buildOcclusion();

        float reach = distance - CharacterEditorSetting.OCCLUSION_BIAS;
        int cell = project(x, y, z, occlusionScratch)
                ? toCell(occlusionScratch.x, occlusionScratch.y)
                : EngineSetting.INDEX_NOT_FOUND;

        if (cell == EngineSetting.INDEX_NOT_FOUND)
            return hitsAnyMesh(reach);

        for (int i = cellStarts.getInt(cell); i < cellStarts.getInt(cell + 1); i++)
            if (hitsFace(cellDocuments.getInt(i), cellFaces.getInt(i), reach))
                return true;

        for (int i = 0; i < unbinnedFaces.size(); i++)
            if (hitsFace(unbinnedDocuments.getInt(i), unbinnedFaces.getInt(i), reach))
                return true;

        return false;
    }

    private boolean hitsAnyMesh(float reach) {

        for (int d = 0; d < wornDocuments.size(); d++)
            for (int f = 0; f < wornDocuments.get(d).getMesh().getFaceCount(); f++)
                if (hitsFace(d, f, reach))
                    return true;

        return false;
    }

    private boolean hitsFace(int documentIndex, int faceIndex, float reach) {

        RiggedMeshStruct mesh = wornDocuments.get(documentIndex).getMesh();
        FloatArrayList positions = wornPositions.get(documentIndex);
        int c0 = mesh.getFaceVertex(faceIndex, 0);
        int c1 = mesh.getFaceVertex(faceIndex, 1);
        int c2 = mesh.getFaceVertex(faceIndex, 2);
        int c3 = mesh.getFaceVertex(faceIndex, 3);

        return hitsTriangle(positions, c0, c1, c2, reach) || hitsTriangle(positions, c2, c3, c0, reach);
    }

    // Every worn face binned into the cells its projection's bounds cover, counted first then placed; a face with a
    // corner behind the camera is kept apart and tested for every point
    private void buildOcclusion() {

        if (occlusionBuilt)
            return;

        this.occlusionBuilt = true;

        int cellPixels = CharacterEditorSetting.OCCLUSION_CELL_PIXELS;

        this.cellColumns = Math.max(1, (context.getWindow().getWidth() + cellPixels - 1) / cellPixels);
        this.cellRows = Math.max(1, (context.getWindow().getHeight() + cellPixels - 1) / cellPixels);

        int cellCount = cellColumns * cellRows;

        fill(cellStarts, cellCount + 1);
        fill(cellFill, cellCount);
        unbinnedDocuments.clear();
        unbinnedFaces.clear();
        projectWorn();

        for (int d = 0; d < wornDocuments.size(); d++) {

            RiggedMeshStruct mesh = wornDocuments.get(d).getMesh();

            for (int f = 0; f < mesh.getFaceCount(); f++) {

                if (!isFaceShown(d, f))
                    continue;

                if (!resolveFaceCells(mesh, wornScreens.get(d), f)) {
                    unbinnedDocuments.add(d);
                    unbinnedFaces.add(f);
                    continue;
                }

                for (int row = faceMinRow; row <= faceMaxRow; row++)
                    for (int column = faceMinColumn; column <= faceMaxColumn; column++) {

                        int slot = row * cellColumns + column + 1;

                        cellStarts.set(slot, cellStarts.getInt(slot) + 1);
                    }
            }
        }

        for (int i = 1; i < cellStarts.size(); i++)
            cellStarts.set(i, cellStarts.getInt(i) + cellStarts.getInt(i - 1));

        fill(cellDocuments, cellStarts.getInt(cellCount));
        fill(cellFaces, cellStarts.getInt(cellCount));

        for (int d = 0; d < wornDocuments.size(); d++) {

            RiggedMeshStruct mesh = wornDocuments.get(d).getMesh();

            for (int f = 0; f < mesh.getFaceCount(); f++) {

                if (!isFaceShown(d, f) || !resolveFaceCells(mesh, wornScreens.get(d), f))
                    continue;

                for (int row = faceMinRow; row <= faceMaxRow; row++)
                    for (int column = faceMinColumn; column <= faceMaxColumn; column++)
                        placeFace(row * cellColumns + column, d, f);
            }
        }
    }

    // Every worn vertex in window pixels, NaN for one behind the camera
    private void projectWorn() {

        while (wornScreens.size() < wornDocuments.size())
            wornScreens.add(new FloatArrayList());

        for (int d = 0; d < wornDocuments.size(); d++) {

            FloatArrayList positions = wornPositions.get(d);
            FloatArrayList screens = wornScreens.get(d);
            int vertexCount = positions.size() / EngineSetting.RIGGED_MESH_POSITION_FLOATS;

            screens.clear();
            screens.ensureCapacity(vertexCount * 2);

            for (int v = 0; v < vertexCount; v++) {

                int base = v * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
                boolean shown = project(
                        positions.getFloat(base),
                        positions.getFloat(base + 1),
                        positions.getFloat(base + 2),
                        occlusionScratch);

                screens.add(shown ? occlusionScratch.x : Float.NaN);
                screens.add(shown ? occlusionScratch.y : Float.NaN);
            }
        }
    }

    // The cells a face's projected bounds cover, held to the window, empty when it lies wholly outside; false when
    // a corner stands behind the camera
    private boolean resolveFaceCells(RiggedMeshStruct mesh, FloatArrayList screens, int faceIndex) {

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int vertexIndex = mesh.getFaceVertex(faceIndex, c);
            float x = screens.getFloat(vertexIndex * 2);
            float y = screens.getFloat(vertexIndex * 2 + 1);

            if (Float.isNaN(x))
                return false;

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        int cellPixels = CharacterEditorSetting.OCCLUSION_CELL_PIXELS;

        this.faceMinColumn = Math.max(0, (int) Math.floor(minX / cellPixels));
        this.faceMinRow = Math.max(0, (int) Math.floor(minY / cellPixels));
        this.faceMaxColumn = Math.min(cellColumns - 1, (int) Math.floor(maxX / cellPixels));
        this.faceMaxRow = Math.min(cellRows - 1, (int) Math.floor(maxY / cellPixels));

        return true;
    }

    private void placeFace(int cell, int documentIndex, int faceIndex) {

        int slot = cellStarts.getInt(cell) + cellFill.getInt(cell);

        cellDocuments.set(slot, documentIndex);
        cellFaces.set(slot, faceIndex);
        cellFill.set(cell, cellFill.getInt(cell) + 1);
    }

    // The cell a window pixel lies in, or INDEX_NOT_FOUND outside the window
    private int toCell(float pixelX, float pixelY) {

        int column = (int) Math.floor(pixelX / CharacterEditorSetting.OCCLUSION_CELL_PIXELS);
        int row = (int) Math.floor(pixelY / CharacterEditorSetting.OCCLUSION_CELL_PIXELS);

        if (column < 0 || row < 0 || column >= cellColumns || row >= cellRows)
            return EngineSetting.INDEX_NOT_FOUND;

        return row * cellColumns + column;
    }

    private void fill(IntArrayList list, int size) {
        list.clear();
        list.size(size);
    }

    // Moller-Trumbore: whether the ray meets a triangle closer than the reach
    private boolean hitsTriangle(FloatArrayList positions, int a, int b, int c, float reach) {

        float[] p = positions.elements();
        int ia = a * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
        int ib = b * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
        int ic = c * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
        float e1x = p[ib] - p[ia];
        float e1y = p[ib + 1] - p[ia + 1];
        float e1z = p[ib + 2] - p[ia + 2];
        float e2x = p[ic] - p[ia];
        float e2y = p[ic + 1] - p[ia + 1];
        float e2z = p[ic + 2] - p[ia + 2];
        float px = rayDirection.y * e2z - rayDirection.z * e2y;
        float py = rayDirection.z * e2x - rayDirection.x * e2z;
        float pz = rayDirection.x * e2y - rayDirection.y * e2x;
        float determinant = e1x * px + e1y * py + e1z * pz;

        if (Math.abs(determinant) < CharacterEditorSetting.OCCLUSION_EPSILON)
            return false;

        float inverse = 1f / determinant;
        float tx = rayOrigin.x - p[ia];
        float ty = rayOrigin.y - p[ia + 1];
        float tz = rayOrigin.z - p[ia + 2];
        float u = (tx * px + ty * py + tz * pz) * inverse;

        if (u < 0f || u > 1f)
            return false;

        float qx = ty * e1z - tz * e1y;
        float qy = tz * e1x - tx * e1z;
        float qz = tx * e1y - ty * e1x;
        float v = (rayDirection.x * qx + rayDirection.y * qy + rayDirection.z * qz) * inverse;

        if (v < 0f || u + v > 1f)
            return false;

        float t = (e2x * qx + e2y * qy + e2z * qz) * inverse;

        this.hitDistance = t;
        this.hitU = u;
        this.hitV = v;

        return t > CharacterEditorSetting.OCCLUSION_EPSILON && t < reach;
    }

    // Accessible \\

    public boolean hasActiveDocument() {
        return activeDocument != null;
    }

    public CharacterMeshDocumentInstance getActiveDocument() {
        return activeDocument;
    }

    public int getVertexCount() {
        return activePositions.size() / EngineSetting.RIGGED_MESH_POSITION_FLOATS;
    }

    public float getX(int vertexIndex) {
        return activePositions.getFloat(vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS);
    }

    public float getY(int vertexIndex) {
        return activePositions.getFloat(vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS + 1);
    }

    public float getZ(int vertexIndex) {
        return activePositions.getFloat(vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS + 2);
    }
}
