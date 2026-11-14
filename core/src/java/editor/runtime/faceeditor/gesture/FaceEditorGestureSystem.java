package editor.runtime.faceeditor.gesture;

import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.geometrypipeline.rig.RigBoneStruct;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.faceeditorpipeline.faceeditormanager.FaceEditorManager;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.charactereditor.mode.CharacterEditorModeSystem;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.viewcamera.ViewCameraSystem;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class FaceEditorGestureSystem extends SystemPackage {

    /*
     * Turns the Character Editor window's pointer into face shaping while its
     * Face layer is worked on, by the face tool in use. A click on a face
     * joint picks its bone with every tool; Move, Rotate and Scale then drag
     * it, and the picked control's target on that bone is shaped so the bone
     * stands where the drag puts it at the control's preview value: the move,
     * turn and growth the drag makes, divided by that value. A press beside
     * every face joint is left to the view camera. A move carries the joint
     * across the plane facing the camera, or along a held world axis, into
     * its parent's space; a turn follows the pointer about the joint on
     * screen, about the line of sight or the held axis, carried into the
     * bone's own Euler rotation; a scale grows the bone by how far the
     * pointer travels from the joint, on every axis or the one held. Ctrl
     * snaps. A drag is one undo step: letting go keeps it, a right click or
     * Escape puts the face back.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ViewCameraSystem viewCameraSystem;
    private FaceEditorManager faceEditorManager;
    private CharacterEditorModeSystem characterEditorModeSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private ShortcutManager shortcutManager;

    // Shortcuts
    private int snapShortcutID;

    // Drag
    private EditorTool dragTool;
    private int axisLock;
    private float value;
    private Vector3 joint;
    private Vector2 jointScreen;
    private Vector3 startHit;
    private Vector3 startRotation;
    private Vector3 bindRotation;
    private Vector3 startMove;
    private Vector3 startTurn;
    private Vector3 startScale;
    private Vector3 parentProportion;
    private Matrix4 parentRotation;
    private Matrix4 parentInverse;
    private Matrix4 startLocal;
    private float startDistance;
    private float startMouseX;
    private float startMouseY;
    private float lastMouseX;
    private float lastMouseY;

    // Target — what the drag writes each time the pointer moves
    private Vector3 move;
    private Vector3 turn;
    private Vector3 scale;

    // Scratch
    private Vector2 screenScratch;
    private Vector3 rayOrigin;
    private Vector3 rayDirection;
    private Vector3 axis;
    private Vector3 hitScratch;
    private Vector3 eulerScratch;
    private Matrix4 turnScratch;
    private Matrix4 rotated;
    private Matrix4 matrixScratch;
    private Matrix4 bindScratch;

    // Base \\

    @Override
    protected void create() {

        // Drag
        this.axisLock = EngineSetting.INDEX_NOT_FOUND;
        this.joint = new Vector3();
        this.jointScreen = new Vector2();
        this.startHit = new Vector3();
        this.startRotation = new Vector3();
        this.bindRotation = new Vector3();
        this.startMove = new Vector3();
        this.startTurn = new Vector3();
        this.startScale = new Vector3();
        this.parentProportion = new Vector3();
        this.parentRotation = new Matrix4();
        this.parentInverse = new Matrix4();
        this.startLocal = new Matrix4();

        // Target
        this.move = new Vector3();
        this.turn = new Vector3();
        this.scale = new Vector3();

        // Scratch
        this.screenScratch = new Vector2();
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();
        this.axis = new Vector3();
        this.hitScratch = new Vector3();
        this.eulerScratch = new Vector3();
        this.turnScratch = new Matrix4();
        this.rotated = new Matrix4();
        this.matrixScratch = new Matrix4();
        this.bindScratch = new Matrix4();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
        this.faceEditorManager = get(FaceEditorManager.class);
        this.characterEditorModeSystem = get(CharacterEditorModeSystem.class);
        this.characterViewPoseSystem = get(CharacterViewPoseSystem.class);
        this.shortcutManager = get(ShortcutManager.class);
    }

    @Override
    protected void start() {
        this.snapShortcutID = shortcutManager.getShortcutIDFromShortcutName(CharacterEditorSetting.SHORTCUT_SNAP);
    }

    // Update \\

    @Override
    protected void update() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (!characterEditorModeSystem.getMode().shapesFace() || faceEditorManager.getToolCanvas() == null
                || !characterViewPoseSystem.hasSubject()) {
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

    // A drag still held when the character goes keeps what it did
    private void abandon() {

        if (dragTool != null)
            faceEditorManager.endGesture();

        this.dragTool = null;
    }

    // Press \\

    private void press(float mouseX, float mouseY) {

        EditorTool tool = faceEditorManager.getFaceTool();
        int picked = pickJoint(mouseX, mouseY);

        if (picked == EngineSetting.INDEX_NOT_FOUND)
            return;

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
        faceEditorManager.selectBone(picked);

        if (tool != EditorTool.SELECT)
            startDrag(tool, mouseX, mouseY);
    }

    // The face bone whose posed joint lies nearest the pointer within the pick radius, or none
    private int pickJoint(float mouseX, float mouseY) {

        AnimationStateHandle state = characterViewPoseSystem.getAnimationStateHandle();
        WindowInstance window = context.getWindow();
        float radius = EditorSetting.CHARACTER_VIEW_PICK_RADIUS_PIXELS;
        float nearest = radius * radius;
        int picked = EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < state.getRigHandle().getBoneCount(); i++) {

            if (!faceEditorManager.isFaceBone(i))
                continue;

            CharacterPoseUtility.getTranslation(state.getWorldMatrix(i), hitScratch);

            if (!CharacterPoseUtility.project(window, hitScratch.x, hitScratch.y, hitScratch.z, screenScratch))
                continue;

            float dx = screenScratch.x - mouseX;
            float dy = screenScratch.y - mouseY;
            float distance = dx * dx + dy * dy;

            if (distance <= nearest) {
                nearest = distance;
                picked = i;
            }
        }

        return picked;
    }

    // Drag \\

    // Holds the picked bone as it stands and the control's target on it; a control the drag takes from near nothing
    // to its far end turns the bone by its own target the more, so the turn the drag starts from counts that in
    private void startDrag(EditorTool tool, float mouseX, float mouseY) {

        float valueBefore = faceEditorManager.getPreviewValue();

        if (!faceEditorManager.beginGesture())
            return;

        AnimationStateHandle state = characterViewPoseSystem.getAnimationStateHandle();
        int boneIndex = faceEditorManager.getSelectedBoneIndex();
        RigBoneStruct bone = state.getRigHandle().getBone(boneIndex);

        this.value = faceEditorManager.getPreviewValue();
        startMove.set(faceEditorManager.getGestureMove());
        startTurn.set(faceEditorManager.getGestureTurn());
        startScale.set(faceEditorManager.getGestureScale());
        move.set(startMove);
        turn.set(startTurn);
        scale.set(startScale);

        CharacterPoseUtility.getTranslation(state.getWorldMatrix(boneIndex), joint);

        if (!CharacterPoseUtility.project(context.getWindow(), joint.x, joint.y, joint.z, jointScreen))
            jointScreen.set(mouseX, mouseY);

        holdParent(state, bone);

        float jump = value - valueBefore;

        bindRotation.set(bone.getRotation());
        startRotation.set(state.getBoneShapeRotation(boneIndex))
                .add(startTurn.x * jump, startTurn.y * jump, startTurn.z * jump);
        CharacterPoseUtility.composeBindTurn(bindRotation, startRotation, startLocal, matrixScratch);
        resolveHit(mouseX, mouseY, startHit);

        this.dragTool = tool;
        this.axisLock = EngineSetting.INDEX_NOT_FOUND;
        this.startMouseX = mouseX;
        this.startMouseY = mouseY;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.startDistance = Math.max(
                EditorSetting.CHARACTER_VIEW_SCALE_MIN_PIXELS,
                (float) Math.hypot(mouseX - jointScreen.x, mouseY - jointScreen.y));
    }

    // The parent's turn and its full inverse in the world, and its proportions, or none of them for the root
    private void holdParent(AnimationStateHandle state, RigBoneStruct bone) {

        if (bone.isRoot()) {
            parentRotation.set(1f);
            parentInverse.set(1f);
            parentProportion.set(1f, 1f, 1f);
            return;
        }

        Matrix4 parentWorld = state.getWorldMatrix(bone.getParentIndex());

        CharacterPoseUtility.extractRotation(parentWorld, parentRotation);
        parentInverse.set(parentWorld).inverse();
        parentProportion.set(state.getBoneProportion(bone.getParentIndex()));
    }

    // A right click or Escape puts the face back, letting go keeps the drag, and a moved pointer or axis carries it on
    private void updateDrag(RawInputHandle rawInput) {

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)
                || rawInput.isButtonClicked(EditorSetting.CHARACTER_VIEW_BUTTON_CANCEL)) {
            editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_CANCEL);
            faceEditorManager.cancelGesture();
            this.dragTool = null;
            return;
        }

        if (!rawInput.isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT)) {
            faceEditorManager.endGesture();
            this.dragTool = null;
            return;
        }

        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (!lockAxis(rawInput) && mouseX == lastMouseX && mouseY == lastMouseY)
            return;

        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        boolean snap = shortcutManager.isHeld(snapShortcutID, context.getWindow());

        switch (dragTool) {
            case MOVE -> applyMove(mouseX, mouseY, snap);
            case ROTATE -> applyRotate(mouseX, mouseY, snap);
            case SCALE -> applyScale(mouseX, mouseY, snap);
            default -> {
                return;
            }
        }

        faceEditorManager.writeGesture(move, turn, scale);
    }

    // X, Y or Z holds the drag to that world axis, the same key again frees it; true when the hold changed
    private boolean lockAxis(RawInputHandle rawInput) {

        boolean changed = false;

        for (int key = 0; key < EditorSetting.CHARACTER_VIEW_KEY_AXES.length; key++)
            if (rawInput.isKeyClicked(EditorSetting.CHARACTER_VIEW_KEY_AXES[key])) {
                this.axisLock = axisLock == key ? EngineSetting.INDEX_NOT_FOUND : key;
                changed = true;
            }

        return changed;
    }

    // The joint carried across the plane facing the camera, or along the held axis, into the parent's space, and the
    // move per unit of the control that puts it there
    private void applyMove(float mouseX, float mouseY, boolean snap) {

        resolveHit(mouseX, mouseY, hitScratch);
        hitScratch.subtract(startHit);

        if (axisLock != EngineSetting.INDEX_NOT_FOUND) {
            setAxis(axisLock);
            float along = hitScratch.x * axis.x + hitScratch.y * axis.y + hitScratch.z * axis.z;
            hitScratch.set(axis).multiply(along);
        }

        CharacterPoseUtility.transformDirection(parentInverse, hitScratch.x, hitScratch.y, hitScratch.z,
                eulerScratch);
        eulerScratch.divide(parentProportion);

        move.set(
                startMove.x + snapStep(eulerScratch.x, EditorSetting.FACE_EDITOR_MOVE_STEP, snap) / value,
                startMove.y + snapStep(eulerScratch.y, EditorSetting.FACE_EDITOR_MOVE_STEP, snap) / value,
                startMove.z + snapStep(eulerScratch.z, EditorSetting.FACE_EDITOR_MOVE_STEP, snap) / value);
    }

    // The pointer's turn about the joint on screen, carried into the bone's rotation under its parent, and the turn
    // per unit of the control that gives it
    private void applyRotate(float mouseX, float mouseY, boolean snap) {

        float angle = (float) (Math.atan2(mouseY - jointScreen.y, mouseX - jointScreen.x)
                - Math.atan2(startMouseY - jointScreen.y, startMouseX - jointScreen.x));
        Vector3 view = context.getWindow().getActiveCamera().getDirection();

        if (axisLock != EngineSetting.INDEX_NOT_FOUND) {
            setAxis(axisLock);

            if (axis.x * view.x + axis.y * view.y + axis.z * view.z > 0f)
                angle = -angle;
        } else
            axis.set(-view.x, -view.y, -view.z).normalize();

        if (snap)
            angle = (float) Math.toRadians(Math.round(Math.toDegrees(angle) / EditorSetting.CHARACTER_VIEW_SNAP_DEGREES)
                    * EditorSetting.CHARACTER_VIEW_SNAP_DEGREES);

        CharacterPoseUtility.toAxisRotation(axis, angle, turnScratch);
        CharacterPoseUtility.applyWorldTurn(parentRotation, turnScratch, startLocal, rotated, matrixScratch);
        CharacterPoseUtility.toKeyEuler(rotated, bindRotation, startRotation, eulerScratch, matrixScratch, bindScratch);
        eulerScratch.subtract(startRotation);

        turn.set(
                startTurn.x + eulerScratch.x / value,
                startTurn.y + eulerScratch.y / value,
                startTurn.z + eulerScratch.z / value);
    }

    // The bone grown by the pointer's distance from the joint against where it started, on one axis or all, and the
    // scale per unit of the control that grows it so far from where it stood
    private void applyScale(float mouseX, float mouseY, boolean snap) {

        float factor = (float) Math.hypot(mouseX - jointScreen.x, mouseY - jointScreen.y) / startDistance;

        if (snap)
            factor = Math.round(factor / EditorSetting.CHARACTER_VIEW_SNAP_SCALE)
                    * EditorSetting.CHARACTER_VIEW_SNAP_SCALE;

        scale.set(
                scaleAxis(EngineSetting.AXIS_X, startScale.x, factor),
                scaleAxis(EngineSetting.AXIS_Y, startScale.y, factor),
                scaleAxis(EngineSetting.AXIS_Z, startScale.z, factor));
    }

    private float scaleAxis(int axisIndex, float start, float factor) {

        if (axisLock != EngineSetting.INDEX_NOT_FOUND && axisLock != axisIndex)
            return start;

        float stood = Math.max(EngineSetting.APPEARANCE_BONE_SCALE_MIN, 1f + start * value);

        return start + stood * (factor - 1f) / value;
    }

    // Utility \\

    private float snapStep(float amount, float step, boolean snap) {
        return snap ? Math.round(amount / step) * step : amount;
    }

    private void setAxis(int axisIndex) {
        axis.set(axisIndex == 0 ? 1f : 0f, axisIndex == 1 ? 1f : 0f, axisIndex == 2 ? 1f : 0f);
    }

    // Where a window pixel's ray meets the plane facing the camera through the held joint
    private void resolveHit(float mouseX, float mouseY, Vector3 out) {

        WindowInstance window = context.getWindow();

        CharacterPoseUtility.resolveRay(window, mouseX, mouseY, rayOrigin, rayDirection);
        CharacterPoseUtility.resolvePlaneHit(
                rayOrigin,
                rayDirection,
                joint,
                window.getActiveCamera().getDirection(),
                out);
    }
}
