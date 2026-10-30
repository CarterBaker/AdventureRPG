package editor.runtime.treeviewer.camera;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.treeplannerpipeline.treeplannermanager.TreePlannerManager;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.treeviewer.TreeViewerSetting;
import engine.assets.camera.CameraInstance;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class TreeViewerCameraSystem extends SystemPackage {

    /*
     * Orbits this window's camera around the tier it frames, or the middle of
     * the whole row, halfway up the tree. The orbit button turns the view and
     * scrolling zooms, the distance measured in sizes of what is framed — the
     * tree's height for one tier, the row's length for the whole — so the
     * frame holds as the tree grows, shrinks or changes species.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private TreePlannerManager treePlannerManager;

    // Orbit
    private Vector3 target;
    private Vector3 position;
    private Vector2 rotation;
    private float distance;
    private int framedTier;

    // Base \\

    @Override
    protected void create() {

        // Orbit
        this.target = new Vector3();
        this.position = new Vector3();
        this.rotation = new Vector2();
        this.distance = TreeViewerSetting.ORBIT_DISTANCE_DEFAULT;
        this.framedTier = TreeViewerSetting.FRAME_ALL;
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.treePlannerManager = get(TreePlannerManager.class);
    }

    @Override
    protected void awake() {

        rotate(TreeViewerSetting.ORBIT_INITIAL_YAW_DEGREES, TreeViewerSetting.ORBIT_INITIAL_PITCH_DEGREES);
        placeCamera();
    }

    // Update \\

    @Override
    protected void update() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (rawInput.isButtonHeld(TreeViewerSetting.BUTTON_ORBIT))
            rotate(rawInput.getDeltaX(), rawInput.getDeltaY());

        distance = Math.clamp(
                distance - rawInput.getScrollY() * TreeViewerSetting.ORBIT_ZOOM_STEP,
                TreeViewerSetting.ORBIT_DISTANCE_MIN,
                TreeViewerSetting.ORBIT_DISTANCE_MAX);

        placeCamera();
    }

    // Orbit \\

    // The tier framed from now on, FRAME_ALL for the whole row
    public void frame(int tier) {
        this.framedTier = tier >= 0 && tier < treePlannerManager.getPreviewTierCount()
                ? tier
                : TreeViewerSetting.FRAME_ALL;
    }

    private void rotate(float yawDegrees, float pitchDegrees) {

        rotation.set(yawDegrees, pitchDegrees);
        context.getWindow().getActiveCamera().setRotation(rotation);
    }

    // The camera set back from the frame's centre along its view, as far as what is framed is large
    private void placeCamera() {

        CameraInstance camera = context.getWindow().getActiveCamera();
        Vector3 direction = camera.getDirection();
        float height = treePlannerManager.getPreviewHeight();
        int tiers = treePlannerManager.getPreviewTierCount();
        boolean whole = framedTier == TreeViewerSetting.FRAME_ALL;
        float rowLength = treePlannerManager.getPreviewTierOffsetX(tiers - 1)
                - treePlannerManager.getPreviewTierOffsetX(0);
        float size = whole ? Math.max(height, rowLength) : height;
        float reach = distance * size;

        target.set(
                whole ? 0f : treePlannerManager.getPreviewTierOffsetX(framedTier),
                height * TreeViewerSetting.ORBIT_TARGET_HEIGHT_SHARE,
                0f);
        position.set(target).subtract(direction.x * reach, direction.y * reach, direction.z * reach);
        camera.setPosition(position);
    }
}
