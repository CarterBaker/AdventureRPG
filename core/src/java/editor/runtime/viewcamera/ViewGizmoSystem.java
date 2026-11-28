package editor.runtime.viewcamera;

import java.util.Arrays;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class ViewGizmoSystem extends SystemPackage {

    /*
     * The axis gizmo in the corner of a 3D viewport, as Blender's navigation
     * gizmo and Unity's scene gizmo turn with the view: every axis a bubble
     * where it points on screen, the positive ones lettered discs joined to
     * the middle by an arm in the axis's colour, the negative ones rings; a
     * bubble clicked looks back along its axis (ViewCameraSystem.snap), or
     * along the opposite one when the view already looks that way, and its
     * middle centres the view (ViewCameraSystem.centre). Every
     * viewport that creates a ViewCameraSystem creates this beside it, after
     * the menu system that gives its window a menu target. Each bubble and
     * arm moves only when it crosses a step, to positions and lengths made
     * once, so the gizmo costs nothing while the view rests. While the view
     * camera has nothing shown to frame the gizmo is hidden with it.
     */

    // Internal
    private MenuManager menuManager;
    private ViewCameraSystem viewCameraSystem;

    // Menus
    private MenuInstance gizmoMenu;
    private boolean gizmoShown;

    // Steps — every step's position, x step then y step, each arm length in pixels, and the step, length and turn each
    // bubble and arm was last put at
    private DimensionVector2Struct[] stepPositions;
    private DimensionVector2Struct[] armSizes;
    private int[] bubbleSteps;
    private int[] armSteps;
    private int[] armLengths;
    private int[] armDegrees;

    // Base \\

    @Override
    protected void create() {

        int steps = EditorSetting.VIEW_GIZMO_STEPS;
        float middle = (steps - 1) * 0.5f;
        float pitch = EditorSetting.VIEW_GIZMO_RADIUS_PIXELS / middle;
        int armCount = EditorSetting.VIEW_GIZMO_AXES.length / 2;
        int lengthCount = Math.round(EditorSetting.VIEW_GIZMO_RADIUS_PIXELS) + 1;

        // Steps
        this.stepPositions = new DimensionVector2Struct[steps * steps];
        this.armSizes = new DimensionVector2Struct[lengthCount];
        this.bubbleSteps = new int[EditorSetting.VIEW_GIZMO_AXES.length];
        this.armSteps = new int[armCount];
        this.armLengths = new int[armCount];
        this.armDegrees = new int[armCount];

        for (int y = 0; y < steps; y++)
            for (int x = 0; x < steps; x++)
                stepPositions[x + y * steps] = new DimensionVector2Struct(
                        DimensionValueStruct.ofAbsolute((x - middle) * pitch),
                        DimensionValueStruct.ofAbsolute((y - middle) * pitch));

        for (int length = 0; length < lengthCount; length++)
            armSizes[length] = new DimensionVector2Struct(
                    DimensionValueStruct.ofAbsolute(length),
                    DimensionValueStruct.ofAbsolute(EditorSetting.VIEW_GIZMO_ARM_PIXELS));

        Arrays.fill(bubbleSteps, EngineSetting.INDEX_NOT_FOUND);
        Arrays.fill(armSteps, EngineSetting.INDEX_NOT_FOUND);
        Arrays.fill(armLengths, EngineSetting.INDEX_NOT_FOUND);
        Arrays.fill(armDegrees, EngineSetting.INDEX_NOT_FOUND);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
    }

    @Override
    protected void awake() {
        this.gizmoMenu = menuManager.openMenu(EditorSetting.VIEW_GIZMO_MENU, context.getWindow());
        this.gizmoShown = true;
    }

    // Update \\

    @Override
    protected void update() {

        if (!showWithSubject())
            return;

        Vector3 right = viewCameraSystem.getRight();
        Vector3 up = viewCameraSystem.getUp();

        for (int i = 0; i < EditorSetting.VIEW_GIZMO_AXES.length; i++) {

            float[] axis = EditorSetting.VIEW_GIZMO_AXIS_VECTORS[i];
            float screenX = dot(axis, right);
            float screenY = dot(axis, up);

            placeBubble(i, screenX, screenY);

            if (i % 2 == 0)
                placeArm(i / 2, screenX, screenY);
        }
    }

    // The gizmo shown only while the view camera frames something, false while hidden
    private boolean showWithSubject() {

        boolean shown = viewCameraSystem.isSubjectShown();

        if (shown != gizmoShown) {

            if (shown)
                gizmoMenu.show();
            else
                gizmoMenu.hide();

            this.gizmoShown = shown;
        }

        return shown;
    }

    // An axis's bubble moved to the step nearest where it points, only when that step changes
    private void placeBubble(int axis, float screenX, float screenY) {

        int step = resolveStep(screenX, screenY);

        if (step == bubbleSteps[axis])
            return;

        ElementInstance bubble = gizmoMenu.getEntryPoint(EditorSetting.VIEW_GIZMO_ENTRY_BUBBLES + axis);

        if (bubble == null)
            return;

        bubbleSteps[axis] = step;
        bubble.setPositionOverride(stepPositions[step]);
    }

    // A positive axis's arm laid from the middle to its bubble: centred halfway, as long as the axis reaches on
    // screen and turned to it, each only when its step changes
    private void placeArm(int arm, float screenX, float screenY) {

        ElementInstance element = gizmoMenu.getEntryPoint(EditorSetting.VIEW_GIZMO_ENTRY_ARMS + arm);

        if (element == null)
            return;

        int step = resolveStep(screenX * 0.5f, screenY * 0.5f);
        int length = Math.min(
                Math.round((float) Math.sqrt(screenX * screenX + screenY * screenY)
                        * EditorSetting.VIEW_GIZMO_RADIUS_PIXELS),
                armSizes.length - 1);
        int degrees = Math.round((float) Math.toDegrees(Math.atan2(screenY, screenX)));

        if (step != armSteps[arm]) {
            armSteps[arm] = step;
            element.setPositionOverride(stepPositions[step]);
        }

        if (length != armLengths[arm]) {
            armLengths[arm] = length;
            element.setSizeOverride(armSizes[length]);
        }

        if (degrees != armDegrees[arm]) {
            armDegrees[arm] = degrees;
            element.setRotation(degrees);
        }
    }

    // The step nearest a point across the gizmo, from -1 to 1 either way
    private int resolveStep(float x, float y) {

        int steps = EditorSetting.VIEW_GIZMO_STEPS;
        float middle = (steps - 1) * 0.5f;
        int stepX = Math.clamp(Math.round(middle + x * middle), 0, steps - 1);
        int stepY = Math.clamp(Math.round(middle + y * middle), 0, steps - 1);

        return stepX + stepY * steps;
    }

    private float dot(float[] axis, Vector3 vector) {
        return axis[EngineSetting.AXIS_X] * vector.x + axis[EngineSetting.AXIS_Y] * vector.y
                + axis[EngineSetting.AXIS_Z] * vector.z;
    }

    // Menu Events \\

    // A bubble clicked: the view looks back along its axis, or along the opposite one when it already does
    public void view(String axisName) {

        int axis = findAxis(axisName);

        if (axis == EngineSetting.INDEX_NOT_FOUND)
            return;

        float[] vector = EditorSetting.VIEW_GIZMO_AXIS_VECTORS[axis];
        Vector3 direction = viewCameraSystem.getViewCameraState().getDirection();

        if (-dot(vector, direction) >= EditorSetting.VIEW_GIZMO_FACING_COSINE)
            vector = EditorSetting.VIEW_GIZMO_AXIS_VECTORS[axis ^ 1];

        viewCameraSystem.snap(vector);
    }

    // The middle clicked: the point the view turns about set back to the middle of what is shown
    public void centre() {
        viewCameraSystem.centre();
    }

    private int findAxis(String axisName) {

        for (int i = 0; i < EditorSetting.VIEW_GIZMO_AXES.length; i++)
            if (EditorSetting.VIEW_GIZMO_AXES[i].equals(axisName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }
}
