package editor.runtime.charactereditor.rig;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorTransform;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.runtime.EditorSetting;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.runtime.charactereditor.mode.CharacterEditorModeSystem;
import editor.runtime.characterview.gizmo.CharacterGizmoSource;
import editor.runtime.characterview.gizmo.CharacterGizmoSystem;
import editor.runtime.characterview.util.CharacterBonePickUtility;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class CharacterEditorRigSystem extends SystemPackage implements CharacterGizmoSource {

    /*
     * Turns this tab's pointer into rig edits while the Rig mode is open,
     * by the rig tool in use. A click on a joint picks its bone with every
     * tool; Move then drags the joint across the plane facing the camera,
     * its children following, Rotate turns the bone's bind rotation about
     * the line of sight by how far the pointer turns about the joint, its
     * twin turned the mirrored way while mirroring, Scale grows or shrinks
     * the bone's size by how
     * far the pointer travels from the joint, Place stands a new child of
     * the selected bone where it clicks, level with that bone's joint, and
     * Erase removes the bone clicked. A press on a handle of the transform
     * gizmo (CharacterGizmoSystem) drags the selected bone held to that
     * axis: Move along it, Rotate about it, Scale its size along it. A press
     * that does none of these is left to the view camera. A drag is one undo step: letting go
     * keeps it, a right click or Escape puts the rig back, and Ctrl snaps it.
     * Edits are written only on frames the pointer moves. A bind edit never
     * moves the mesh: the skin stays where it is, as a rig tool keeps it.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ViewCameraSystem viewCameraSystem;
    private CharacterEditorManager characterEditorManager;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private CharacterEditorModeSystem characterEditorModeSystem;
    private CharacterGizmoSystem characterGizmoSystem;
    private ShortcutManager shortcutManager;

    // Shortcuts
    private int snapShortcutID;

    // Drag
    private EditorTool dragTool;
    private int dragAxis;
    private Vector3 dragAxisVector;
    private Vector3 joint;
    private Vector2 jointScreen;
    private Vector3 startHit;
    private float startDistance;
    private float startMouseX;
    private float startMouseY;
    private float lastMouseX;
    private float lastMouseY;

    // Scratch
    private FloatArrayList pickScreen;
    private BooleanArrayList pickable;
    private IntArrayList pickParents;
    private Vector2 screenScratch;
    private Vector3 rayOrigin;
    private Vector3 rayDirection;
    private Vector3 hitScratch;
    private Vector3 axisScratch;
    private Matrix4 turnScratch;

    // Base \\

    @Override
    protected void create() {

        // Drag
        this.dragAxis = EngineSetting.INDEX_NOT_FOUND;
        this.dragAxisVector = new Vector3();
        this.joint = new Vector3();
        this.jointScreen = new Vector2();
        this.startHit = new Vector3();

        // Scratch
        this.pickScreen = new FloatArrayList();
        this.pickable = new BooleanArrayList();
        this.pickParents = new IntArrayList();
        this.screenScratch = new Vector2();
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();
        this.hitScratch = new Vector3();
        this.axisScratch = new Vector3();
        this.turnScratch = new Matrix4();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditorMeshViewSystem = get(CharacterEditorMeshViewSystem.class);
        this.characterEditorModeSystem = get(CharacterEditorModeSystem.class);
        this.characterGizmoSystem = get(CharacterGizmoSystem.class);
        this.shortcutManager = get(ShortcutManager.class);
    }

    @Override
    protected void awake() {
        characterGizmoSystem.addSource(this);
    }

    @Override
    protected void start() {
        this.snapShortcutID = shortcutManager.getShortcutIDFromShortcutName(CharacterEditorSetting.SHORTCUT_SNAP);
    }

    // Update \\

    @Override
    protected void update() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (characterEditorManager.getMode() != CharacterEditorMode.RIG
                || characterEditorManager.getRigBoneCount() == 0) {
            abandon();
            return;
        }

        if (dragTool != null) {
            updateDrag(rawInput);
            return;
        }

        if (!editorInputSystem.isPointerActive() || viewCameraSystem.isDragging())
            return;

        if (editorInputSystem.isClicked(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT))
            press(rawInput.getMouseX(), rawInput.getMouseY());
    }

    // A drag still held when the mode closes keeps what it did
    private void abandon() {

        if (dragTool != null)
            characterEditorManager.endRigGesture();

        this.dragTool = null;
    }

    // Press \\

    // Every tool works on the joint pressed, Place on the space beside one
    private void press(float mouseX, float mouseY) {

        EditorTool tool = characterEditorManager.getRigTool();
        int gizmoAxis = characterGizmoSystem.getHoveredAxis();

        if (gizmoAxis != EngineSetting.INDEX_NOT_FOUND && getGizmoTransform() != null) {
            editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
            startDrag(tool, gizmoAxis, mouseX, mouseY);
            return;
        }

        int picked = pickJoint(mouseX, mouseY);

        if (picked == EngineSetting.INDEX_NOT_FOUND) {

            if (tool == EditorTool.PLACE)
                placeBone(mouseX, mouseY);

            return;
        }

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);

        if (tool == EditorTool.ERASE) {
            characterEditorManager.removeRigBone(picked);
            return;
        }

        characterEditorManager.selectRigBone(picked);

        if (CharacterEditorTransform.forTool(tool) != null)
            startDrag(tool, EngineSetting.INDEX_NOT_FOUND, mouseX, mouseY);
    }

    // The tab region's bone the pointer is over, at its joint or along the segment drawn from it
    // (CharacterBonePickUtility), or none
    private int pickJoint(float mouseX, float mouseY) {

        int boneCount = characterEditorManager.getRigBoneCount();

        CharacterBonePickUtility.reset(pickScreen, pickable, pickParents, boneCount);

        for (int i = 0; i < boneCount; i++) {

            pickParents.set(i, characterEditorManager.getRigBoneParent(i));
            CharacterPoseUtility.getTranslation(characterEditorManager.getRigBoneWorldMatrix(i), joint);

            if (!characterEditorMeshViewSystem.project(joint.x, joint.y, joint.z, screenScratch))
                continue;

            pickScreen.set(i * 2, screenScratch.x);
            pickScreen.set(i * 2 + 1, screenScratch.y);
            pickable.set(i, characterEditorManager.isRigBoneInRegion(characterEditorModeSystem.getRegion(), i));
        }

        return CharacterBonePickUtility.pick(pickScreen, pickable, pickParents, mouseX, mouseY);
    }

    // A new child of the selected bone where the pointer meets the plane facing the camera through its joint
    private void placeBone(float mouseX, float mouseY) {

        int selected = characterEditorManager.getSelectedRigBoneIndex();

        if (selected == EngineSetting.INDEX_NOT_FOUND)
            return;

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
        CharacterPoseUtility.getTranslation(characterEditorManager.getRigBoneWorldMatrix(selected), joint);
        resolveHit(mouseX, mouseY, hitScratch);
        characterEditorManager.addRigBone(hitScratch);
    }

    // Drag \\

    // A drag of the selected bone by the tool, free or held to an axis
    private void startDrag(EditorTool tool, int axis, float mouseX, float mouseY) {

        if (!characterEditorManager.beginRigGesture())
            return;

        int selected = characterEditorManager.getSelectedRigBoneIndex();

        CharacterPoseUtility.getTranslation(characterEditorManager.getRigBoneWorldMatrix(selected), joint);

        if (!characterEditorMeshViewSystem.project(joint.x, joint.y, joint.z, jointScreen))
            jointScreen.set(mouseX, mouseY);

        resolveHit(mouseX, mouseY, startHit);

        this.dragTool = tool;
        this.dragAxis = axis;
        dragAxisVector.set(
                axis == EngineSetting.AXIS_X ? 1f : 0f,
                axis == EngineSetting.AXIS_Y ? 1f : 0f,
                axis == EngineSetting.AXIS_Z ? 1f : 0f);
        this.startDistance = Math.max(
                EditorSetting.CHARACTER_VIEW_SCALE_MIN_PIXELS,
                (float) Math.hypot(mouseX - jointScreen.x, mouseY - jointScreen.y));
        this.startMouseX = mouseX;
        this.startMouseY = mouseY;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    // A right click or Escape puts the rig back, letting go keeps the drag, and a moved pointer carries it on
    private void updateDrag(RawInputHandle rawInput) {

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)
                || rawInput.isButtonClicked(EditorSetting.CHARACTER_VIEW_BUTTON_CANCEL)) {
            editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_CANCEL);
            characterEditorManager.cancelRigGesture();
            this.dragTool = null;
            return;
        }

        if (!rawInput.isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT)) {
            characterEditorManager.endRigGesture();
            this.dragTool = null;
            return;
        }

        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (mouseX == lastMouseX && mouseY == lastMouseY)
            return;

        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        boolean snap = shortcutManager.isHeld(snapShortcutID, context.getWindow());

        switch (dragTool) {
            case MOVE -> applyMove(mouseX, mouseY, snap);
            case ROTATE -> applyRotate(mouseX, mouseY, snap);
            default -> applyScale(mouseX, mouseY, snap);
        }
    }

    // The pointer's travel across the plane facing the camera, or its share along the axis held
    private void applyMove(float mouseX, float mouseY, boolean snap) {

        resolveHit(mouseX, mouseY, hitScratch);
        hitScratch.subtract(startHit);

        if (dragAxis != EngineSetting.INDEX_NOT_FOUND) {

            float along = hitScratch.x * dragAxisVector.x + hitScratch.y * dragAxisVector.y
                    + hitScratch.z * dragAxisVector.z;

            hitScratch.set(dragAxisVector.x * along, dragAxisVector.y * along, dragAxisVector.z * along);
        }

        hitScratch.set(snapValue(hitScratch.x, snap), snapValue(hitScratch.y, snap), snapValue(hitScratch.z, snap));
        characterEditorManager.moveRigJoint(hitScratch);
    }

    // The pointer's turn about the joint on screen, a turn about the line of sight or the axis held, facing the viewer
    private void applyRotate(float mouseX, float mouseY, boolean snap) {

        float angle = (float) (Math.atan2(mouseY - jointScreen.y, mouseX - jointScreen.x)
                - Math.atan2(startMouseY - jointScreen.y, startMouseX - jointScreen.x));
        Vector3 view = context.getWindow().getActiveCamera().getDirection();

        if (snap)
            angle = (float) Math.toRadians(Math.round(Math.toDegrees(angle) / EditorSetting.CHARACTER_VIEW_SNAP_DEGREES)
                    * EditorSetting.CHARACTER_VIEW_SNAP_DEGREES);

        if (dragAxis == EngineSetting.INDEX_NOT_FOUND)
            axisScratch.set(-view.x, -view.y, -view.z).normalize();
        else {
            axisScratch.set(dragAxisVector);

            if (axisScratch.x * view.x + axisScratch.y * view.y + axisScratch.z * view.z > 0f)
                angle = -angle;
        }

        CharacterPoseUtility.toAxisRotation(axisScratch, angle, turnScratch);
        characterEditorManager.rotateRigBone(turnScratch);
    }

    private void applyScale(float mouseX, float mouseY, boolean snap) {

        float factor = (float) Math.hypot(mouseX - jointScreen.x, mouseY - jointScreen.y) / startDistance;

        if (snap)
            factor = Math.round(factor / EditorSetting.CHARACTER_VIEW_SNAP_SCALE)
                    * EditorSetting.CHARACTER_VIEW_SNAP_SCALE;

        characterEditorManager.scaleRigBone(
                dragAxis == EngineSetting.INDEX_NOT_FOUND || dragAxis == EngineSetting.AXIS_X ? factor : 1f,
                dragAxis == EngineSetting.INDEX_NOT_FOUND || dragAxis == EngineSetting.AXIS_Y ? factor : 1f,
                dragAxis == EngineSetting.INDEX_NOT_FOUND || dragAxis == EngineSetting.AXIS_Z ? factor : 1f);
    }

    // Gizmo \\

    // The transform the Rig tab's tool drags on the selected bone while no drag is held; null otherwise
    @Override
    public CharacterEditorTransform getGizmoTransform() {
        return characterEditorManager.getMode() == CharacterEditorMode.RIG && dragTool == null
                && characterEditorManager.getSelectedRigBoneIndex() != EngineSetting.INDEX_NOT_FOUND
                ? CharacterEditorTransform.forTool(characterEditorManager.getRigTool())
                : null;
    }

    @Override
    public boolean resolveGizmoPivot(Vector3 out) {
        CharacterPoseUtility.getTranslation(
                characterEditorManager.getRigBoneWorldMatrix(characterEditorManager.getSelectedRigBoneIndex()), out);
        return true;
    }

    // Utility \\

    // Where a window pixel's ray meets the plane facing the camera through the held joint
    private void resolveHit(float mouseX, float mouseY, Vector3 out) {

        characterEditorMeshViewSystem.resolveRay(mouseX, mouseY, rayOrigin, rayDirection);
        CharacterPoseUtility.resolvePlaneHit(
                rayOrigin,
                rayDirection,
                joint,
                context.getWindow().getActiveCamera().getDirection(),
                out);
    }

    private float snapValue(float value, boolean snap) {

        float step = characterEditorManager.getSnapStep();

        if (!snap || step <= 0f)
            return value;

        return Math.round(value / step) * step;
    }
}
