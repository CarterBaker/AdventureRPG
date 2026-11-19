package editor.runtime.charactereditor.groom;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.brushpipeline.brush.BrushHandle;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.brushpipeline.util.BrushUtility;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterGroomStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterStrandStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSelection;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.runtime.EditorSetting;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class CharacterEditorGroomSystem extends SystemPackage {

    /*
     * Turns this tab's pointer into grooming on the Hair layer. Draw roots a
     * strand on the surface pressed and carries it along the drag across the
     * plane through the root facing the camera; Place grows a straight
     * strand where it clicks; Pick takes the material of the strand nearest
     * the pointer. Every other tool strokes the brush over the strands: each
     * stamp hands the points the camera sees under the tip, strand by strand,
     * with the share of the tip each takes and how far the pointer moved in
     * the world across them, and Select, Shift adding and Ctrl taking away,
     * selects the strands it reaches. A press that reaches nothing is left to
     * the view camera, a selecting one clearing the selection first.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ViewCameraSystem viewCameraSystem;
    private CharacterEditorManager characterEditorManager;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private BrushManager brushManager;
    private ShortcutManager shortcutManager;

    // Shortcuts
    private int addSelectionShortcutID;
    private int subtractSelectionShortcutID;

    // Gesture — a strand being drawn on its plane, or a stroke and where the pointer stood its last frame
    private boolean drawing;
    private boolean stroking;
    private Vector3 planePoint;
    private Vector3 planeNormal;
    private float lastMouseX;
    private float lastMouseY;

    // Stamp — the points under the brush strand by strand, their shares and the drag across them
    private IntArrayList strands;
    private IntArrayList points;
    private FloatArrayList weights;
    private Vector3 centre;
    private Vector3 drag;

    // Scratch
    private Vector2 screen;
    private Vector3 rayOrigin;
    private Vector3 rayDirection;
    private Vector3 hitPoint;
    private Vector3 hitNormal;
    private Vector3 previousHit;

    // Base \\

    @Override
    protected void create() {

        // Gesture
        this.planePoint = new Vector3();
        this.planeNormal = new Vector3();

        // Stamp
        this.strands = new IntArrayList();
        this.points = new IntArrayList();
        this.weights = new FloatArrayList();
        this.centre = new Vector3();
        this.drag = new Vector3();

        // Scratch
        this.screen = new Vector2();
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();
        this.hitPoint = new Vector3();
        this.hitNormal = new Vector3();
        this.previousHit = new Vector3();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditorMeshViewSystem = get(CharacterEditorMeshViewSystem.class);
        this.brushManager = get(BrushManager.class);
        this.shortcutManager = get(ShortcutManager.class);
    }

    @Override
    protected void start() {
        this.addSelectionShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                CharacterEditorSetting.SHORTCUT_ADD_SELECTION);
        this.subtractSelectionShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                CharacterEditorSetting.SHORTCUT_SUBTRACT_SELECTION);
    }

    // Update \\

    @Override
    protected void update() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (!characterEditorManager.getMode().groomsHair() || !characterEditorMeshViewSystem.hasActiveDocument()) {
            abandon();
            return;
        }

        if (drawing) {
            updateDraw(rawInput);
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

    // Lets go of whatever the pointer held when the Hair layer closes or loses its surface
    private void abandon() {

        if (drawing)
            characterEditorManager.endHairDraw();

        if (stroking)
            characterEditorManager.endHairStroke();

        this.drawing = false;
        this.stroking = false;
    }

    // Press \\

    private void press(float mouseX, float mouseY) {

        EditorTool tool = characterEditorManager.getHairTool();

        switch (tool) {
            case DRAW -> beginDraw(mouseX, mouseY);
            case PLACE -> place(mouseX, mouseY);
            case PICK -> pick(mouseX, mouseY);
            default -> beginStroke(tool, mouseX, mouseY);
        }
    }

    private void place(float mouseX, float mouseY) {

        int face = characterEditorMeshViewSystem.pickSurface(mouseX, mouseY, hitPoint, hitNormal);

        if (face == EngineSetting.INDEX_NOT_FOUND)
            return;

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
        characterEditorManager.placeHairStrand(hitPoint, hitNormal, face);
    }

    private void pick(float mouseX, float mouseY) {

        int strand = pickStrand(mouseX, mouseY);

        if (strand == EngineSetting.INDEX_NOT_FOUND)
            return;

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
        characterEditorManager.pickHairMaterial(strand);
    }

    // The strand with a shown point nearest the pointer within the pick radius, or none
    private int pickStrand(float mouseX, float mouseY) {

        CharacterGroomStruct groom = characterEditorManager.getGroom();
        float radius = EditorSetting.CHARACTER_VIEW_PICK_RADIUS_PIXELS;
        float nearest = radius * radius;
        int picked = EngineSetting.INDEX_NOT_FOUND;

        if (groom == null)
            return picked;

        for (int s = 0; s < groom.getStrandCount(); s++) {

            CharacterStrandStruct strand = groom.getStrand(s);

            for (int p = 0; p < strand.getPointCount(); p++) {

                if (!projectShown(strand, p))
                    continue;

                float dx = screen.x - mouseX;
                float dy = screen.y - mouseY;
                float distance = dx * dx + dy * dy;

                if (distance <= nearest) {
                    nearest = distance;
                    picked = s;
                }
            }
        }

        return picked;
    }

    // Draw \\

    // A strand rooted on the surface pressed, drawn on across the plane through the root that faces the camera
    private void beginDraw(float mouseX, float mouseY) {

        int face = characterEditorMeshViewSystem.pickSurface(mouseX, mouseY, hitPoint, hitNormal);

        if (face == EngineSetting.INDEX_NOT_FOUND)
            return;

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
        characterEditorManager.beginHairDraw(hitPoint, hitNormal, face);

        if (!characterEditorManager.isDrawingHair())
            return;

        characterEditorMeshViewSystem.resolveRay(mouseX, mouseY, rayOrigin, rayDirection);
        planePoint.set(hitPoint);
        planeNormal.set(rayDirection).multiply(-1f);
        this.drawing = true;
    }

    private void updateDraw(RawInputHandle rawInput) {

        if (!rawInput.isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT)) {
            this.drawing = false;
            characterEditorManager.endHairDraw();
            return;
        }

        characterEditorMeshViewSystem.resolveRay(rawInput.getMouseX(), rawInput.getMouseY(), rayOrigin, rayDirection);
        CharacterPoseUtility.resolvePlaneHit(rayOrigin, rayDirection, planePoint, planeNormal, hitPoint);
        characterEditorManager.extendHairDraw(hitPoint);
    }

    // Stroke \\

    // A stroke begun where the brush reaches a strand; a selecting press reaching none still clears the selection
    private void beginStroke(EditorTool tool, float mouseX, float mouseY) {

        if (!characterEditorManager.getToolCanvas().strokes(tool))
            return;

        CharacterEditorSelection selection = resolveSelection();

        collectUnderBrush(mouseX, mouseY);

        if (strands.isEmpty()) {

            if (tool == EditorTool.SELECT && selection == CharacterEditorSelection.REPLACE
                    && characterEditorManager.hasGroom()) {
                characterEditorManager.beginHairStroke(selection);
                characterEditorManager.endHairStroke();
            }

            return;
        }

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
        characterEditorManager.beginHairStroke(selection);
        this.stroking = true;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        drag.set(0f, 0f, 0f);
        characterEditorManager.strokeHair(strands, points, weights, drag);
    }

    private void updateStroke(RawInputHandle rawInput) {

        if (!rawInput.isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT)) {
            this.stroking = false;
            characterEditorManager.endHairStroke();
            return;
        }

        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (mouseX == lastMouseX && mouseY == lastMouseY)
            return;

        collectUnderBrush(mouseX, mouseY);

        if (!strands.isEmpty()) {
            resolveDrag(mouseX, mouseY);
            characterEditorManager.strokeHair(strands, points, weights, drag);
        }

        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    // How a selecting stroke combines with the selection: Shift adds, Ctrl takes away, a plain stroke replaces it
    private CharacterEditorSelection resolveSelection() {

        if (shortcutManager.isHeld(subtractSelectionShortcutID, context.getWindow()))
            return CharacterEditorSelection.SUBTRACT;

        return shortcutManager.isHeld(addSelectionShortcutID, context.getWindow())
                ? CharacterEditorSelection.ADD
                : CharacterEditorSelection.REPLACE;
    }

    // Every shown point of every strand under the preset's tip, listed strand by strand with the share of the tip it
    // takes at the tool's strength, the points' middle kept for the drag
    private void collectUnderBrush(float mouseX, float mouseY) {

        CharacterGroomStruct groom = characterEditorManager.getGroom();
        BrushHandle brush = brushManager.getActiveBrush();
        float radius = characterEditorManager.getBrushRadius();
        float strength = characterEditorManager.getBrushStrength();
        int count = 0;

        strands.clear();
        points.clear();
        weights.clear();
        centre.set(0f, 0f, 0f);

        if (groom == null)
            return;

        for (int s = 0; s < groom.getStrandCount(); s++) {

            CharacterStrandStruct strand = groom.getStrand(s);

            for (int p = 0; p < strand.getPointCount(); p++) {

                if (!projectShown(strand, p))
                    continue;

                float coverage = BrushUtility.resolveCoverage(brush, screen.x - mouseX, screen.y - mouseY, radius);

                if (coverage <= 0f)
                    continue;

                strands.add(s);
                points.add(p);
                weights.add(coverage * strength);
                centre.add(strand.getX(p), strand.getY(p), strand.getZ(p));
                count++;
            }
        }

        if (count > 0)
            centre.multiply(1f / count);
    }

    // How far the pointer moved since its last frame across the plane through the points under the brush that faces
    // the camera
    private void resolveDrag(float mouseX, float mouseY) {

        characterEditorMeshViewSystem.resolveRay(lastMouseX, lastMouseY, rayOrigin, rayDirection);
        planeNormal.set(rayDirection).multiply(-1f);
        CharacterPoseUtility.resolvePlaneHit(rayOrigin, rayDirection, centre, planeNormal, previousHit);
        characterEditorMeshViewSystem.resolveRay(mouseX, mouseY, rayOrigin, rayDirection);
        CharacterPoseUtility.resolvePlaneHit(rayOrigin, rayDirection, centre, planeNormal, hitPoint);
        drag.set(hitPoint).subtract(previousHit);
    }

    // A strand's point on the window, written to the screen scratch; false behind the camera or hidden by the body
    private boolean projectShown(CharacterStrandStruct strand, int pointIndex) {

        float x = strand.getX(pointIndex);
        float y = strand.getY(pointIndex);
        float z = strand.getZ(pointIndex);

        return characterEditorMeshViewSystem.project(x, y, z, screen)
                && characterEditorMeshViewSystem.isPointShown(x, y, z);
    }
}
