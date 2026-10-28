package editor.runtime.charactereditor.sculpt;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSelection;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorTransform;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.runtime.input.EditorInputSystem;
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
     * open. A click picks the vertex under the pointer and a drag selects a
     * box, Shift adding and Ctrl taking away; in Weights mode the Brush and
     * Erase tools stroke the weights instead. Each mesh mode is a tool canvas
     * of ToolManager's while this tab is open. A transform the panel asks for — by its
     * button or its shortcut, G, R or S — holds the selection to the
     * pointer: X, Y and Z lock it to an axis, Ctrl snaps it, a click or Enter
     * confirms and a right click or Escape puts it back. The tab's shortcuts
     * run here only with the pointer over the view and nothing under way,
     * each the panel's own action by its row key, so a key and its button do
     * exactly the same thing; the modifiers a selection and a transform read
     * are held shortcuts too.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CharacterEditorManager characterEditorManager;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private ShortcutManager shortcutManager;
    private ToolManager toolManager;

    // Shortcuts
    private int addSelectionShortcutID;
    private int subtractSelectionShortcutID;
    private int snapShortcutID;

    // Selection
    private boolean pressed;
    private boolean boxSelecting;
    private float pressX;
    private float pressY;

    // Stroke
    private boolean stroking;

    // Transform
    private CharacterEditorTransform transform;
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
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditorMeshViewSystem = get(CharacterEditorMeshViewSystem.class);
        this.shortcutManager = get(ShortcutManager.class);
        this.toolManager = get(ToolManager.class);
    }

    @Override
    protected void awake() {
        toolManager.openCanvas(context.getWindow(), characterEditorManager);
    }

    @Override
    protected void dispose() {
        toolManager.closeCanvas(context.getWindow());
    }

    @Override
    protected void start() {

        // Shortcuts
        this.addSelectionShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                CharacterEditorSetting.SHORTCUT_ADD_SELECTION);
        this.subtractSelectionShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                CharacterEditorSetting.SHORTCUT_SUBTRACT_SELECTION);
        this.snapShortcutID = shortcutManager.getShortcutIDFromShortcutName(CharacterEditorSetting.SHORTCUT_SNAP);
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
            startTransform(requested, rawInput);
            return;
        }

        if (!editorInputSystem.isPointerActive() || rawInput.isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_ORBIT))
            return;

        if (editorInputSystem.isClicked(CharacterEditorSetting.BUTTON_SELECT))
            press(rawInput);
    }

    // Drops whatever the pointer was holding when the mesh modes close or lose their mesh
    private void abandon() {

        if (transform != null)
            characterEditorManager.cancelTransform();

        if (stroking)
            characterEditorManager.endStroke();

        this.transform = null;
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
                && !editorInputSystem.getRawInputHandle().isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_ORBIT);
    }

    private boolean isShortcutHeld(int shortcutID) {
        return shortcutManager.isHeld(shortcutID, context.getWindow());
    }

    // Selection \\

    private void press(RawInputHandle rawInput) {

        if (characterEditorManager.isBrushTool()) {
            characterEditorManager.beginStroke();
            this.stroking = true;
            applyStroke(rawInput);
            return;
        }

        this.pressed = true;
        this.boxSelecting = false;
        this.pressX = rawInput.getMouseX();
        this.pressY = rawInput.getMouseY();
    }

    // A press held past the drag distance becomes a box; letting go picks the vertex or the box
    private void updateSelection(RawInputHandle rawInput) {

        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (rawInput.isButtonHeld(CharacterEditorSetting.BUTTON_SELECT)) {

            float dx = mouseX - pressX;
            float dy = mouseY - pressY;

            if (dx * dx + dy * dy > CharacterEditorSetting.DRAG_PIXELS * CharacterEditorSetting.DRAG_PIXELS)
                this.boxSelecting = true;

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

    private void updateStroke(RawInputHandle rawInput) {

        if (!rawInput.isButtonHeld(CharacterEditorSetting.BUTTON_SELECT)) {
            characterEditorManager.endStroke();
            this.stroking = false;
            return;
        }

        applyStroke(rawInput);
    }

    // The brush's strength scaled by the frame's length, so a stroke paints at the same rate however fast it runs
    private void applyStroke(RawInputHandle rawInput) {

        float strength = characterEditorManager.getWeightStrength()
                * Math.min(1f, internal.getDeltaTime() * CharacterEditorSetting.BRUSH_RATE);

        characterEditorMeshViewSystem.collectBrush(
                rawInput.getMouseX(),
                rawInput.getMouseY(),
                characterEditorManager.getBrushRadius(),
                strength,
                hits,
                strengths);

        if (!hits.isEmpty())
            characterEditorManager.stroke(hits, strengths);
    }

    // Transform \\

    private void startTransform(CharacterEditorTransform requested, RawInputHandle rawInput) {

        boolean requestedAxis = characterEditorManager.hasRequestedAxis();

        if (requestedAxis)
            axisVector.set(characterEditorManager.getRequestedAxis());

        if (!characterEditorManager.beginTransform())
            return;

        this.transform = requested;
        this.transformAxis = EngineSetting.INDEX_NOT_FOUND;
        this.customAxis = requestedAxis;
        this.startMouseX = rawInput.getMouseX();
        this.startMouseY = rawInput.getMouseY();

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
            resolvePlaneHit(startHit);
    }

    private void updateTransform(RawInputHandle rawInput) {

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)
                || rawInput.isButtonClicked(CharacterEditorSetting.BUTTON_CANCEL)) {
            characterEditorManager.cancelTransform();
            this.transform = null;
            return;
        }

        lockAxis(rawInput);
        applyTransform(rawInput);

        if (rawInput.isButtonClicked(CharacterEditorSetting.BUTTON_SELECT)
                || rawInput.isKeyClicked(EditorSetting.KEY_ENTER)
                || rawInput.isKeyClicked(EditorSetting.KEY_ENTER_NUMPAD)) {
            characterEditorManager.confirmTransform();
            this.transform = null;
        }
    }

    // X, Y or Z holds the transform to that axis, the same key again frees it
    private void lockAxis(RawInputHandle rawInput) {

        for (int axis = 0; axis < CharacterEditorSetting.KEY_AXES.length; axis++) {

            if (!rawInput.isKeyClicked(CharacterEditorSetting.KEY_AXES[axis]))
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

            resolvePlaneHit(hitScratch);
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
            angle = (float) Math.toRadians(Math.round(Math.toDegrees(angle) / CharacterEditorSetting.SNAP_DEGREES)
                    * CharacterEditorSetting.SNAP_DEGREES);

        characterEditorManager.updateTransform(CharacterEditorTransform.ROTATE, vectorScratch, angle);
    }

    // The pointer's distance from the pivot on screen against where it started, on the axis held or every axis
    private void applyScale(float mouseX, float mouseY, boolean snap) {

        float start = Math.max(
                CharacterEditorSetting.SCALE_MIN_PIXELS,
                (float) Math.hypot(startMouseX - pivotScreen.x, startMouseY - pivotScreen.y));
        float factor = (float) Math.hypot(mouseX - pivotScreen.x, mouseY - pivotScreen.y) / start;

        if (snap)
            factor = Math.round(factor / CharacterEditorSetting.SNAP_SCALE) * CharacterEditorSetting.SNAP_SCALE;

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

    // Where the pointer's ray meets the plane through the pivot facing the camera
    private void resolvePlaneHit(Vector3 out) {

        Vector3 normal = context.getWindow().getActiveCamera().getDirection();
        float facing = rayDirection.x * normal.x + rayDirection.y * normal.y + rayDirection.z * normal.z;

        if (Math.abs(facing) < CharacterEditorSetting.RAY_EPSILON) {
            out.set(pivot);
            return;
        }

        float t = ((pivot.x - rayOrigin.x) * normal.x
                + (pivot.y - rayOrigin.y) * normal.y
                + (pivot.z - rayOrigin.z) * normal.z) / facing;

        out.set(
                rayOrigin.x + rayDirection.x * t,
                rayOrigin.y + rayDirection.y * t,
                rayOrigin.z + rayDirection.z * t);
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

        if (denominator < CharacterEditorSetting.RAY_EPSILON)
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

    // Whether the weight brush shows under the pointer
    public boolean isBrushShown() {
        return characterEditorManager.isBrushTool() && (stroking || editorInputSystem.isPointerActive());
    }
}
