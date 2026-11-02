package editor.runtime.shipviewer.camera;

import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.kernel.inputpipeline.input.RawInputHandle;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.shipviewer.ShipViewerSetting;
import editor.runtime.shipviewer.view.ShipViewerSystem;
import engine.assets.camera.CameraInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class ShipViewerCameraSystem extends SystemPackage {

    /*
     * Orbits this window's camera around the middle of the shown ship's
     * bounds. The orbit button turns the view and scrolling zooms, the
     * distance measured in the ship's bounding radius, so the frame holds as
     * the ship is saved larger or smaller or another one is shown.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ShipViewerSystem shipViewerSystem;

    // Orbit
    private Vector3 target;
    private Vector3 position;
    private Vector2 rotation;
    private float distance;

    // Base \\

    @Override
    protected void create() {

        // Orbit
        this.target = new Vector3();
        this.position = new Vector3();
        this.rotation = new Vector2();
        this.distance = ShipViewerSetting.ORBIT_DISTANCE_DEFAULT;
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.shipViewerSystem = get(ShipViewerSystem.class);
    }

    @Override
    protected void awake() {

        rotate(ShipViewerSetting.ORBIT_INITIAL_YAW_DEGREES, ShipViewerSetting.ORBIT_INITIAL_PITCH_DEGREES);
        placeCamera();
    }

    // Update \\

    @Override
    protected void update() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (rawInput.isButtonHeld(ShipViewerSetting.BUTTON_ORBIT))
            rotate(rawInput.getDeltaX(), rawInput.getDeltaY());

        distance = Math.clamp(
                distance - rawInput.getScrollY() * ShipViewerSetting.ORBIT_ZOOM_STEP,
                ShipViewerSetting.ORBIT_DISTANCE_MIN,
                ShipViewerSetting.ORBIT_DISTANCE_MAX);

        placeCamera();
    }

    // Orbit \\

    private void rotate(float yawDegrees, float pitchDegrees) {

        rotation.set(yawDegrees, pitchDegrees);
        context.getWindow().getActiveCamera().setRotation(rotation);
    }

    // The camera set back from the ship's middle along its view, as far as the ship is large
    private void placeCamera() {

        VehicleHandle vehicleHandle = shipViewerSystem.getVehicleHandle();

        if (vehicleHandle == null)
            return;

        CameraInstance camera = context.getWindow().getActiveCamera();
        Vector3 direction = camera.getDirection();
        float scale = 0.5f / EngineSetting.SUB_VOXEL_RESOLUTION;
        float reach = distance * vehicleHandle.getBoundingRadius();

        target.set(
                (vehicleHandle.getMinX() + vehicleHandle.getMaxX()) * scale,
                (vehicleHandle.getMinY() + vehicleHandle.getMaxY()) * scale,
                (vehicleHandle.getMinZ() + vehicleHandle.getMaxZ()) * scale);
        position.set(target).subtract(direction.x * reach, direction.y * reach, direction.z * reach);
        camera.setPosition(position);
    }
}
