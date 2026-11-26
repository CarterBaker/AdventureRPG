package editor.runtime.treeviewer.layer;

import java.util.Arrays;

import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.treeplannerpipeline.treeplanner.TreePreviewTierStruct;
import editor.bootstrap.treeplannerpipeline.treeplannermanager.TreePlannerManager;
import editor.runtime.treeviewer.TreeViewerSetting;
import editor.runtime.treeviewer.frame.TreeViewerFrameSystem;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreeViewerLayerSystem extends SystemPackage {

    /*
     * The Tree Viewer's layers, one for every detail the tree is drawn at, as
     * a game engine's mesh viewer lists its levels of detail: Full, Model,
     * Sketch and Outline, top first, each with an eye. A layer picked is the
     * one the view frames; a layer hidden leaves the row, the others keeping
     * their places as a hidden layer leaves the layers beside it, and the view
     * frames every layer shown while none is picked or the one picked is
     * hidden. Layers are keyed by their detail's name, so the choice holds as
     * the row is built again.
     */

    // Internal
    private TreePlannerManager treePlannerManager;
    private TreeViewerFrameSystem treeViewerFrameSystem;
    private ViewCameraSystem viewCameraSystem;

    // Layers — the detail picked, which are shown, and the revision that moves with either
    private int pickedTier;
    private boolean[] tier2Shown;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Layers
        this.pickedTier = TreeViewerSetting.FRAME_ALL;
        this.tier2Shown = new boolean[TreeViewerSetting.TIER_NAMES.length];
        Arrays.fill(tier2Shown, true);
    }

    @Override
    protected void get() {
        this.treePlannerManager = get(TreePlannerManager.class);
        this.treeViewerFrameSystem = get(TreeViewerFrameSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
    }

    // Layers \\

    // Every tier of the row, top first, the one picked active, each with its eye
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {

        for (int i = 0; i < treePlannerManager.getPreviewTierCount(); i++) {

            TreePreviewTierStruct tier = treePlannerManager.getPreviewTier(i);
            int detail = tier.getDetail().ordinal();

            roots.add(new LayerNodeStruct(
                    ArpgUtility.toEnumName(tier.getDetail()),
                    TreeViewerSetting.TIER_NAMES[detail],
                    i == pickedTier,
                    true,
                    tier2Shown[detail],
                    false));
        }
    }

    // A tier picked: the view framed on it, centred
    public void select(String layerKey) {

        int tier = findTier(layerKey);

        if (tier == EngineSetting.INDEX_NOT_FOUND)
            return;

        this.pickedTier = tier;
        revision++;
        reframe();
    }

    // A tier's eye pressed: shown or hidden, the view framed again
    public void toggleShown(String layerKey) {

        int tier = findTier(layerKey);

        if (tier == EngineSetting.INDEX_NOT_FOUND)
            return;

        int detail = treePlannerManager.getPreviewTier(tier).getDetail().ordinal();

        tier2Shown[detail] = !tier2Shown[detail];
        revision++;
        reframe();
    }

    private void reframe() {
        treeViewerFrameSystem.frame(isShown(pickedTier) ? pickedTier : TreeViewerSetting.FRAME_ALL);
        viewCameraSystem.centre();
    }

    private int findTier(String layerKey) {

        for (int i = 0; i < treePlannerManager.getPreviewTierCount(); i++)
            if (ArpgUtility.toEnumName(treePlannerManager.getPreviewTier(i).getDetail()).equals(layerKey))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Accessible \\

    // Whether a tier of the row is shown; FRAME_ALL and tiers past the row are not tiers
    public boolean isShown(int tier) {
        return tier >= 0
                && tier < treePlannerManager.getPreviewTierCount()
                && tier2Shown[treePlannerManager.getPreviewTier(tier).getDetail().ordinal()];
    }

    public int getRevision() {
        return revision + treePlannerManager.getPreviewRevision();
    }
}
