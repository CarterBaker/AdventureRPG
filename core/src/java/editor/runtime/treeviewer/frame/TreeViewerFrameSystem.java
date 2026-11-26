package editor.runtime.treeviewer.frame;

import editor.bootstrap.treeviewerpipeline.treeviewermanager.TreeViewerManager;
import editor.runtime.treeviewer.TreeViewerSetting;
import editor.runtime.treeviewer.layer.TreeViewerLayerSystem;
import editor.runtime.viewcamera.ViewCameraSubject;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class TreeViewerFrameSystem extends SystemPackage implements ViewCameraSubject {

    /*
     * What this window's camera frames: the layer shown, standing on the
     * origin, halfway up the tallest tree of the preview, held in a sphere as
     * wide as half the diagonal of a square that tree's height across, so the
     * frame holds from one layer to the next and as the tree grows, shrinks
     * or changes species.
     */

    // Internal
    private TreeViewerManager treeViewerManager;
    private TreeViewerLayerSystem treeViewerLayerSystem;

    // Base \\

    @Override
    protected void get() {
        this.treeViewerManager = get(TreeViewerManager.class);
        this.treeViewerLayerSystem = get(TreeViewerLayerSystem.class);
    }

    // Accessible \\

    @Override
    public boolean isShown() {
        return treeViewerManager.getTreeName() != null
                && treeViewerLayerSystem.getShownLayer() != EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    public float getCenter(int axis) {
        return axis == EngineSetting.AXIS_Y
                ? treeViewerManager.getPreviewHeight() * TreeViewerSetting.FRAME_HEIGHT_SHARE
                : 0f;
    }

    @Override
    public float getBoundingRadius() {

        float height = treeViewerManager.getPreviewHeight();

        return 0.5f * (float) Math.hypot(height, height);
    }
}
