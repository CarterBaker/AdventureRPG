package editor.runtime.viewcamera;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.runtime.EditorSetting;
import editor.runtime.input.EditorInputSystem;
import engine.assets.camera.CameraInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class ViewCameraSystem extends SystemPackage {

    /*
     * The one camera every 3D editor viewport looks through, orbiting what
     * the context's ViewCameraSubject frames. A drag with the orbit button
     * turns the view, a drag with the pan button slides the point it turns
     * about so what is shown follows the pointer, and each notch of the wheel
     * zooms by one factor, the distance measured in the subject's bounding
     * radius so the frame holds as it grows, shrinks or changes. A drag
     * begins only on a press over the view that no tool of the window claimed
     * that frame, made with no other button held, so it reads input late,
     * after every tool has had the press. A view can be snapped to look back
     * along any axis, centred, and the pan set back to the middle. The
     * camera's right and up are kept for panning and for an axis gizmo.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ViewCameraSubject viewCameraSubject;

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
    private int dragButton;

    // Base \\

    @Override
    protected void create() {

        // Orbit
        this.target = new Vector3();
        this.pan = new Vector3();
        this.position = new Vector3();
        this.direction = new Vector3();
        this.rotation = new Vector2();
        this.distance = EditorSetting.VIEW_CAMERA_DISTANCE_DEFAULT;

        // Basis
        this.right = new Vector3();
        this.up = new Vector3();

        // Drag
        this.dragButton = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {

        this.editorInputSystem = get(EditorInputSystem.class);
        this.viewCameraSubject = context instanceof ViewCameraProvider provider
                ? provider.getViewCameraSubject()
                : throwException("'" + context.getClass().getSimpleName()
                        + "' creates a ViewCameraSystem but does not implement ViewCameraProvider.");
    }

    @Override
    protected void awake() {

        rotate(EditorSetting.VIEW_CAMERA_INITIAL_YAW_DEGREES, EditorSetting.VIEW_CAMERA_INITIAL_PITCH_DEGREES);
        placeCamera();
    }

    // Update \\

    @Override
    protected void lateUpdate() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        this.dragButton = resolveDrag(rawInput);

        if (!viewCameraSubject.isShown())
            return;

        if (dragButton == EditorSetting.VIEW_CAMERA_BUTTON_ORBIT)
            rotate(rawInput.getDeltaX(), rawInput.getDeltaY());
        else if (dragButton == EditorSetting.VIEW_CAMERA_BUTTON_PAN)
            slide(rawInput.getDeltaX(), rawInput.getDeltaY());

        if (editorInputSystem.isPointerOver() && rawInput.getScrollY() != 0f)
            distance = Math.clamp(
                    distance * (float) Math.pow(EditorSetting.VIEW_CAMERA_ZOOM_FACTOR, -rawInput.getScrollY()),
                    EditorSetting.VIEW_CAMERA_DISTANCE_MIN,
                    EditorSetting.VIEW_CAMERA_DISTANCE_MAX);

        placeCamera();
    }

    // Drag \\

    // The button a drag is held with: kept while it stays down, begun by a press only the camera took
    private int resolveDrag(RawInputHandle rawInput) {

        if (dragButton != EngineSetting.INDEX_NOT_FOUND && rawInput.isButtonHeld(dragButton))
            return dragButton;

        if (beginsDrag(rawInput, EditorSetting.VIEW_CAMERA_BUTTON_ORBIT, EditorSetting.VIEW_CAMERA_BUTTON_PAN))
            return EditorSetting.VIEW_CAMERA_BUTTON_ORBIT;

        if (beginsDrag(rawInput, EditorSetting.VIEW_CAMERA_BUTTON_PAN, EditorSetting.VIEW_CAMERA_BUTTON_ORBIT))
            return EditorSetting.VIEW_CAMERA_BUTTON_PAN;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private boolean beginsDrag(RawInputHandle rawInput, int button, int otherButton) {
        return editorInputSystem.isClicked(button) && !editorInputSystem.isClaimed(button)
                && !rawInput.isButtonHeld(otherButton);
    }

    // Orbit \\

    private void rotate(float yawDegrees, float pitchDegrees) {

        rotation.set(yawDegrees, pitchDegrees);
        context.getWindow().getActiveCamera().setRotation(rotation);
    }

    // The point the view turns about slid across the view by a drag, as far as the pointer moved across the frame
    // at the distance the camera stands from it, so what is shown stays under the pointer
    private void slide(float deltaX, float deltaY) {

        CameraInstance camera = context.getWindow().getActiveCamera();
        float reach = distance * viewCameraSubject.getBoundingRadius();
        float step = 2f * reach * (float) Math.tan(Math.toRadians(camera.getFOV() * 0.5f))
                / Math.max(1f, camera.getViewport().y);

        pan.subtract(right.x * deltaX * step, right.y * deltaX * step, right.z * deltaX * step);
        pan.add(up.x * deltaY * step, up.y * deltaY * step, up.z * deltaY * step);
    }

    // Looks back at what is shown along an axis, centred on its middle; straight up or down leans a little along x
    // so the view keeps a heading
    public void snap(float[] axis) {

        float lean = axis[EngineSetting.AXIS_Y] != 0f ? EditorSetting.VIEW_CAMERA_VERTICAL_LEAN : 0f;

        direction.set(-axis[EngineSetting.AXIS_X] - lean, -axis[EngineSetting.AXIS_Y],
                -axis[EngineSetting.AXIS_Z]);
        context.getWindow().getActiveCamera().setDirection(direction);
        centre();
    }

    // The point the view turns about set back to the middle of what is shown
    public void centre() {
        pan.set(0f, 0f, 0f);
        placeCamera();
    }

    // The camera set back from the point it turns about along its view, as far as what is shown is large, its right
    // and up worked out again
    private void placeCamera() {

        CameraInstance camera = context.getWindow().getActiveCamera();
        Vector3 view = camera.getDirection();

        resolveBasis(view);

        if (!viewCameraSubject.isShown())
            return;

        float reach = distance * viewCameraSubject.getBoundingRadius();

        target.set(
                viewCameraSubject.getCenter(EngineSetting.AXIS_X),
                viewCameraSubject.getCenter(EngineSetting.AXIS_Y),
                viewCameraSubject.getCenter(EngineSetting.AXIS_Z)).add(pan);
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

    // Whether the camera holds a drag, so the window's tools leave the pointer alone
    public boolean isDragging() {
        return dragButton != EngineSetting.INDEX_NOT_FOUND;
    }

    // The camera's right in model space
    public Vector3 getRight() {
        return right;
    }

    // The camera's up in model space
    public Vector3 getUp() {
        return up;
    }
}
