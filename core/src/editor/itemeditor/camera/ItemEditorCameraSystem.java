package editor.itemeditor.camera;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.itemeditor.ItemEditorSetting;
import editor.runtime.EditorInputSystem;
import engine.assets.camera.CameraInstance;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class ItemEditorCameraSystem extends SystemPackage {

    /*
     * Orbits this window's camera around the centre of the active item's
     * model, however many blocks it spans. The orbit button turns the view and
     * scrolling zooms, within limits scaled to the model's largest side, and
     * the distance scales with it when a larger or smaller model comes up.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ItemEditorManager itemEditorManager;

    // Orbit
    private Vector3 target;
    private Vector3 position;
    private Vector2 rotation;
    private float distance;
    private int span;

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
        this.span = 1;
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.itemEditorManager = get(ItemEditorManager.class);
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

        followModel();

        if (rawInput.isButtonHeld(ItemEditorSetting.BUTTON_ORBIT))
            rotate(rawInput.getDeltaX(), rawInput.getDeltaY());

        distance = Math.max(
                ItemEditorSetting.ORBIT_DISTANCE_MIN * span,
                Math.min(
                        ItemEditorSetting.ORBIT_DISTANCE_MAX * span,
                        distance - rawInput.getScrollY() * ItemEditorSetting.ORBIT_ZOOM_STEP * span));

        placeCamera();
    }

    // Orbit \\

    // Centres the orbit on the active model and scales the distance by its largest side when that changes
    private void followModel() {

        ItemDocumentInstance document = itemEditorManager.getActiveDocument();

        if (document == null)
            return;

        SubVoxelModelStruct model = document.getModel();
        int modelSpan = Math.max(model.getBlocksX(), Math.max(model.getBlocksY(), model.getBlocksZ()));

        target.set(
                model.getBlocksX() * ItemEditorSetting.ORBIT_TARGET_X,
                model.getBlocksY() * ItemEditorSetting.ORBIT_TARGET_Y,
                model.getBlocksZ() * ItemEditorSetting.ORBIT_TARGET_Z);

        if (modelSpan == span)
            return;

        distance = distance * modelSpan / span;
        span = modelSpan;
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
