package editor.runtime.treeviewer.frame;

import editor.bootstrap.treeplannerpipeline.treeplannermanager.TreePlannerManager;
import editor.runtime.treeviewer.TreeViewerSetting;
import editor.runtime.treeviewer.layer.TreeViewerLayerSystem;
import editor.runtime.viewcamera.ViewCameraSubject;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class TreeViewerFrameSystem extends SystemPackage implements ViewCameraSubject {

    /*
     * What this window's camera frames: the tier picked in the Layers tab, or
     * every tier shown, halfway up the tree, held in a sphere as wide as half
     * the diagonal of what is framed — the tree's height across for one tier,
     * the span from the first tier shown to the last for the row — so the
     * frame holds as the tree grows, shrinks or changes species.
     */

    // Internal
    private TreePlannerManager treePlannerManager;
    private TreeViewerLayerSystem treeViewerLayerSystem;

    // Frame
    private int framedTier;

    // Base \\

    @Override
    protected void create() {

        // Frame
        this.framedTier = TreeViewerSetting.FRAME_ALL;
    }

    @Override
    protected void get() {
        this.treePlannerManager = get(TreePlannerManager.class);
        this.treeViewerLayerSystem = get(TreeViewerLayerSystem.class);
    }

    // Frame \\

    // The tier framed from now on, FRAME_ALL for every tier shown
    public void frame(int tier) {
        this.framedTier = tier >= 0 && tier < treePlannerManager.getPreviewTierCount()
                ? tier
                : TreeViewerSetting.FRAME_ALL;
    }

    private boolean isWhole() {
        return framedTier == TreeViewerSetting.FRAME_ALL;
    }

    // The span from the first tier shown to the last, along the row
    private float getRowLength() {
        return getShownEnd() - getShownStart();
    }

    private float getShownStart() {

        for (int i = 0; i < treePlannerManager.getPreviewTierCount(); i++)
            if (treeViewerLayerSystem.isShown(i))
                return treePlannerManager.getPreviewTierOffsetX(i);

        return 0f;
    }

    private float getShownEnd() {

        for (int i = treePlannerManager.getPreviewTierCount() - 1; i >= 0; i--)
            if (treeViewerLayerSystem.isShown(i))
                return treePlannerManager.getPreviewTierOffsetX(i);

        return 0f;
    }

    // Accessible \\

    @Override
    public boolean isShown() {
        return treePlannerManager.getPreviewTierCount() > 0;
    }

    @Override
    public float getCenter(int axis) {
        return switch (axis) {
            case EngineSetting.AXIS_X -> isWhole()
                    ? 0.5f * (getShownStart() + getShownEnd())
                    : treePlannerManager.getPreviewTierOffsetX(framedTier);
            case EngineSetting.AXIS_Y -> treePlannerManager.getPreviewHeight() * TreeViewerSetting.FRAME_HEIGHT_SHARE;
            default -> 0f;
        };
    }

    @Override
    public float getBoundingRadius() {

        float height = treePlannerManager.getPreviewHeight();
        float width = isWhole() ? Math.max(height, getRowLength()) : height;

        return 0.5f * (float) Math.hypot(width, height);
    }
}
