package editor.characterview.camera;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.EditorInputSystem;
import editor.runtime.EditorSetting;
import engine.assets.camera.CameraInstance;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class CharacterViewCameraSystem extends SystemPackage {

    /*
     * Orbits this window's camera around the character a character viewport
     * shows, aimed at a share of its model's height. The orbit button turns
     * the view while held, and scrolling over the viewport zooms within
     * limits scaled to the model's height, so a tall or a small character
     * frames the same.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;

    // Orbit
    private Vector3 target;
    private Vector3 position;
    private Vector2 rotation;
    private float distance;
    private float span;

    // Base \\

    @Override
    protected void create() {

        // Orbit
        this.target = new Vector3();
        this.position = new Vector3();
        this.rotation = new Vector2();
        this.distance = EditorSetting.CHARACTER_VIEW_ORBIT_DISTANCE_DEFAULT;
        this.span = 1f;
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.characterViewPoseSystem = get(CharacterViewPoseSystem.class);
    }

    @Override
    protected void awake() {

        rotate(
                EditorSetting.CHARACTER_VIEW_ORBIT_INITIAL_YAW_DEGREES,
                EditorSetting.CHARACTER_VIEW_ORBIT_INITIAL_PITCH_DEGREES);
        placeCamera();
    }

    // Update \\

    @Override
    protected void update() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        followCharacter();

        if (rawInput.isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_ORBIT))
            rotate(rawInput.getDeltaX(), rawInput.getDeltaY());

        if (editorInputSystem.isPointerActive())
            distance = Math.max(
                    EditorSetting.CHARACTER_VIEW_ORBIT_DISTANCE_MIN * span,
                    Math.min(
                            EditorSetting.CHARACTER_VIEW_ORBIT_DISTANCE_MAX * span,
                            distance - rawInput.getScrollY() * EditorSetting.CHARACTER_VIEW_ORBIT_ZOOM_STEP * span));

        placeCamera();
    }

    // Orbit \\

    // Aims at the character and scales the distance by its height when that changes
    private void followCharacter() {

        if (!characterViewPoseSystem.hasSubject())
            return;

        float height = characterViewPoseSystem.getEntityData().getModelHeight();

        if (height <= 0f)
            return;

        target.set(0f, height * EditorSetting.CHARACTER_VIEW_ORBIT_TARGET_HEIGHT, 0f);

        if (height == span)
            return;

        distance = distance * height / span;
        span = height;
    }

    private void rotate(float yawDegrees, float pitchDegrees) {

        rotation.set(yawDegrees, pitchDegrees);
        context.getWindow().getActiveCamera().setRotation(rotation);
    }

    private void placeCamera() {

        CameraInstance camera = context.getWindow().getActiveCamera();
        Vector3 direction = camera.getDirection();

        position.set(target).subtract(direction.x * distance, direction.y * distance, direction.z * distance);
        camera.setPosition(position);
    }
}
