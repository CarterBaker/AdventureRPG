package editor.runtime.animationeditor.pose;

import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.geometrypipeline.rig.RigBoneStruct;
import application.bootstrap.geometrypipeline.rig.RigIkChainStruct;
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
import editor.runtime.viewcamera.ViewCameraSystem;
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
     * tool; Rotate, Move and Scale then drag it, keying it at the playhead as
     * the pointer moves. A press beside every joint is left to the view
     * camera. A turn
     * follows the pointer about the joint on screen, about the line of sight
     * or the world axis X, Y or Z holds it to, and is carried into the bone's
     * own Euler rotation exactly, the nearest form to the one it replaces; a
     * move carries the joint across the plane facing the camera, or along a
     * held axis, into its parent's space; a scale grows the bone by how far
     * the pointer travels from the joint, on every axis or the one held.
     * Ctrl snaps. With IK Handle only the ends of the rig's IK handles are
     * picked: the end follows the pointer across the plane facing the camera
     * and the root and middle bones turn to reach it, the middle joint bent
     * toward the handle's pole, both keyed together. A drag is one undo
     * step: letting go keeps it, a right click or Escape puts the clip back.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ViewCameraSystem viewCameraSystem;
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

    // IK — the handle dragged and its chain as it stood when the drag began
    private RigIkChainStruct ikChain;
    private float[] middlePose;
    private Vector3 ikRoot;
    private Vector3 ikMiddle;
    private Vector3 ikEnd;
    private Vector3 middleStartRotation;
    private Vector3 middleBindRotation;
    private Matrix4 rootWorldRotation;
    private Matrix4 middleStartLocal;

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
    private Matrix4 bindScratch;
    private Matrix4 rootTurn;
    private Matrix4 middleTurn;
    private Vector3 reached;
    private Vector3 bend;
    private Vector3 vectorA;
    private Vector3 vectorB;

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

        // IK
        this.middlePose = new float[AnimationEditorChannel.getPoseLength()];
        this.ikRoot = new Vector3();
        this.ikMiddle = new Vector3();
        this.ikEnd = new Vector3();
        this.middleStartRotation = new Vector3();
        this.middleBindRotation = new Vector3();
        this.rootWorldRotation = new Matrix4();
        this.middleStartLocal = new Matrix4();

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
        this.bindScratch = new Matrix4();
        this.rootTurn = new Matrix4();
        this.middleTurn = new Matrix4();
        this.reached = new Vector3();
        this.bend = new Vector3();
        this.vectorA = new Vector3();
        this.vectorB = new Vector3();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
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

        if (!editorInputSystem.isPointerActive() || viewCameraSystem.isDragging())
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
        int picked = pickJoint(mouseX, mouseY, tool == EditorTool.IK);

        if (picked == EngineSetting.INDEX_NOT_FOUND)
            return;

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
        animationEditorManager.selectBone(picked);

        if (tool == EditorTool.IK)
            startIkDrag(picked, mouseX, mouseY);
        else if (tool != EditorTool.SELECT)
            startDrag(tool, mouseX, mouseY);
    }

    // The bone whose posed joint lies nearest the pointer within the pick radius, or none; never a bone its springs
    // simulate, and only handle ends for IK
    private int pickJoint(float mouseX, float mouseY, boolean handleEnds) {

        AnimationStateHandle state = characterViewPoseSystem.getAnimationStateHandle();
        WindowInstance window = context.getWindow();
        float radius = EditorSetting.CHARACTER_VIEW_PICK_RADIUS_PIXELS;
        float nearest = radius * radius;
        int picked = EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < state.getRigHandle().getBoneCount(); i++) {

            if (state.getRigHandle().isSpringBone(i)
                    || handleEnds && state.getRigHandle().findIkChainByEnd(i) == null)
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
        readRotation(pose, startRotation);
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

    // A right click or Escape puts the clip back, letting go keeps the drag, and a moved pointer or axis carries it on
    private void updateDrag(RawInputHandle rawInput) {

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)
                || rawInput.isButtonClicked(EditorSetting.CHARACTER_VIEW_BUTTON_CANCEL)) {
            editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_CANCEL);
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
            case IK -> {
                applyIk(mouseX, mouseY);
                animationEditorManager.writeChainPose(ikChain, pose, middlePose);
                return;
            }
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
        CharacterPoseUtility.toKeyEuler(rotated, bindRotation, startRotation, eulerScratch, matrixScratch, bindScratch);
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

    // IK \\

    // Holds a handle's chain as it stands: its three joints, the root's frame and both turning bones' own turns
    private void startIkDrag(int endBone, float mouseX, float mouseY) {

        AnimationStateHandle state = characterViewPoseSystem.getAnimationStateHandle();
        RigIkChainStruct chain = state.getRigHandle().findIkChainByEnd(endBone);

        if (chain == null || !animationEditorManager.beginChainPose(chain))
            return;

        RigBoneStruct root = state.getRigHandle().getBone(chain.getRootBone());
        RigBoneStruct middle = state.getRigHandle().getBone(chain.getMiddleBone());

        System.arraycopy(animationEditorManager.getChainPoseStart(0), 0, pose, 0, pose.length);
        System.arraycopy(animationEditorManager.getChainPoseStart(1), 0, middlePose, 0, middlePose.length);
        CharacterPoseUtility.getTranslation(state.getWorldMatrix(chain.getRootBone()), ikRoot);
        CharacterPoseUtility.getTranslation(state.getWorldMatrix(chain.getMiddleBone()), ikMiddle);
        CharacterPoseUtility.getTranslation(state.getWorldMatrix(chain.getEndBone()), ikEnd);
        CharacterPoseUtility.extractRotation(state.getWorldMatrix(chain.getRootBone()), rootWorldRotation);
        holdParent(state, root);

        bindRotation.set(root.getRotation());
        readRotation(pose, startRotation);
        CharacterPoseUtility.composeBindTurn(bindRotation, startRotation, startLocal, matrixScratch);
        middleBindRotation.set(middle.getRotation());
        readRotation(middlePose, middleStartRotation);
        CharacterPoseUtility.composeBindTurn(middleBindRotation, middleStartRotation, middleStartLocal, matrixScratch);

        joint.set(ikEnd);
        resolveHit(mouseX, mouseY, startHit);

        this.ikChain = chain;
        this.dragTool = EditorTool.IK;
        this.axisLock = EngineSetting.INDEX_NOT_FOUND;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    // The end carried to the pointer and the chain bent to reach it: the root turned so the middle joint stands where
    // the two bones' lengths meet on the side the pole names, then the middle turned to point the end at the target
    private void applyIk(float mouseX, float mouseY) {

        resolveHit(mouseX, mouseY, hitScratch);
        hitScratch.subtract(startHit).add(ikEnd);

        float upper = vectorA.set(ikMiddle).subtract(ikRoot).length();
        float lower = vectorB.set(ikEnd).subtract(ikMiddle).length();
        Vector3 toTarget = reached.set(hitScratch).subtract(ikRoot);
        float distance = toTarget.length();

        if (upper <= 0f || lower <= 0f || distance <= 0f)
            return;

        toTarget.divide(distance);
        distance = Math.clamp(distance,
                Math.abs(upper - lower) + EditorSetting.ANIMATION_EDITOR_IK_REACH_MARGIN,
                upper + lower - EditorSetting.ANIMATION_EDITOR_IK_REACH_MARGIN);

        float cos = Math.clamp(
                (upper * upper + distance * distance - lower * lower) / (2f * upper * distance), -1f, 1f);
        float sin = (float) Math.sqrt(1f - cos * cos);
        Vector3 pole = ikChain.getPole();

        bend.set(pole).subtract(toTarget.x * pole.dot(toTarget), toTarget.y * pole.dot(toTarget),
                toTarget.z * pole.dot(toTarget));

        if (bend.length() <= EditorSetting.ANIMATION_EDITOR_IK_POLE_EPSILON)
            bend.set(vectorA).subtract(toTarget.x * vectorA.dot(toTarget), toTarget.y * vectorA.dot(toTarget),
                    toTarget.z * vectorA.dot(toTarget));

        if (bend.length() <= EditorSetting.ANIMATION_EDITOR_IK_POLE_EPSILON)
            return;

        bend.divide(bend.length());

        hitScratch.set(toTarget).multiply(upper * cos).add(bend.x * upper * sin, bend.y * upper * sin,
                bend.z * upper * sin);
        resolveTurn(vectorA, hitScratch, rootTurn);

        CharacterPoseUtility.transformDirection(rootTurn, vectorB.x, vectorB.y, vectorB.z, eulerScratch);
        reached.set(toTarget).multiply(distance).subtract(hitScratch);
        resolveTurn(eulerScratch, reached, middleTurn);

        CharacterPoseUtility.applyWorldTurn(parentRotation, rootTurn, startLocal, rotated, matrixScratch);
        CharacterPoseUtility.toKeyEuler(rotated, bindRotation, startRotation, eulerScratch, matrixScratch, bindScratch);
        writeChannel(AnimationEditorChannel.ROTATION, eulerScratch);

        matrixScratch.set(rootTurn).inverse();
        turn.set(matrixScratch).multiply(middleTurn).multiply(rootTurn);
        CharacterPoseUtility.applyWorldTurn(rootWorldRotation, turn, middleStartLocal, rotated, matrixScratch);
        CharacterPoseUtility.toKeyEuler(
                rotated, middleBindRotation, middleStartRotation, eulerScratch, matrixScratch, bindScratch);
        middlePose[AnimationEditorChannel.ROTATION.toPoseIndex(0)] = eulerScratch.x;
        middlePose[AnimationEditorChannel.ROTATION.toPoseIndex(1)] = eulerScratch.y;
        middlePose[AnimationEditorChannel.ROTATION.toPoseIndex(2)] = eulerScratch.z;
    }

    // The least turn carrying one direction onto another
    private void resolveTurn(Vector3 from, Vector3 to, Matrix4 out) {

        float fromLength = from.length();
        float toLength = to.length();

        axis.set(from.y * to.z - from.z * to.y, from.z * to.x - from.x * to.z, from.x * to.y - from.y * to.x);

        float sin = axis.length();

        if (fromLength <= 0f || toLength <= 0f || sin <= EditorSetting.ANIMATION_EDITOR_IK_POLE_EPSILON) {
            out.set(1f);
            return;
        }

        float angle = (float) Math.atan2(sin, from.dot(to));

        CharacterPoseUtility.toAxisRotation(axis.divide(sin), angle, out);
    }

    private Vector3 readRotation(float[] channels, Vector3 out) {
        return out.set(
                channels[AnimationEditorChannel.ROTATION.toPoseIndex(0)],
                channels[AnimationEditorChannel.ROTATION.toPoseIndex(1)],
                channels[AnimationEditorChannel.ROTATION.toPoseIndex(2)]);
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
