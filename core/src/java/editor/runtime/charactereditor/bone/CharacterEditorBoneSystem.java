package editor.runtime.charactereditor.bone;

import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.charactereditor.meshview.CharacterEditorMeshViewSystem;
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

public class CharacterEditorBoneSystem extends SystemPackage {

    /*
     * Turns this tab's pointer into bone work on the mesh and weights layers
     * while the tool in use is Pick or Pose. Pick picks the bone whose joint
     * is clicked, or, on the mesh away from every joint, the bone weighing
     * most on the vertex clicked, so the weight tools act on it. Pose picks
     * the bone whose joint is pressed and turns it about the joint as the
     * pointer circles it on screen, about the line of sight, everything it
     * carries following in the edit pose, and its twin across the body the
     * mirrored way while mirroring; Ctrl snaps the turn. Letting go keeps the
     * turn, a right click or Escape puts the bone back. Turns are written
     * only on frames the pointer moves.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ViewCameraSystem viewCameraSystem;
    private CharacterEditorManager characterEditorManager;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private CharacterEditorModeSystem characterEditorModeSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private ShortcutManager shortcutManager;

    // Shortcuts
    private int snapShortcutID;

    // Drag
    private int dragBone;
    private Vector3 startTurn;
    private Matrix4 startWorld;
    private Matrix4 startWorldInverse;
    private Vector2 jointScreen;
    private float startMouseX;
    private float startMouseY;
    private float lastMouseX;
    private float lastMouseY;

    // Scratch
    private Vector3 jointScratch;
    private Vector2 screenScratch;
    private Vector3 axis;
    private Vector3 turnScratch;
    private Matrix4 turn;
    private Matrix4 rotated;

    // Base \\

    @Override
    protected void create() {

        // Drag
        this.dragBone = EngineSetting.INDEX_NOT_FOUND;
        this.startTurn = new Vector3();
        this.startWorld = new Matrix4();
        this.startWorldInverse = new Matrix4();
        this.jointScreen = new Vector2();

        // Scratch
        this.jointScratch = new Vector3();
        this.screenScratch = new Vector2();
        this.axis = new Vector3();
        this.turnScratch = new Vector3();
        this.turn = new Matrix4();
        this.rotated = new Matrix4();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditorMeshViewSystem = get(CharacterEditorMeshViewSystem.class);
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

        if (!characterEditorManager.isBoneTool() || !characterViewPoseSystem.hasSubject()
                || !characterEditorMeshViewSystem.hasActiveDocument()) {
            this.dragBone = EngineSetting.INDEX_NOT_FOUND;
            return;
        }

        if (dragBone != EngineSetting.INDEX_NOT_FOUND) {
            updateDrag(rawInput);
            return;
        }

        if (!editorInputSystem.isPointerActive() || viewCameraSystem.isDragging())
            return;

        if (editorInputSystem.isClicked(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT))
            press(rawInput.getMouseX(), rawInput.getMouseY());
    }

    // Press \\

    // A joint pressed picks its bone and, with Pose, starts turning it; Pick on the mesh picks the bone the vertex
    // clicked leans on most; a press on neither is left to the view camera
    private void press(float mouseX, float mouseY) {

        boolean posing = characterEditorManager.isPoseTool();
        int picked = pickJoint(mouseX, mouseY);

        if (picked == EngineSetting.INDEX_NOT_FOUND && !posing)
            picked = pickVertexBone(mouseX, mouseY);

        if (picked == EngineSetting.INDEX_NOT_FOUND)
            return;

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
        characterEditorManager.pickBone(picked);

        if (posing)
            startDrag(picked, mouseX, mouseY);
    }

    // The shown bone whose posed joint lies nearest the pointer within the pick radius, or none
    private int pickJoint(float mouseX, float mouseY) {

        AnimationStateHandle state = characterViewPoseSystem.getAnimationStateHandle();
        RigHandle rigHandle = state.getRigHandle();
        float radius = EditorSetting.CHARACTER_VIEW_PICK_RADIUS_PIXELS;
        float nearest = radius * radius;
        int picked = EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < rigHandle.getBoneCount(); i++) {

            if (!characterEditorManager.isBoneShown(characterEditorModeSystem.getRegion(), rigHandle, i))
                continue;

            CharacterPoseUtility.getTranslation(state.getWorldMatrix(i), jointScratch);

            if (!characterEditorMeshViewSystem.project(jointScratch.x, jointScratch.y, jointScratch.z, screenScratch))
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

    // The bone weighing most on the vertex under the pointer, or none off the mesh
    private int pickVertexBone(float mouseX, float mouseY) {

        int vertexIndex = characterEditorMeshViewSystem.pickVertex(mouseX, mouseY);

        if (vertexIndex == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        RiggedMeshStruct mesh = characterEditorMeshViewSystem.getActiveDocument().getMesh();
        int heaviest = EngineSetting.INDEX_NOT_FOUND;
        float most = 0f;

        for (int i = 0; i < mesh.getInfluenceCount(vertexIndex); i++)
            if (mesh.getInfluenceWeight(vertexIndex, i) > most) {
                most = mesh.getInfluenceWeight(vertexIndex, i);
                heaviest = mesh.getInfluenceBone(vertexIndex, i);
            }

        return heaviest;
    }

    // Drag \\

    // Holds the bone's turn in the edit pose and its frame in the world as it stands this frame
    private void startDrag(int boneIndex, float mouseX, float mouseY) {

        Matrix4 world = characterViewPoseSystem.getAnimationStateHandle().getWorldMatrix(boneIndex);

        CharacterPoseUtility.getTranslation(world, jointScratch);

        if (!characterEditorMeshViewSystem.project(jointScratch.x, jointScratch.y, jointScratch.z, jointScreen))
            jointScreen.set(mouseX, mouseY);

        CharacterPoseUtility.extractRotation(world, startWorld);
        startWorldInverse.set(startWorld).inverse();
        startTurn.set(characterEditorManager.getEditPoseTurn(boneIndex));

        this.dragBone = boneIndex;
        this.startMouseX = mouseX;
        this.startMouseY = mouseY;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    // A right click or Escape puts the bone back, letting go keeps the turn, and a moved pointer carries it on
    private void updateDrag(RawInputHandle rawInput) {

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)
                || rawInput.isButtonClicked(EditorSetting.CHARACTER_VIEW_BUTTON_CANCEL)) {
            editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_CANCEL);
            characterEditorManager.setEditPoseTurn(dragBone, startTurn);
            this.dragBone = EngineSetting.INDEX_NOT_FOUND;
            return;
        }

        if (!rawInput.isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT)) {
            this.dragBone = EngineSetting.INDEX_NOT_FOUND;
            return;
        }

        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (mouseX == lastMouseX && mouseY == lastMouseY)
            return;

        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        applyTurn(mouseX, mouseY, shortcutManager.isHeld(snapShortcutID, context.getWindow()));
    }

    // The pointer's turn about the joint on screen, about the line of sight, carried into the bone's own frame and
    // laid after the turn it held: start * inverse(world) * turn * world
    private void applyTurn(float mouseX, float mouseY, boolean snap) {

        float angle = (float) (Math.atan2(mouseY - jointScreen.y, mouseX - jointScreen.x)
                - Math.atan2(startMouseY - jointScreen.y, startMouseX - jointScreen.x));
        Vector3 view = context.getWindow().getActiveCamera().getDirection();

        if (snap)
            angle = (float) Math.toRadians(Math.round(Math.toDegrees(angle) / EditorSetting.CHARACTER_VIEW_SNAP_DEGREES)
                    * EditorSetting.CHARACTER_VIEW_SNAP_DEGREES);

        axis.set(-view.x, -view.y, -view.z).normalize();
        CharacterPoseUtility.toAxisRotation(axis, angle, turn);
        CharacterPoseUtility.toRotation(startTurn, rotated);
        rotated.multiply(startWorldInverse).multiply(turn).multiply(startWorld);
        CharacterPoseUtility.toEuler(rotated, startTurn, turnScratch);
        characterEditorManager.setEditPoseTurn(dragBone, turnScratch);
    }
}
