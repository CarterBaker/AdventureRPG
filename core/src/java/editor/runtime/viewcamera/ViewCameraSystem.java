package editor.runtime.viewcamera;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.runtime.EditorSetting;
import editor.runtime.input.EditorInputSystem;
import engine.assets.camera.CameraInstance;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ViewCameraSystem extends SystemPackage {

    /*
     * The one camera every 3D editor viewport looks through, orbiting what
     * the context's ViewCameraSubject frames. A drag with the orbit button
     * turns the view, a drag with the pan button slides the point it turns
     * about so what is shown follows the pointer, and each notch of the wheel
     * zooms by one factor, the distance measured in the subject's bounding
     * radius so the frame holds as it grows, shrinks or changes; zoomed in as
     * far as it goes, the wheel dollies the point it turns about forward along
     * the view, as Blender's dolly does, and the near plane follows the
     * distance so nothing close to the lens is clipped. A drag
     * begins only on a press over the view that no tool of the window claimed
     * that frame, made with no other button held, so it reads input late,
     * after every tool has had the press. A view can be snapped to look back
     * along any axis, centred, and the pan set back to the middle. The way
     * it faces, its pan and its distance are its ViewCameraStateStruct. A
     * context naming a kind of view to share (ViewCameraShareProvider) shares
     * it with the tabs stacked with it that name the same kind, as one
     * camera: the tab shown hands its view to the others behind it every
     * frame, and a tab brought forward or joining a stack first takes the
     * view the stack holds, so switching tabs keeps the same part in sight;
     * tabs in separate docks look independently. The camera's right and up
     * are kept for panning and for an axis gizmo.
     */

    // Internal
    private TabManager tabManager;
    private EditorInputSystem editorInputSystem;
    private ViewCameraSubject viewCameraSubject;

    // View
    private ViewCameraStateStruct viewCameraState;

    // Share — the kind of view handed on through the stack, and the stack it was last handed through while shown
    private String viewCameraShare;
    private ObjectArrayList<TabHandle> sharedStack;

    // Orbit
    private Vector3 target;
    private Vector3 position;
    private Vector3 direction;
    private Vector2 rotation;

    // Basis — the camera's right and up, for panning and the gizmo
    private Vector3 right;
    private Vector3 up;

    // Drag
    private int dragButton;

    // Base \\

    @Override
    protected void create() {

        // View
        this.viewCameraState = new ViewCameraStateStruct();

        // Orbit
        this.target = new Vector3();
        this.position = new Vector3();
        this.direction = new Vector3();
        this.rotation = new Vector2();

        // Basis
        this.right = new Vector3();
        this.up = new Vector3();

        // Drag
        this.dragButton = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {

        this.tabManager = get(TabManager.class);
        this.editorInputSystem = get(EditorInputSystem.class);
        this.viewCameraShare = context instanceof ViewCameraShareProvider provider
                ? provider.getViewCameraShare()
                : EditorSetting.VIEW_CAMERA_SHARE_NONE;
        this.viewCameraSubject = context instanceof ViewCameraProvider provider
                ? provider.getViewCameraSubject()
                : throwException("'" + context.getClass().getSimpleName()
                        + "' creates a ViewCameraSystem but does not implement ViewCameraProvider.");
    }

    @Override
    protected void awake() {
        aimInitial();
        placeCamera();
    }

    // Update \\

    @Override
    protected void lateUpdate() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (!viewCameraShare.isEmpty())
            shareStack();

        this.dragButton = resolveDrag(rawInput);

        if (!viewCameraSubject.isShown())
            return;

        if (dragButton == EditorSetting.VIEW_CAMERA_BUTTON_ORBIT)
            rotate(rawInput.getDeltaX(), rawInput.getDeltaY());
        else if (dragButton == EditorSetting.VIEW_CAMERA_BUTTON_PAN)
            slide(rawInput.getDeltaX(), rawInput.getDeltaY());

        if (editorInputSystem.isPointerOver() && rawInput.getScrollY() != 0f)
            zoom(rawInput.getScrollY());

        placeCamera();
    }

    // Zoom \\

    // The distance scaled by one factor a notch; past the nearest distance the rest of the step carries the point the
    // view turns about forward along the view instead
    private void zoom(float notches) {

        float distance = viewCameraState.getDistance();
        float zoomed = distance * (float) Math.pow(EditorSetting.VIEW_CAMERA_ZOOM_FACTOR, -notches);

        if (zoomed < EditorSetting.VIEW_CAMERA_DISTANCE_MIN) {

            Vector3 view = viewCameraState.getDirection();
            float dolly = (Math.max(distance, EditorSetting.VIEW_CAMERA_DISTANCE_MIN) - zoomed)
                    * viewCameraSubject.getBoundingRadius();

            viewCameraState.getPan().add(view.x * dolly, view.y * dolly, view.z * dolly);
        }

        viewCameraState.setDistance(
                Math.clamp(zoomed, EditorSetting.VIEW_CAMERA_DISTANCE_MIN, EditorSetting.VIEW_CAMERA_DISTANCE_MAX));
    }

    // Share \\

    // While the window is shown, its view handed to every tab stacked with it sharing the same kind; brought forward
    // or moved to another stack, it first takes the view of the first such tab there
    private void shareStack() {

        ObjectArrayList<TabHandle> stack = context.getWindow().isWithdrawn()
                ? null
                : tabManager.getStackOf(context.getWindow());
        boolean joined = stack != sharedStack;

        this.sharedStack = stack;

        if (stack == null)
            return;

        for (int i = 0; i < stack.size(); i++) {

            ContextPackage stacked = stack.get(i).getTabContext().getContentContext();

            if (stacked == context || !(stacked instanceof ViewCameraShareProvider sharer)
                    || !viewCameraShare.equals(sharer.getViewCameraShare()))
                continue;

            if (joined) {
                viewCameraState.copy(sharer.getViewCameraState());
                return;
            }

            sharer.getViewCameraState().copy(viewCameraState);
        }
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

    // The view faced the way a new camera faces, turned by the initial yaw and pitch
    private void aimInitial() {

        CameraInstance camera = context.getWindow().getActiveCamera();

        rotation.set(EditorSetting.VIEW_CAMERA_INITIAL_YAW_DEGREES, EditorSetting.VIEW_CAMERA_INITIAL_PITCH_DEGREES);
        camera.setRotation(rotation);
        viewCameraState.aim(camera.getDirection());
    }

    // The view turned from the way it faces now, which another window sharing it may have moved since this one drew
    private void rotate(float yawDegrees, float pitchDegrees) {

        CameraInstance camera = context.getWindow().getActiveCamera();

        rotation.set(yawDegrees, pitchDegrees);
        camera.setDirection(viewCameraState.getDirection());
        camera.setRotation(rotation);
        viewCameraState.aim(camera.getDirection());
    }

    // The point the view turns about slid across the view by a drag, as far as the pointer moved across the frame
    // at the distance the camera stands from it, so what is shown stays under the pointer
    private void slide(float deltaX, float deltaY) {

        CameraInstance camera = context.getWindow().getActiveCamera();
        float reach = viewCameraState.getDistance() * viewCameraSubject.getBoundingRadius();
        Vector3 pan = viewCameraState.getPan();
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
        viewCameraState.aim(direction);
        centre();
    }

    // The point the view turns about set back to the middle of what is shown
    public void centre() {
        viewCameraState.getPan().set(0f, 0f, 0f);
        placeCamera();
    }

    // The camera faced along the view and set back from the point it turns about, as far as what is shown is large,
    // its right and up worked out again
    private void placeCamera() {

        CameraInstance camera = context.getWindow().getActiveCamera();

        camera.setDirection(viewCameraState.getDirection());

        Vector3 view = camera.getDirection();

        resolveBasis(view);

        if (!viewCameraSubject.isShown())
            return;

        float reach = viewCameraState.getDistance() * viewCameraSubject.getBoundingRadius();

        target.set(
                viewCameraSubject.getCenter(EngineSetting.AXIS_X),
                viewCameraSubject.getCenter(EngineSetting.AXIS_Y),
                viewCameraSubject.getCenter(EngineSetting.AXIS_Z)).add(viewCameraState.getPan());
        position.set(target).subtract(view.x * reach, view.y * reach, view.z * reach);
        camera.setPosition(position);
        camera.setNearPlane(Math.clamp(
                reach * EditorSetting.VIEW_CAMERA_NEAR_SHARE,
                EditorSetting.VIEW_CAMERA_NEAR_MIN,
                EngineSetting.CAMERA_NEAR_PLANE));
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

    // The view this camera looks through
    public ViewCameraStateStruct getViewCameraState() {
        return viewCameraState;
    }

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
