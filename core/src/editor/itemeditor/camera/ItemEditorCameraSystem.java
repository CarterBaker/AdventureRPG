package editor.itemeditor.camera;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.itemeditor.ItemEditorSetting;
import engine.assets.camera.CameraInstance;
import engine.editor.EditorInputSystem;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class ItemEditorCameraSystem extends SystemPackage {

    /*
     * Orbits this window's camera around the item's block. The orbit button
     * turns the view and scrolling zooms.
     */

    // Internal
    private EditorInputSystem editorInputSystem;

    // Orbit
    private Vector3 target;
    private Vector3 position;
    private Vector2 rotation;
    private float distance;

    // Base \\

    @Override
    protected void create() {

        // Orbit
        this.target = new Vector3(
                ItemEditorSetting.ORBIT_TARGET_X,
                ItemEditorSetting.ORBIT_TARGET_Y,
                ItemEditorSetting.ORBIT_TARGET_Z);
        this.position = new Vector3();
        this.rotation = new Vector2();
        this.distance = ItemEditorSetting.ORBIT_DISTANCE_DEFAULT;
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
    }

    @Override
    protected void awake() {

        rotate(ItemEditorSetting.ORBIT_INITIAL_YAW_DEGREES, ItemEditorSetting.ORBIT_INITIAL_PITCH_DEGREES);
        placeCamera();
    }

    // Update \\

    @Override
    protected void update() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (rawInput.isButtonHeld(ItemEditorSetting.BUTTON_ORBIT))
            rotate(rawInput.getDeltaX(), rawInput.getDeltaY());

        distance = Math.max(
                ItemEditorSetting.ORBIT_DISTANCE_MIN,
                Math.min(
                        ItemEditorSetting.ORBIT_DISTANCE_MAX,
                        distance - rawInput.getScrollY() * ItemEditorSetting.ORBIT_ZOOM_STEP));

        placeCamera();
    }

    // Orbit \\

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
