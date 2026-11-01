package editor.runtime.charactereditor.rig;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class CharacterEditorRigSystem extends SystemPackage {

    /*
     * Turns this tab's pointer into rig edits while the Rig mode is open,
     * by the rig tool in use. A click on a joint picks its bone with every
     * tool; Move then drags the joint across the plane facing the camera,
     * its children following, Scale grows or shrinks the bone's size by how
     * far the pointer travels from the joint, Place stands a new child of
     * the selected bone where it clicks, level with that bone's joint, and
     * Erase removes the bone clicked. A drag is one undo step: letting go
     * keeps it, a right click or Escape puts the rig back, and Ctrl snaps it.
     * Edits are written only on frames the pointer moves.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CharacterEditorManager characterEditorManager;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private ShortcutManager shortcutManager;

    // Shortcuts
    private int snapShortcutID;

    // Drag
    private EditorTool dragTool;
    private Vector3 joint;
    private Vector2 jointScreen;
    private Vector3 startHit;
    private float startDistance;
    private float lastMouseX;
    private float lastMouseY;

    // Scratch
    private Vector2 screenScratch;
    private Vector3 rayOrigin;
    private Vector3 rayDirection;
    private Vector3 hitScratch;

    // Base \\

    @Override
    protected void create() {

        // Drag
        this.joint = new Vector3();
        this.jointScreen = new Vector2();
        this.startHit = new Vector3();

        // Scratch
        this.screenScratch = new Vector2();
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();
        this.hitScratch = new Vector3();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditorMeshViewSystem = get(CharacterEditorMeshViewSystem.class);
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

        if (characterEditorManager.getMode() != CharacterEditorMode.RIG
                || characterEditorManager.getRigBoneCount() == 0) {
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

    // A drag still held when the mode closes keeps what it did
    private void abandon() {

        if (dragTool != null)
            characterEditorManager.endRigGesture();

        this.dragTool = null;
    }

    // Press \\

    private void press(float mouseX, float mouseY) {

        EditorTool tool = characterEditorManager.getRigTool();
        int picked = pickJoint(mouseX, mouseY);

        if (tool == EditorTool.ERASE) {

            if (picked != EngineSetting.INDEX_NOT_FOUND)
                characterEditorManager.removeRigBone(picked);

            return;
        }

        if (picked != EngineSetting.INDEX_NOT_FOUND)
            characterEditorManager.selectRigBone(picked);

        switch (tool) {
            case MOVE, SCALE -> startDrag(tool, mouseX, mouseY);
            case PLACE -> {
                if (picked == EngineSetting.INDEX_NOT_FOUND)
                    placeBone(mouseX, mouseY);
            }
            default -> {
            }
        }
    }

    // The bone whose joint lies nearest the pointer within the pick radius, or none
    private int pickJoint(float mouseX, float mouseY) {

        float radius = EditorSetting.CHARACTER_VIEW_PICK_RADIUS_PIXELS;
        float nearest = radius * radius;
        int picked = EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < characterEditorManager.getRigBoneCount(); i++) {

            CharacterPoseUtility.getTranslation(characterEditorManager.getRigBoneWorldMatrix(i), joint);

            if (!characterEditorMeshViewSystem.project(joint.x, joint.y, joint.z, screenScratch))
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

    // A new child of the selected bone where the pointer meets the plane facing the camera through its joint
    private void placeBone(float mouseX, float mouseY) {

        int selected = characterEditorManager.getSelectedRigBoneIndex();

        if (selected == EngineSetting.INDEX_NOT_FOUND)
            return;

        CharacterPoseUtility.getTranslation(characterEditorManager.getRigBoneWorldMatrix(selected), joint);
        resolveHit(mouseX, mouseY, hitScratch);
        characterEditorManager.addRigBone(hitScratch);
    }

    // Drag \\

    private void startDrag(EditorTool tool, float mouseX, float mouseY) {

        if (!characterEditorManager.beginRigGesture())
            return;

        int selected = characterEditorManager.getSelectedRigBoneIndex();

        CharacterPoseUtility.getTranslation(characterEditorManager.getRigBoneWorldMatrix(selected), joint);

        if (!characterEditorMeshViewSystem.project(joint.x, joint.y, joint.z, jointScreen))
            jointScreen.set(mouseX, mouseY);

        resolveHit(mouseX, mouseY, startHit);

        this.dragTool = tool;
        this.startDistance = Math.max(
                EditorSetting.CHARACTER_VIEW_SCALE_MIN_PIXELS,
                (float) Math.hypot(mouseX - jointScreen.x, mouseY - jointScreen.y));
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    // A right click or Escape puts the rig back, letting go keeps the drag, and a moved pointer carries it on
    private void updateDrag(RawInputHandle rawInput) {

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)
                || rawInput.isButtonClicked(EditorSetting.CHARACTER_VIEW_BUTTON_CANCEL)) {
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

        if (dragTool == EditorTool.MOVE)
            applyMove(mouseX, mouseY, snap);
        else
            applyScale(mouseX, mouseY, snap);
    }

    private void applyMove(float mouseX, float mouseY, boolean snap) {

        resolveHit(mouseX, mouseY, hitScratch);
        hitScratch.subtract(startHit);
        hitScratch.set(snapValue(hitScratch.x, snap), snapValue(hitScratch.y, snap), snapValue(hitScratch.z, snap));
        characterEditorManager.moveRigJoint(hitScratch);
    }

    private void applyScale(float mouseX, float mouseY, boolean snap) {

        float factor = (float) Math.hypot(mouseX - jointScreen.x, mouseY - jointScreen.y) / startDistance;

        if (snap)
            factor = Math.round(factor / EditorSetting.CHARACTER_VIEW_SNAP_SCALE)
                    * EditorSetting.CHARACTER_VIEW_SNAP_SCALE;

        characterEditorManager.scaleRigBone(factor);
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
