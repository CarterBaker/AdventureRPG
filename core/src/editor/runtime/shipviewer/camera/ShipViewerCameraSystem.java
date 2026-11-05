package editor.runtime.shipviewer.camera;

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
     * Orbits this window's camera around the shown ship. A drag with the orbit
     * button begun over the viewport turns the view, a drag with the pan
     * button slides the point it turns about across the view so the ship
     * follows the pointer, and scrolling zooms, the distance measured in the
     * ship's bounding radius, so the frame holds as the ship is drawn larger
     * or smaller or another one is shown. A view can be snapped to look back
     * along any axis of the ship — from the bow, astern, above, below,
     * starboard or port — centred on the ship's middle, and the pan set back
     * to it. The camera's right and up are kept for
     * the axis gizmo that shows where the view looks.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ShipViewerSystem shipViewerSystem;

    // Orbit
    private Vector3 target;
    private Vector3 pan;
    private Vector3 position;
    private Vector3 direction;
    private Vector2 rotation;
    private float distance;

    // Basis — the camera's right and up, for panning and the gizmo
    private Vector3 right;
    private Vector3 up;

    // Drag
    private boolean orbiting;
    private boolean panning;

    // Base \\

    @Override
    protected void create() {

        // Orbit
        this.target = new Vector3();
        this.pan = new Vector3();
        this.position = new Vector3();
        this.direction = new Vector3();
        this.rotation = new Vector2();
        this.distance = ShipViewerSetting.ORBIT_DISTANCE_DEFAULT;

        // Basis
        this.right = new Vector3();
        this.up = new Vector3();
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

        this.orbiting = rawInput.isButtonHeld(ShipViewerSetting.BUTTON_ORBIT)
                && (orbiting || editorInputSystem.isClicked(ShipViewerSetting.BUTTON_ORBIT));
        this.panning = rawInput.isButtonHeld(ShipViewerSetting.BUTTON_PAN)
                && (panning || editorInputSystem.isClicked(ShipViewerSetting.BUTTON_PAN));

        if (orbiting)
            rotate(rawInput.getDeltaX(), rawInput.getDeltaY());

        if (panning)
            slide(rawInput.getDeltaX(), rawInput.getDeltaY());

        if (editorInputSystem.isPointerActive())
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

    // The point the view turns about slid across the view by a drag, as far as the pointer moved across the frame
    // at the distance the camera stands from it, so the ship stays under the pointer
    private void slide(float deltaX, float deltaY) {

        CameraInstance camera = context.getWindow().getActiveCamera();
        float reach = distance * shipViewerSystem.getBoundingRadius();
        float step = 2f * reach * (float) Math.tan(Math.toRadians(camera.getFOV() * 0.5f))
                / Math.max(1f, camera.getViewport().y);

        pan.subtract(right.x * deltaX * step, right.y * deltaX * step, right.z * deltaX * step);
        pan.add(up.x * deltaY * step, up.y * deltaY * step, up.z * deltaY * step);
    }

    // Looks back at the ship along one of its axes, by the axis's index in VIEW_AXES, centred on its middle; straight
    // up or down leans a little toward the bow so the view keeps a heading
    public void snap(int axisIndex) {

        float[] axis = ShipViewerSetting.VIEW_AXIS_VECTORS[axisIndex];
        float lean = axis[EngineSetting.AXIS_Y] != 0f ? ShipViewerSetting.VIEW_VERTICAL_LEAN : 0f;

        direction.set(-axis[EngineSetting.AXIS_X] - lean, -axis[EngineSetting.AXIS_Y],
                -axis[EngineSetting.AXIS_Z]);
        context.getWindow().getActiveCamera().setDirection(direction);
        pan.set(0f, 0f, 0f);
        placeCamera();
    }

    // The point the view turns about set back to the ship's middle
    public void centre() {
        pan.set(0f, 0f, 0f);
        placeCamera();
    }

    // The camera set back from the point it turns about along its view, as far as the ship is large, its right and
    // up worked out again
    private void placeCamera() {

        CameraInstance camera = context.getWindow().getActiveCamera();
        Vector3 view = camera.getDirection();

        resolveBasis(view);

        if (!shipViewerSystem.isShown())
            return;

        float reach = distance * shipViewerSystem.getBoundingRadius();

        target.set(
                shipViewerSystem.getCenter(EngineSetting.AXIS_X),
                shipViewerSystem.getCenter(EngineSetting.AXIS_Y),
                shipViewerSystem.getCenter(EngineSetting.AXIS_Z)).add(pan);
        position.set(target).subtract(view.x * reach, view.y * reach, view.z * reach);
        camera.setPosition(position);
    }

    // right = view × world up, up = right × view
    private void resolveBasis(Vector3 view) {

        right.set(-view.z, 0f, view.x);

        if (right.lengthSquared() < EngineSetting.DIVISION_EPSILON)
            right.set(1f, 0f, 0f);

        right.normalize();
        up.set(
                right.y * view.z - right.z * view.y,
                right.z * view.x - right.x * view.z,
                right.x * view.y - right.y * view.x);
    }

    // Accessible \\

    // The camera's right in model space
    public Vector3 getRight() {
        return right;
    }

    // The camera's up in model space
    public Vector3 getUp() {
        return up;
    }
}
