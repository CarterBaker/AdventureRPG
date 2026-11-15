package editor.runtime.charactereditor.paint;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.brushpipeline.util.BrushUtility;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPaintStampStruct;
import editor.runtime.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.viewcamera.ViewCameraSystem;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class CharacterEditorPaintSystem extends SystemPackage {

    /*
     * Turns this tab's pointer into paint while Paint mode is open. A tool
     * that strokes the brush — Brush, Erase, Smooth — begins only where the
     * brush covers the part and lays a stamp every spacing of the brush
     * along the drag; each stamp hands the painter every face of the tab's
     * region the brush may cover that the camera sees, its corners where the
     * window shows them, so the brush is laid over exactly the texels the
     * screen shows under it. Fill and Pick work on the face clicked, at its
     * UV there. With Mirror on, every face a stamp covers is laid again on
     * its twin across the body (the face whose corners are its corners'
     * twins), the twin's corners placed where their twins show, so the same
     * coverage lands on the twin's texels, as symmetry painting in object
     * space does; the stroke stays one undo step. A press beside the part
     * is left to the view camera. The part's vertices are placed on the
     * window once a frame, whatever the number of stamps.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ViewCameraSystem viewCameraSystem;
    private CharacterEditorManager characterEditorManager;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private BrushManager brushManager;

    // Stroke
    private boolean stroking;
    private float strokeX;
    private float strokeY;
    private CharacterPaintStampStruct stamp;

    // Mirror — each vertex's twin while the stroke mirrors, null otherwise, and each face's twin face
    private IntArrayList mirrorVertices;
    private IntArrayList face2Mirror;
    private Long2IntOpenHashMap corners2Face;
    private int[] cornerScratch;

    // Screen — the active part's vertices in window pixels this frame, NaN behind the camera
    private FloatArrayList screens;
    private boolean projected;

    // Scratch
    private Vector2 screenScratch;
    private Vector2 uvScratch;

    // Base \\

    @Override
    protected void create() {

        // Stroke
        this.stamp = new CharacterPaintStampStruct();

        // Mirror
        this.face2Mirror = new IntArrayList();
        this.corners2Face = new Long2IntOpenHashMap();
        this.corners2Face.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.cornerScratch = new int[EngineSetting.QUAD_VERTEX_COUNT];

        // Screen
        this.screens = new FloatArrayList();

        // Scratch
        this.screenScratch = new Vector2();
        this.uvScratch = new Vector2();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditorMeshViewSystem = get(CharacterEditorMeshViewSystem.class);
        this.brushManager = get(BrushManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        this.projected = false;

        if (!characterEditorManager.getMode().paintsMaps() || !characterEditorMeshViewSystem.hasActiveDocument()) {
            abandon();
            return;
        }

        if (stroking) {
            updateStroke(rawInput);
            return;
        }

        if (!editorInputSystem.isPointerActive() || viewCameraSystem.isDragging())
            return;

        if (editorInputSystem.isClicked(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT))
            press(rawInput.getMouseX(), rawInput.getMouseY());
    }

    // A stroke still held when the mode closes or loses its part keeps what it painted
    private void abandon() {

        if (stroking)
            characterEditorManager.endPaint();

        this.stroking = false;
    }

    // Press \\

    // A stroking tool begins where the brush covers the part; Fill and Pick act on the face clicked
    private void press(float mouseX, float mouseY) {

        if (characterEditorManager.isBrushTool()) {
            beginStroke(mouseX, mouseY);
            return;
        }

        int faceIndex = characterEditorMeshViewSystem.pickFace(mouseX, mouseY, uvScratch);

        if (faceIndex == EngineSetting.INDEX_NOT_FOUND)
            return;

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
        characterEditorManager.clickPaint(faceIndex, uvScratch.x, uvScratch.y);
    }

    // Stroke \\

    private void beginStroke(float mouseX, float mouseY) {

        resolveFaceMirrors();
        collectStamp(mouseX, mouseY);

        if (stamp.isEmpty() || !characterEditorManager.beginPaint())
            return;

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
        this.stroking = true;
        this.strokeX = mouseX;
        this.strokeY = mouseY;
        characterEditorManager.paint(stamp);
    }

    private void updateStroke(RawInputHandle rawInput) {

        if (!rawInput.isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT)) {
            characterEditorManager.endPaint();
            this.stroking = false;
            return;
        }

        continueStroke(rawInput.getMouseX(), rawInput.getMouseY());
    }

    // A stamp every spacing from the last toward the pointer, the stroke left on its last stamp
    private void continueStroke(float mouseX, float mouseY) {

        float dx = mouseX - strokeX;
        float dy = mouseY - strokeY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float spacing = BrushUtility.resolveSpacing(
                brushManager.getActiveBrush(), characterEditorManager.getBrushRadius());
        int stamps = (int) (length / spacing);

        for (int i = 1; i <= stamps; i++) {

            float t = i * spacing / length;

            collectStamp(strokeX + dx * t, strokeY + dy * t);
            characterEditorManager.paint(stamp);
        }

        if (stamps == 0)
            return;

        float travelled = stamps * spacing / length;

        this.strokeX += dx * travelled;
        this.strokeY += dy * travelled;
    }

    // Every face of the region the brush may reach whose middle the camera sees, with its corners on the window
    private void collectStamp(float x, float y) {

        float radius = characterEditorManager.getBrushRadius();
        RiggedMeshStruct mesh = characterEditorMeshViewSystem.getActiveDocument().getMesh();

        stamp.begin(x, y, radius);
        projectVertices();

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (isUnderBrush(mesh, f, x, y, radius) && isFaceShown(mesh, f)) {

                stamp.addFace(f);

                for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                    int vertex = mesh.getFaceVertex(f, c);
                    stamp.addCorner(screens.getFloat(vertex * 2), screens.getFloat(vertex * 2 + 1));
                }
            }

        if (mirrorVertices != null)
            addMirroredFaces(mesh);
    }

    // Every face covered laid again on its twin, each twin corner where its own twin shows on the window
    private void addMirroredFaces(RiggedMeshStruct mesh) {

        int covered = stamp.getFaceCount();

        for (int i = 0; i < covered; i++) {

            int twinFace = face2Mirror.getInt(stamp.getFace(i));

            if (twinFace == EngineSetting.INDEX_NOT_FOUND || !isTwinShown(mesh, twinFace))
                continue;

            stamp.addFace(twinFace);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int twin = mirrorVertices.getInt(mesh.getFaceVertex(twinFace, c));
                stamp.addCorner(screens.getFloat(twin * 2), screens.getFloat(twin * 2 + 1));
            }
        }
    }

    // Whether every corner's twin shows on the window, so the twin face can take the covered face's place there
    private boolean isTwinShown(RiggedMeshStruct mesh, int twinFace) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (Float.isNaN(screens.getFloat(mirrorVertices.getInt(mesh.getFaceVertex(twinFace, c)) * 2)))
                return false;

        return true;
    }

    // Each face's twin across the body, found once a stroke while it mirrors: the face whose corners are its
    // corners' twins
    private void resolveFaceMirrors() {

        this.mirrorVertices = characterEditorManager.getMirrorVertices();

        if (mirrorVertices == null)
            return;

        RiggedMeshStruct mesh = characterEditorMeshViewSystem.getActiveDocument().getMesh();
        int faceCount = mesh.getFaceCount();

        corners2Face.clear();
        face2Mirror.size(faceCount);

        for (int f = 0; f < faceCount; f++)
            corners2Face.put(toCornerKey(mesh, f, false), f);

        for (int f = 0; f < faceCount; f++)
            face2Mirror.set(f, corners2Face.get(toCornerKey(mesh, f, true)));
    }

    // A face's corners, or their twins, as one key whatever their order: sorted, sixteen bits each
    private long toCornerKey(RiggedMeshStruct mesh, int faceIndex, boolean twins) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int vertex = mesh.getFaceVertex(faceIndex, c);
            cornerScratch[c] = twins ? mirrorVertices.getInt(vertex) : vertex;

            if (cornerScratch[c] == EngineSetting.INDEX_NOT_FOUND)
                return EngineSetting.INDEX_NOT_FOUND;
        }

        Arrays.sort(cornerScratch);

        long key = 0L;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            key = (key << Short.SIZE) | cornerScratch[c];

        return key;
    }

    // Whether a face's corners all show on the window and their bounds meet the brush's
    private boolean isUnderBrush(RiggedMeshStruct mesh, int faceIndex, float x, float y, float radius) {

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int vertex = mesh.getFaceVertex(faceIndex, c);
            float sx = screens.getFloat(vertex * 2);
            float sy = screens.getFloat(vertex * 2 + 1);

            if (Float.isNaN(sx))
                return false;

            minX = Math.min(minX, sx);
            minY = Math.min(minY, sy);
            maxX = Math.max(maxX, sx);
            maxY = Math.max(maxY, sy);
        }

        return maxX >= x - radius && minX <= x + radius && maxY >= y - radius && minY <= y + radius;
    }

    // Whether a face belongs to the tab's region and the camera sees its middle
    private boolean isFaceShown(RiggedMeshStruct mesh, int faceIndex) {

        float x = 0f;
        float y = 0f;
        float z = 0f;
        boolean inRegion = false;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int vertex = mesh.getFaceVertex(faceIndex, c);

            inRegion |= characterEditorMeshViewSystem.isInRegion(vertex);
            x += characterEditorMeshViewSystem.getX(vertex);
            y += characterEditorMeshViewSystem.getY(vertex);
            z += characterEditorMeshViewSystem.getZ(vertex);
        }

        float share = 1f / EngineSetting.QUAD_VERTEX_COUNT;

        return inRegion && characterEditorMeshViewSystem.isPointShown(x * share, y * share, z * share);
    }

    // The active part's vertices on the window, once a frame
    private void projectVertices() {

        if (projected)
            return;

        this.projected = true;

        int vertexCount = characterEditorMeshViewSystem.getVertexCount();

        screens.clear();
        screens.ensureCapacity(vertexCount * 2);

        for (int v = 0; v < vertexCount; v++) {

            boolean shown = characterEditorMeshViewSystem.project(
                    characterEditorMeshViewSystem.getX(v),
                    characterEditorMeshViewSystem.getY(v),
                    characterEditorMeshViewSystem.getZ(v),
                    screenScratch);

            screens.add(shown ? screenScratch.x : Float.NaN);
            screens.add(shown ? screenScratch.y : Float.NaN);
        }
    }
}
