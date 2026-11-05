package editor.runtime.charactereditor.sculpt;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.brushpipeline.util.BrushUtility;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterStampStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSelection;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorTransform;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.viewcamera.ViewCameraSystem;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class CharacterEditorSculptSystem extends SystemPackage {

    /*
     * Turns this tab's pointer and keys into mesh edits while a mesh mode is
     * open. A press works the mesh only where it lands on it; a press beside
     * it is left to the view camera, a click there still clearing the
     * selection. A click picks the vertex under the pointer and a drag from a
     * vertex selects a box, Shift adding and Ctrl taking away, either of them
     * boxing from anywhere; in Mesh mode the Move, Rotate and Scale tools drag
     * the selection from a vertex instead, picking it first when it is not
     * selected, letting go to confirm; in Sculpt mode every tool strokes the
     * surface the brush covers, Ctrl inverting the stroke, and in Weights mode
     * the Brush, Erase and Smooth tools stroke the weights. Each stamp of a
     * stroke is placed on the mesh at the depth of the vertex nearest the
     * brush's centre, which gives the brush its radius there and the distance
     * the pointer carried it since the last stamp. A transform asked for — by
     * its Tools panel action or its shortcut, G, R or S — holds the selection
     * to the pointer: X, Y and Z lock it to an axis, Ctrl snaps it, a click or
     * Enter confirms and a right click or Escape puts it back, each click
     * claimed from the camera. The tab's shortcuts run here only with the
     * pointer over the view and nothing under way, each the action of the same
     * key, so a key and its button do exactly the same thing; the modifiers a
     * selection and a transform read are held shortcuts too.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ViewCameraSystem viewCameraSystem;
    private CharacterEditorManager characterEditorManager;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private ShortcutManager shortcutManager;
    private BrushManager brushManager;

    // Shortcuts
    private int addSelectionShortcutID;
    private int subtractSelectionShortcutID;
    private int snapShortcutID;
    private int invertStrokeShortcutID;

    // Selection
    private boolean pressed;
    private boolean pressClaimed;
    private boolean boxSelecting;
    private float pressX;
    private float pressY;

    // Stroke
    private boolean stroking;
    private float strokeX;
    private float strokeY;
    private float stampX;
    private float stampY;
    private CharacterStampStruct stamp;
    private Vector3 stampDepth;

    // Transform
    private CharacterEditorTransform transform;
    private boolean draggingTransform;
    private int transformAxis;
    private boolean customAxis;
    private Vector3 axisVector;
    private Vector3 pivot;
    private Vector2 pivotScreen;
    private Vector3 startHit;
    private float startMouseX;
    private float startMouseY;
    private float startAxisParameter;

    // Scratch
    private IntArrayList hits;
    private FloatArrayList strengths;
    private Vector3 rayOrigin;
    private Vector3 rayDirection;
    private Vector3 vectorScratch;
    private Vector3 hitScratch;

    // Base \\

    @Override
    protected void create() {

        // Stroke
        this.stamp = new CharacterStampStruct();
        this.stampDepth = new Vector3();

        // Transform
        this.transformAxis = EngineSetting.INDEX_NOT_FOUND;
        this.axisVector = new Vector3();
        this.pivot = new Vector3();
        this.pivotScreen = new Vector2();
        this.startHit = new Vector3();

        // Scratch
        this.hits = new IntArrayList();
        this.strengths = new FloatArrayList();
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();
        this.vectorScratch = new Vector3();
        this.hitScratch = new Vector3();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditorMeshViewSystem = get(CharacterEditorMeshViewSystem.class);
        this.shortcutManager = get(ShortcutManager.class);
        this.brushManager = get(BrushManager.class);
    }

    @Override
    protected void start() {

        // Shortcuts
        this.addSelectionShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                CharacterEditorSetting.SHORTCUT_ADD_SELECTION);
        this.subtractSelectionShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                CharacterEditorSetting.SHORTCUT_SUBTRACT_SELECTION);
        this.snapShortcutID = shortcutManager.getShortcutIDFromShortcutName(CharacterEditorSetting.SHORTCUT_SNAP);
        this.invertStrokeShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                CharacterEditorSetting.SHORTCUT_INVERT_STROKE);
    }

    // Update \\

    @Override
    protected void update() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (!characterEditorManager.getMode().isMeshMode() || !characterEditorMeshViewSystem.hasActiveDocument()) {
            abandon();
            return;
        }

        if (transform != null) {
            updateTransform(rawInput);
            return;
        }

        if (stroking) {
            updateStroke(rawInput);
            return;
        }

        if (pressed) {
            updateSelection(rawInput);
            return;
        }

        CharacterEditorTransform requested = characterEditorManager.takeTransformRequest();

        if (requested != null) {
            startTransform(requested, rawInput.getMouseX(), rawInput.getMouseY());
            return;
        }

        if (!editorInputSystem.isPointerActive() || viewCameraSystem.isDragging())
            return;

        if (editorInputSystem.isClicked(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT))
            press(rawInput.getMouseX(), rawInput.getMouseY());
    }

    // Drops whatever the pointer was holding when the mesh modes close or lose their mesh
    private void abandon() {

        if (transform != null)
            characterEditorManager.cancelTransform();

        if (stroking)
            characterEditorManager.endStroke();

        this.transform = null;
        this.draggingTransform = false;
        this.stroking = false;
        this.pressed = false;
        this.boxSelecting = false;
    }

    // Shortcuts \\

    // One of the panel's actions by its row key, in either mesh mode
    public void act(String key) {
        if (isReady())
            characterEditorManager.act(key);
    }

    // One of the panel's actions on the mesh itself by its row key, only while the mesh is edited
    public void actMesh(String key) {
        if (isReady() && characterEditorManager.getMode() == CharacterEditorMode.MESH)
            characterEditorManager.act(key);
    }

    // Everything selected when nothing is, nothing otherwise
    public void toggleSelection() {
        if (isReady())
            characterEditorManager.toggleSelection();
    }

    // A shortcut acts only with the pointer over the view while nothing is pressed, painted, held or orbited
    private boolean isReady() {
        return characterEditorManager.getMode().isMeshMode() && characterEditorMeshViewSystem.hasActiveDocument()
                && transform == null && !stroking && !pressed && editorInputSystem.isPointerActive()
                && !viewCameraSystem.isDragging();
    }

    private boolean isShortcutHeld(int shortcutID) {
        return shortcutManager.isHeld(shortcutID, context.getWindow());
    }

    // Selection \\

    // A brush strokes where it covers the mesh and a transform tool drags the selection from a vertex, picking the
    // vertex first; any other press selects, claimed from the camera only on a vertex or with a modifier held
    private void press(float mouseX, float mouseY) {

        if (characterEditorManager.isBrushTool()) {
            beginStroke(mouseX, mouseY);
            return;
        }

        int vertexIndex = characterEditorMeshViewSystem.pickVertex(mouseX, mouseY);
        boolean modified = isShortcutHeld(addSelectionShortcutID) || isShortcutHeld(subtractSelectionShortcutID);
        CharacterEditorTransform toolTransform = characterEditorManager.getToolTransform();

        if (toolTransform != null && vertexIndex != EngineSetting.INDEX_NOT_FOUND && !modified) {

            if (!characterEditorMeshViewSystem.getActiveDocument().isSelected(vertexIndex))
                characterEditorManager.selectVertex(vertexIndex, CharacterEditorSelection.REPLACE);

            startTransform(toolTransform, mouseX, mouseY);
            this.draggingTransform = transform != null;

            if (draggingTransform) {
                editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
                return;
            }
        }

        this.pressed = true;
        this.pressClaimed = modified || vertexIndex != EngineSetting.INDEX_NOT_FOUND;
        this.boxSelecting = false;
        this.pressX = mouseX;
        this.pressY = mouseY;

        if (pressClaimed)
            editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
    }

    // A claimed press held past the drag distance becomes a box, one the camera took is let go; letting go picks the
    // vertex or the box
    private void updateSelection(RawInputHandle rawInput) {

        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (rawInput.isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT)) {

            float dx = mouseX - pressX;
            float dy = mouseY - pressY;

            if (dx * dx + dy * dy > CharacterEditorSetting.DRAG_PIXELS * CharacterEditorSetting.DRAG_PIXELS) {
                this.boxSelecting = pressClaimed;
                this.pressed = pressClaimed;
            }

            return;
        }

        boolean shift = isShortcutHeld(addSelectionShortcutID);
        boolean control = isShortcutHeld(subtractSelectionShortcutID);

        if (boxSelecting) {
            characterEditorMeshViewSystem.collectBox(pressX, pressY, mouseX, mouseY, hits);
            characterEditorManager.selectVertices(hits, resolveSelection(shift, control, CharacterEditorSelection.ADD));
        } else
            pickVertex(mouseX, mouseY, shift, control);

        this.pressed = false;
        this.boxSelecting = false;
    }

    // A click on a vertex picks it; a click on nothing clears the selection unless a modifier is held
    private void pickVertex(float mouseX, float mouseY, boolean shift, boolean control) {

        int vertexIndex = characterEditorMeshViewSystem.pickVertex(mouseX, mouseY);

        if (vertexIndex == EngineSetting.INDEX_NOT_FOUND && (shift || control))
            return;

        characterEditorManager.selectVertex(
                vertexIndex,
                resolveSelection(shift, control, CharacterEditorSelection.TOGGLE));
    }

    private CharacterEditorSelection resolveSelection(
            boolean shift,
            boolean control,
            CharacterEditorSelection shifted) {

        if (control)
            return CharacterEditorSelection.SUBTRACT;

        return shift ? shifted : CharacterEditorSelection.REPLACE;
    }

    // Stroke \\

    // A stroke begins only where the brush covers the mesh, a press beside it left to the camera
    private void beginStroke(float mouseX, float mouseY) {

        characterEditorMeshViewSystem.collectBrush(
                mouseX,
                mouseY,
                characterEditorManager.getBrushRadius(),
                characterEditorManager.getBrushStrength(),
                hits,
                strengths);

        if (hits.isEmpty())
            return;

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
        characterEditorManager.beginStroke();
        this.stroking = true;
        this.strokeX = mouseX;
        this.strokeY = mouseY;
        this.stampX = mouseX;
        this.stampY = mouseY;
        stamp(mouseX, mouseY);
    }

    private void updateStroke(RawInputHandle rawInput) {

        if (!rawInput.isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT)) {
            characterEditorManager.endStroke();
            this.stroking = false;
            return;
        }

        continueStroke(rawInput.getMouseX(), rawInput.getMouseY());
    }

    // A stamp every spacing from the last toward the pointer, the stroke left on its last stamp, as an image stroke is
    private void continueStroke(float mouseX, float mouseY) {

        float dx = mouseX - strokeX;
        float dy = mouseY - strokeY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float spacing = BrushUtility.resolveSpacing(
                brushManager.getActiveBrush(), characterEditorManager.getBrushRadius());
        int stamps = (int) (length / spacing);

        for (int i = 1; i <= stamps; i++) {

            float t = i * spacing / length;

            stamp(strokeX + dx * t, strokeY + dy * t);
        }

        if (stamps == 0)
            return;

        float travelled = stamps * spacing / length;

        this.strokeX += dx * travelled;
        this.strokeY += dy * travelled;
    }

    // One stamp of the brush, at the tool's strength scaled by how much of the tip covers each vertex
    private void stamp(float x, float y) {

        characterEditorMeshViewSystem.collectBrush(
                x,
                y,
                characterEditorManager.getBrushRadius(),
                characterEditorManager.getBrushStrength(),
                hits,
                strengths);

        if (!hits.isEmpty()) {
            placeStamp(x, y);
            characterEditorManager.stroke(hits, strengths, stamp);
        }

        this.stampX = x;
        this.stampY = y;
    }

    // The stamp's centre, radius and drag on the plane facing the camera through the vertex the brush covers most
    private void placeStamp(float x, float y) {

        int nearest = hits.getInt(0);
        float strongest = strengths.getFloat(0);

        for (int i = 1; i < hits.size(); i++)
            if (strengths.getFloat(i) > strongest) {
                strongest = strengths.getFloat(i);
                nearest = hits.getInt(i);
            }

        Vector3 view = context.getWindow().getActiveCamera().getDirection();

        stampDepth.set(
                characterEditorMeshViewSystem.getX(nearest),
                characterEditorMeshViewSystem.getY(nearest),
                characterEditorMeshViewSystem.getZ(nearest));
        stamp.getViewDirection().set(view);

        resolveAtDepth(x, y, stamp.getCenter());
        resolveAtDepth(x + characterEditorManager.getBrushRadius(), y, hitScratch);
        stamp.setRadius(hitScratch.subtract(stamp.getCenter()).length());
        resolveAtDepth(stampX, stampY, hitScratch);
        stamp.getDrag().set(stamp.getCenter()).subtract(hitScratch);
        stamp.setInverted(isShortcutHeld(invertStrokeShortcutID));
    }

    // Where a window pixel's ray meets the plane facing the camera through the stamp's depth
    private void resolveAtDepth(float x, float y, Vector3 out) {
        characterEditorMeshViewSystem.resolveRay(x, y, rayOrigin, rayDirection);
        resolvePlaneHit(stampDepth, out);
    }

    // Transform \\

    private void startTransform(CharacterEditorTransform requested, float mouseX, float mouseY) {

        boolean requestedAxis = characterEditorManager.hasRequestedAxis();

        if (requestedAxis)
            axisVector.set(characterEditorManager.getRequestedAxis());

        if (!characterEditorManager.beginTransform())
            return;

        this.transform = requested;
        this.transformAxis = EngineSetting.INDEX_NOT_FOUND;
        this.customAxis = requestedAxis;
        this.startMouseX = mouseX;
        this.startMouseY = mouseY;

        pivot.set(characterEditorManager.getTransformPivot());

        if (!characterEditorMeshViewSystem.project(pivot.x, pivot.y, pivot.z, pivotScreen))
            pivotScreen.set(startMouseX, startMouseY);

        anchorGrab();
    }

    // Where the pointer first meets the grab's plane or line, so the selection moves by how far it travels from there
    private void anchorGrab() {

        characterEditorMeshViewSystem.resolveRay(startMouseX, startMouseY, rayOrigin, rayDirection);

        if (hasAxis())
            this.startAxisParameter = resolveAxisParameter();
        else
            resolvePlaneHit(pivot, startHit);
    }

    // A dragged transform confirms when the button is let go, a held one on a click or Enter
    private void updateTransform(RawInputHandle rawInput) {

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)
                || rawInput.isButtonClicked(EditorSetting.CHARACTER_VIEW_BUTTON_CANCEL)) {
            editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_CANCEL);
            characterEditorManager.cancelTransform();
            endTransform();
            return;
        }

        lockAxis(rawInput);
        applyTransform(rawInput);

        boolean released = draggingTransform && !rawInput.isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);

        if (released || rawInput.isButtonClicked(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT)
                || rawInput.isKeyClicked(EditorSetting.KEY_ENTER)
                || rawInput.isKeyClicked(EditorSetting.KEY_ENTER_NUMPAD)) {
            editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
            characterEditorManager.confirmTransform();
            endTransform();
        }
    }

    private void endTransform() {
        this.transform = null;
        this.draggingTransform = false;
    }

    // X, Y or Z holds the transform to that axis, the same key again frees it
    private void lockAxis(RawInputHandle rawInput) {

        for (int axis = 0; axis < EditorSetting.CHARACTER_VIEW_KEY_AXES.length; axis++) {

            if (!rawInput.isKeyClicked(EditorSetting.CHARACTER_VIEW_KEY_AXES[axis]))
                continue;

            this.customAxis = false;
            this.transformAxis = transformAxis == axis ? EngineSetting.INDEX_NOT_FOUND : axis;
            axisVector.set(axis == 0 ? 1f : 0f, axis == 1 ? 1f : 0f, axis == 2 ? 1f : 0f);
            anchorGrab();
        }
    }

    private void applyTransform(RawInputHandle rawInput) {

        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();
        boolean snap = isShortcutHeld(snapShortcutID);

        switch (transform) {
            case GRAB -> applyGrab(mouseX, mouseY, snap);
            case ROTATE -> applyRotate(mouseX, mouseY, snap);
            case SCALE -> applyScale(mouseX, mouseY, snap);
        }
    }

    private void applyGrab(float mouseX, float mouseY, boolean snap) {

        characterEditorMeshViewSystem.resolveRay(mouseX, mouseY, rayOrigin, rayDirection);

        if (hasAxis()) {

            float distance = snapValue(resolveAxisParameter() - startAxisParameter, snap);
            vectorScratch.set(axisVector).multiply(distance);
        } else {

            resolvePlaneHit(pivot, hitScratch);
            vectorScratch.set(
                    snapValue(hitScratch.x - startHit.x, snap),
                    snapValue(hitScratch.y - startHit.y, snap),
                    snapValue(hitScratch.z - startHit.z, snap));
        }

        characterEditorManager.updateTransform(CharacterEditorTransform.GRAB, vectorScratch, 0f);
    }

    // The pointer's turn about the pivot on screen, about the axis held or the line of sight, following the pointer
    private void applyRotate(float mouseX, float mouseY, boolean snap) {

        float angle = (float) (Math.atan2(mouseY - pivotScreen.y, mouseX - pivotScreen.x)
                - Math.atan2(startMouseY - pivotScreen.y, startMouseX - pivotScreen.x));
        Vector3 viewDirection = context.getWindow().getActiveCamera().getDirection();

        if (hasAxis()) {

            vectorScratch.set(axisVector);

            if (vectorScratch.x * viewDirection.x + vectorScratch.y * viewDirection.y
                    + vectorScratch.z * viewDirection.z > 0f)
                angle = -angle;
        } else
            vectorScratch.set(-viewDirection.x, -viewDirection.y, -viewDirection.z).normalize();

        if (snap)
            angle = (float) Math.toRadians(Math.round(Math.toDegrees(angle) / EditorSetting.CHARACTER_VIEW_SNAP_DEGREES)
                    * EditorSetting.CHARACTER_VIEW_SNAP_DEGREES);

        characterEditorManager.updateTransform(CharacterEditorTransform.ROTATE, vectorScratch, angle);
    }

    // The pointer's distance from the pivot on screen against where it started, on the axis held or every axis
    private void applyScale(float mouseX, float mouseY, boolean snap) {

        float start = Math.max(
                EditorSetting.CHARACTER_VIEW_SCALE_MIN_PIXELS,
                (float) Math.hypot(startMouseX - pivotScreen.x, startMouseY - pivotScreen.y));
        float factor = (float) Math.hypot(mouseX - pivotScreen.x, mouseY - pivotScreen.y) / start;

        if (snap)
            factor = Math.round(factor / EditorSetting.CHARACTER_VIEW_SNAP_SCALE)
                    * EditorSetting.CHARACTER_VIEW_SNAP_SCALE;

        if (transformAxis == EngineSetting.INDEX_NOT_FOUND)
            vectorScratch.set(factor, factor, factor);
        else
            vectorScratch.set(
                    transformAxis == 0 ? factor : 1f,
                    transformAxis == 1 ? factor : 1f,
                    transformAxis == 2 ? factor : 1f);

        characterEditorManager.updateTransform(CharacterEditorTransform.SCALE, vectorScratch, 0f);
    }

    // Grab Geometry \\

    private boolean hasAxis() {
        return customAxis || transformAxis != EngineSetting.INDEX_NOT_FOUND;
    }

    // Where the pointer's ray meets the plane through a point facing the camera
    private void resolvePlaneHit(Vector3 point, Vector3 out) {
        CharacterPoseUtility.resolvePlaneHit(
                rayOrigin,
                rayDirection,
                point,
                context.getWindow().getActiveCamera().getDirection(),
                out);
    }

    // How far along the held axis through the pivot the point nearest the pointer's ray lies
    private float resolveAxisParameter() {

        float b = axisVector.x * rayDirection.x + axisVector.y * rayDirection.y + axisVector.z * rayDirection.z;
        float wx = pivot.x - rayOrigin.x;
        float wy = pivot.y - rayOrigin.y;
        float wz = pivot.z - rayOrigin.z;
        float d = axisVector.x * wx + axisVector.y * wy + axisVector.z * wz;
        float e = rayDirection.x * wx + rayDirection.y * wy + rayDirection.z * wz;
        float denominator = 1f - b * b;

        if (denominator < EditorSetting.CHARACTER_VIEW_RAY_EPSILON)
            return 0f;

        return (b * e - d) / denominator;
    }

    private float snapValue(float value, boolean snap) {

        float step = characterEditorManager.getSnapStep();

        if (!snap || step <= 0f)
            return value;

        return Math.round(value / step) * step;
    }

    // Accessible \\

    public boolean isBoxSelecting() {
        return boxSelecting;
    }

    public float getPressX() {
        return pressX;
    }

    public float getPressY() {
        return pressY;
    }

    public boolean isTransforming() {
        return transform != null;
    }

    // Whether the transform held is locked to a line, drawn through the pivot along the axis vector
    public boolean hasTransformAxis() {
        return transform != null && hasAxis();
    }

    public int getTransformAxis() {
        return customAxis ? EngineSetting.INDEX_NOT_FOUND : transformAxis;
    }

    public Vector3 getAxisVector() {
        return axisVector;
    }

    public Vector3 getPivot() {
        return pivot;
    }

    // Whether the brush shows under the pointer
    public boolean isBrushShown() {
        return characterEditorManager.isBrushTool() && (stroking || editorInputSystem.isPointerActive());
    }
}
