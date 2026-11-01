package editor.runtime.animationeditor.pose;

import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.geometrypipeline.rig.RigBoneStruct;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.animationeditorpipeline.animationeditormanager.AnimationEditorManager;
import editor.bootstrap.animationeditorpipeline.util.AnimationEditorChannel;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.runtime.animationeditor.AnimationEditorSetting;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class AnimationEditorPoseSystem extends SystemPackage {

    /*
     * Turns this tab's pointer into keys while a clip plays on a character,
     * by the pose tool in use. A click on a joint picks its bone with every
     * tool; Rotate, Move and Scale then drag the selected bone from wherever
     * the press lands, keying it at the playhead as the pointer moves. A turn
     * follows the pointer about the joint on screen, about the line of sight
     * or the world axis X, Y or Z holds it to, and is carried into the bone's
     * own Euler rotation exactly, the nearest form to the one it replaces; a
     * move carries the joint across the plane facing the camera, or along a
     * held axis, into its parent's space; a scale grows the bone by how far
     * the pointer travels from the joint, on every axis or the one held.
     * Ctrl snaps. A drag is one undo step: letting go keeps it, a right
     * click or Escape puts the clip back.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private AnimationEditorManager animationEditorManager;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private ShortcutManager shortcutManager;

    // Shortcuts
    private int snapShortcutID;

    // Drag
    private EditorTool dragTool;
    private int axisLock;
    private float[] pose;
    private Vector3 joint;
    private Vector2 jointScreen;
    private Vector3 startHit;
    private Vector3 startRotation;
    private Vector3 bindRotation;
    private Vector3 parentProportion;
    private Matrix4 parentRotation;
    private Matrix4 parentInverse;
    private Matrix4 startLocal;
    private float startDistance;
    private float startMouseX;
    private float startMouseY;
    private float lastMouseX;
    private float lastMouseY;

    // Scratch
    private Vector2 screenScratch;
    private Vector3 rayOrigin;
    private Vector3 rayDirection;
    private Vector3 axis;
    private Vector3 hitScratch;
    private Vector3 eulerScratch;
    private Matrix4 turn;
    private Matrix4 rotated;
    private Matrix4 matrixScratch;

    // Base \\

    @Override
    protected void create() {

        // Drag
        this.axisLock = EngineSetting.INDEX_NOT_FOUND;
        this.pose = new float[AnimationEditorChannel.getPoseLength()];
        this.joint = new Vector3();
        this.jointScreen = new Vector2();
        this.startHit = new Vector3();
        this.startRotation = new Vector3();
        this.bindRotation = new Vector3();
        this.parentProportion = new Vector3();
        this.parentRotation = new Matrix4();
        this.parentInverse = new Matrix4();
        this.startLocal = new Matrix4();

        // Scratch
        this.screenScratch = new Vector2();
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();
        this.axis = new Vector3();
        this.hitScratch = new Vector3();
        this.eulerScratch = new Vector3();
        this.turn = new Matrix4();
        this.rotated = new Matrix4();
        this.matrixScratch = new Matrix4();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.animationEditorManager = get(AnimationEditorManager.class);
        this.characterViewPoseSystem = get(CharacterViewPoseSystem.class);
        this.shortcutManager = get(ShortcutManager.class);
    }

    @Override
    protected void start() {
        this.snapShortcutID = shortcutManager.getShortcutIDFromShortcutName(AnimationEditorSetting.SHORTCUT_SNAP);
    }

    // Update \\

    @Override
    protected void update() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (animationEditorManager.getToolCanvas() == null || !characterViewPoseSystem.hasSubject()) {
            abandon();
            return;
        }

        if (dragTool != null) {
            updateDrag(rawInput);
            return;
        }

        if (!editorInputSystem.isPointerActive() || rawInput.isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_ORBIT))
            return;

        if (editorInputSystem.isClicked(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT))
            press(rawInput.getMouseX(), rawInput.getMouseY());
    }

    // A drag still held when the clip or its character goes keeps what it did
    private void abandon() {

        if (dragTool != null)
            animationEditorManager.endPose();

        this.dragTool = null;
    }

    // Press \\

    private void press(float mouseX, float mouseY) {

        EditorTool tool = animationEditorManager.getPoseTool();
        int picked = pickJoint(mouseX, mouseY);

        if (picked != EngineSetting.INDEX_NOT_FOUND)
            animationEditorManager.selectBone(picked);

        if (tool != EditorTool.SELECT)
            startDrag(tool, mouseX, mouseY);
    }

    // The bone whose posed joint lies nearest the pointer within the pick radius, or none
    private int pickJoint(float mouseX, float mouseY) {

        AnimationStateHandle state = characterViewPoseSystem.getAnimationStateHandle();
        WindowInstance window = context.getWindow();
        float radius = EditorSetting.CHARACTER_VIEW_PICK_RADIUS_PIXELS;
        float nearest = radius * radius;
        int picked = EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < state.getRigHandle().getBoneCount(); i++) {

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

    // Holds the selected bone as it stands: its joint, its parent's frame and its own turn, all from this frame's pose
    private void startDrag(EditorTool tool, float mouseX, float mouseY) {

        if (!animationEditorManager.beginPose())
            return;

        AnimationStateHandle state = characterViewPoseSystem.getAnimationStateHandle();
        int boneIndex = animationEditorManager.getSelectedBoneIndex();
        RigBoneStruct bone = state.getRigHandle().getBone(boneIndex);

        System.arraycopy(animationEditorManager.getPoseStart(), 0, pose, 0, pose.length);
        CharacterPoseUtility.getTranslation(state.getWorldMatrix(boneIndex), joint);

        if (!CharacterPoseUtility.project(context.getWindow(), joint.x, joint.y, joint.z, jointScreen))
            jointScreen.set(mouseX, mouseY);

        holdParent(state, bone);

        bindRotation.set(bone.getRotation());
        startRotation.set(
                bindRotation.x + pose[AnimationEditorChannel.ROTATION.toPoseIndex(0)],
                bindRotation.y + pose[AnimationEditorChannel.ROTATION.toPoseIndex(1)],
                bindRotation.z + pose[AnimationEditorChannel.ROTATION.toPoseIndex(2)]);
        CharacterPoseUtility.toRotation(startRotation, startLocal);
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

    // A right click or Escape puts the clip back, letting go keeps the drag, and a moved pointer or axis carries it on
    private void updateDrag(RawInputHandle rawInput) {

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)
                || rawInput.isButtonClicked(EditorSetting.CHARACTER_VIEW_BUTTON_CANCEL)) {
            animationEditorManager.cancelPose();
            this.dragTool = null;
            return;
        }

        if (!rawInput.isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT)) {
            animationEditorManager.endPose();
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
            case ROTATE -> applyRotate(mouseX, mouseY, snap);
            case MOVE -> applyMove(mouseX, mouseY, snap);
            case SCALE -> applyScale(mouseX, mouseY, snap);
            default -> {
                return;
            }
        }

        animationEditorManager.writePose(pose);
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

    // The pointer's turn about the joint on screen, carried into the bone's rotation under its parent
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

        CharacterPoseUtility.toAxisRotation(axis, angle, turn);
        CharacterPoseUtility.applyWorldTurn(parentRotation, turn, startLocal, rotated, matrixScratch);
        CharacterPoseUtility.toEuler(rotated, startRotation, eulerScratch);
        eulerScratch.subtract(bindRotation);
        writeChannel(AnimationEditorChannel.ROTATION, eulerScratch);
    }

    // The joint carried across the plane facing the camera, or along the held axis, into the parent's space
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

        float[] start = animationEditorManager.getPoseStart();

        eulerScratch.set(
                snapPosition(start[AnimationEditorChannel.POSITION.toPoseIndex(0)] + eulerScratch.x, snap),
                snapPosition(start[AnimationEditorChannel.POSITION.toPoseIndex(1)] + eulerScratch.y, snap),
                snapPosition(start[AnimationEditorChannel.POSITION.toPoseIndex(2)] + eulerScratch.z, snap));
        writeChannel(AnimationEditorChannel.POSITION, eulerScratch);
    }

    // The bone grown by the pointer's distance from the joint against where it started, on one axis or all
    private void applyScale(float mouseX, float mouseY, boolean snap) {

        float factor = (float) Math.hypot(mouseX - jointScreen.x, mouseY - jointScreen.y) / startDistance;
        float[] start = animationEditorManager.getPoseStart();

        if (snap)
            factor = Math.round(factor / EditorSetting.CHARACTER_VIEW_SNAP_SCALE)
                    * EditorSetting.CHARACTER_VIEW_SNAP_SCALE;

        for (int i = 0; i < EditorSetting.CHARACTER_VIEW_AXIS_NAMES.length; i++) {

            int index = AnimationEditorChannel.SCALE.toPoseIndex(i);
            float scaled = axisLock == EngineSetting.INDEX_NOT_FOUND || axisLock == i ? start[index] * factor
                    : start[index];

            pose[index] = Math.max(EditorSetting.ANIMATION_EDITOR_SCALE_MIN, scaled);
        }
    }

    // Utility \\

    private void writeChannel(AnimationEditorChannel channel, Vector3 value) {
        pose[channel.toPoseIndex(0)] = value.x;
        pose[channel.toPoseIndex(1)] = value.y;
        pose[channel.toPoseIndex(2)] = value.z;
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

    private float snapPosition(float value, boolean snap) {

        float step = EditorSetting.ANIMATION_EDITOR_POSITION_STEP;

        return snap ? Math.round(value / step) * step : value;
    }
}
