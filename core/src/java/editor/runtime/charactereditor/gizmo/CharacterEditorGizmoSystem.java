package editor.runtime.charactereditor.gizmo;

import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorTransform;
import editor.runtime.EditorSetting;
import editor.runtime.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.runtime.charactereditor.sculpt.CharacterEditorSculptSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector4;

public class CharacterEditorGizmoSystem extends SystemPackage {

    /*
     * The transform gizmo on the Mesh tab's selection, as every 3D tool
     * shows one: while Move, Rotate or Scale is the tool and something is
     * selected, red, green and blue handles for X, Y and Z stand at the
     * selection's centre as the view shows it, the tool choosing which —
     * Move an arrow along each axis, Rotate a ring about it, Scale a shaft
     * ending in a cube — each as long on screen whatever the zoom. The
     * handle under the pointer is drawn in the hovered colour, and a press
     * on it is taken by the sculpt system as a drag held to its axis
     * (getHoveredAxis). Nothing is drawn while a transform is under way,
     * whose own axis line shows instead.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CharacterEditorManager characterEditorManager;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private CharacterEditorSculptSystem characterEditorSculptSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;
    private ViewCameraSystem viewCameraSystem;

    // Gizmo — the axis the pointer is over, or none, and each axis's colour and the hovered one
    private int hoveredAxis;
    private Vector4[] axisColors;
    private Vector4 hoverColor;

    // Scratch
    private Vector3 pivot;
    private Vector3 axisEnd;
    private Vector3 pointA;
    private Vector3 pointB;
    private Vector2 screenPivot;
    private Vector2 screenA;
    private Vector2 screenB;

    // Base \\

    @Override
    protected void create() {

        // Gizmo
        this.hoveredAxis = EngineSetting.INDEX_NOT_FOUND;
        this.axisColors = new Vector4[EditorSetting.TRANSFORM_GIZMO_COLORS.length];

        for (int i = 0; i < axisColors.length; i++) {
            float[] color = EditorSetting.TRANSFORM_GIZMO_COLORS[i];
            axisColors[i] = new Vector4(color[0], color[1], color[2], color[3]);
        }

        float[] hover = EditorSetting.TRANSFORM_GIZMO_HOVER_COLOR;
        this.hoverColor = new Vector4(hover[0], hover[1], hover[2], hover[3]);

        // Scratch
        this.pivot = new Vector3();
        this.axisEnd = new Vector3();
        this.pointA = new Vector3();
        this.pointB = new Vector3();
        this.screenPivot = new Vector2();
        this.screenA = new Vector2();
        this.screenB = new Vector2();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditorMeshViewSystem = get(CharacterEditorMeshViewSystem.class);
        this.characterEditorSculptSystem = get(CharacterEditorSculptSystem.class);
        this.characterViewRenderSystem = get(CharacterViewRenderSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        this.hoveredAxis = EngineSetting.INDEX_NOT_FOUND;

        CharacterEditorTransform transform = characterEditorManager.getToolTransform();

        if (transform == null || characterEditorSculptSystem.isTransforming()
                || !characterEditorMeshViewSystem.hasActiveDocument()
                || !characterEditorManager.resolveGizmoPivot(characterEditorMeshViewSystem.getSkinningMatrices(), pivot)
                || !characterEditorMeshViewSystem.project(pivot.x, pivot.y, pivot.z, screenPivot))
            return;

        float length = resolveHandleLength();

        if (length <= 0f)
            return;

        if (editorInputSystem.isPointerOver())
            this.hoveredAxis = resolveHoveredAxis(transform, length);

        for (int axis = 0; axis < axisColors.length; axis++)
            drawHandle(transform, axis, length, axis == hoveredAxis ? hoverColor : axisColors[axis]);
    }

    // The world length a handle takes to reach its length in pixels at the pivot's depth
    private float resolveHandleLength() {

        Vector3 right = viewCameraSystem.getRight();

        if (!characterEditorMeshViewSystem.project(pivot.x + right.x, pivot.y + right.y, pivot.z + right.z, screenA))
            return 0f;

        float pixelsPerUnit = (float) Math.hypot(screenA.x - screenPivot.x, screenA.y - screenPivot.y);

        return pixelsPerUnit > 0f ? EditorSetting.TRANSFORM_GIZMO_PIXELS / pixelsPerUnit : 0f;
    }

    // Hover \\

    // The axis whose handle lies nearest the pointer on screen within the pick distance, or none
    private int resolveHoveredAxis(CharacterEditorTransform transform, float length) {

        float mouseX = editorInputSystem.getRawInputHandle().getMouseX();
        float mouseY = editorInputSystem.getRawInputHandle().getMouseY();
        float nearest = EditorSetting.TRANSFORM_GIZMO_PICK_PIXELS;
        int hovered = EngineSetting.INDEX_NOT_FOUND;

        for (int axis = 0; axis < axisColors.length; axis++) {

            float distance = transform == CharacterEditorTransform.ROTATE
                    ? resolveRingDistance(axis, length, mouseX, mouseY)
                    : resolveShaftDistance(axis, length, mouseX, mouseY);

            if (distance < nearest) {
                nearest = distance;
                hovered = axis;
            }
        }

        return hovered;
    }

    private float resolveShaftDistance(int axis, float length, float mouseX, float mouseY) {

        resolveAxisPoint(axis, length, axisEnd);

        if (!characterEditorMeshViewSystem.project(axisEnd.x, axisEnd.y, axisEnd.z, screenA))
            return Float.MAX_VALUE;

        return resolveSegmentDistance(screenPivot, screenA, mouseX, mouseY);
    }

    private float resolveRingDistance(int axis, float length, float mouseX, float mouseY) {

        float nearest = Float.MAX_VALUE;
        int segments = EditorSetting.TRANSFORM_GIZMO_RING_SEGMENTS;

        resolveRingPoint(axis, length, 0, pointA);
        boolean shownA = characterEditorMeshViewSystem.project(pointA.x, pointA.y, pointA.z, screenA);

        for (int i = 1; i <= segments; i++) {

            resolveRingPoint(axis, length, i, pointB);
            boolean shownB = characterEditorMeshViewSystem.project(pointB.x, pointB.y, pointB.z, screenB);

            if (shownA && shownB)
                nearest = Math.min(nearest, resolveSegmentDistance(screenA, screenB, mouseX, mouseY));

            screenA.set(screenB.x, screenB.y);
            shownA = shownB;
        }

        return nearest;
    }

    // The distance in pixels from a point to a segment on screen
    private float resolveSegmentDistance(Vector2 from, Vector2 to, float x, float y) {

        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float lengthSquared = dx * dx + dy * dy;
        float t = lengthSquared > 0f
                ? Math.clamp(((x - from.x) * dx + (y - from.y) * dy) / lengthSquared, 0f, 1f)
                : 0f;

        return (float) Math.hypot(x - (from.x + dx * t), y - (from.y + dy * t));
    }

    // Draw \\

    // One axis's handle as the tool shows it: an arrow for Move, a ring for Rotate, a shaft and cube for Scale
    private void drawHandle(CharacterEditorTransform transform, int axis, float length, Vector4 color) {

        if (transform == CharacterEditorTransform.ROTATE) {
            drawRing(axis, length, color);
            return;
        }

        float shaft = length * EditorSetting.TRANSFORM_GIZMO_SHAFT_SHARE;

        if (transform == CharacterEditorTransform.SCALE) {
            resolveAxisPoint(axis, length, axisEnd);
            characterViewRenderSystem.addGizmoSegment(
                    pivot, axisEnd, shaft, EditorSetting.CHARACTER_VIEW_MARKER_CUBE, color);
            characterViewRenderSystem.addGizmoCube(
                    axisEnd, length * EditorSetting.TRANSFORM_GIZMO_CUBE_SHARE, color);
            return;
        }

        float head = length * EditorSetting.TRANSFORM_GIZMO_HEAD_SHARE;

        resolveAxisPoint(axis, length - head, pointA);
        resolveAxisPoint(axis, length, axisEnd);
        characterViewRenderSystem.addGizmoSegment(
                pivot, pointA, shaft, EditorSetting.CHARACTER_VIEW_MARKER_CUBE, color);
        characterViewRenderSystem.addGizmoSegment(
                pointA, axisEnd, head * EditorSetting.TRANSFORM_GIZMO_HEAD_WIDTH_SHARE,
                EditorSetting.CHARACTER_VIEW_MARKER_OCTAHEDRAL, color);
    }

    private void drawRing(int axis, float length, Vector4 color) {

        float width = length * EditorSetting.TRANSFORM_GIZMO_SHAFT_SHARE;

        resolveRingPoint(axis, length, 0, pointA);

        for (int i = 1; i <= EditorSetting.TRANSFORM_GIZMO_RING_SEGMENTS; i++) {

            resolveRingPoint(axis, length, i, pointB);
            characterViewRenderSystem.addGizmoSegment(
                    pointA, pointB, width, EditorSetting.CHARACTER_VIEW_MARKER_CUBE, color);
            pointA.set(pointB.x, pointB.y, pointB.z);
        }
    }

    // Geometry \\

    // The point a distance along an axis from the pivot
    private void resolveAxisPoint(int axis, float distance, Vector3 out) {
        out.set(
                pivot.x + (axis == EngineSetting.AXIS_X ? distance : 0f),
                pivot.y + (axis == EngineSetting.AXIS_Y ? distance : 0f),
                pivot.z + (axis == EngineSetting.AXIS_Z ? distance : 0f));
    }

    // A point of the ring about an axis, a step round it, as wide as the handle's length
    private void resolveRingPoint(int axis, float length, int step, Vector3 out) {

        double angle = Math.PI * 2.0 * step / EditorSetting.TRANSFORM_GIZMO_RING_SEGMENTS;
        float a = (float) Math.cos(angle) * length;
        float b = (float) Math.sin(angle) * length;

        out.set(
                pivot.x + (axis == EngineSetting.AXIS_X ? 0f : a),
                pivot.y + (axis == EngineSetting.AXIS_Y ? 0f : axis == EngineSetting.AXIS_X ? a : b),
                pivot.z + (axis == EngineSetting.AXIS_Z ? 0f : b));
    }

    // Accessible \\

    // The axis whose handle the pointer is over, which a press drags along, or none
    public int getHoveredAxis() {
        return hoveredAxis;
    }
}
