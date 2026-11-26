package editor.runtime.treeviewer.layer;

import java.util.Arrays;

import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.treeviewerpipeline.treeviewermanager.TreeViewerManager;
import editor.runtime.EditorSetting;
import editor.runtime.treeviewer.TreeViewerSetting;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreeViewerLayerSystem extends SystemPackage {

    /*
     * The Tree Viewer's layers, one for every mesh level the world draws a
     * tree at before its impostor stage, as a game engine's mesh viewer lists
     * its levels of detail, and last that stage itself, the impostor baked
     * from the shared tree the species' LOD seed grows in full, each with an
     * eye.
     * Every layer stands on the same root, so the viewer draws only the
     * topmost layer whose eye is open, as an image editor shows the top layer
     * over those beneath; levels from the shared level on are the impostor
     * stage's and not listed. The layer picked is
     * the Layers tab's choice and nothing more. Layers are keyed by stable
     * names, so the choice holds as the preview is built again.
     */

    // Internal
    private TreeViewerManager treeViewerManager;
    private ViewCameraSystem viewCameraSystem;

    // Layers — the layer picked, which are shown, and the revision that moves with either
    private int pickedLayer;
    private boolean[] layer2Shown;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Layers
        this.pickedLayer = EngineSetting.INDEX_NOT_FOUND;
        this.layer2Shown = new boolean[EditorSetting.TREE_VIEWER_LAYER_COUNT];
        Arrays.fill(layer2Shown, true);
    }

    @Override
    protected void get() {
        this.treeViewerManager = get(TreeViewerManager.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
    }

    // Layers \\

    // Every listed layer, finest first, the one picked active, each with its eye
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {
        for (int layer = 0; layer < layer2Shown.length; layer++)
            if (isListed(layer))
                roots.add(new LayerNodeStruct(
                        toKey(layer), getName(layer), layer == pickedLayer, true, layer2Shown[layer], false));
    }

    // Every mesh level before the impostor stage, and the impostor
    private boolean isListed(int layer) {
        return layer == EditorSetting.TREE_VIEWER_LAYER_SHARED || layer < treeViewerManager.getSharedLevel();
    }

    public void select(String layerKey) {

        int layer = findLayer(layerKey);

        if (layer == EngineSetting.INDEX_NOT_FOUND)
            return;

        this.pickedLayer = layer;
        revision++;
    }

    // A layer's eye pressed: shown or hidden, the view centred on what is shown now
    public void toggleShown(String layerKey) {

        int layer = findLayer(layerKey);

        if (layer == EngineSetting.INDEX_NOT_FOUND)
            return;

        layer2Shown[layer] = !layer2Shown[layer];
        revision++;
        viewCameraSystem.centre();
    }

    private String toKey(int layer) {
        return layer == EditorSetting.TREE_VIEWER_LAYER_SHARED
                ? TreeViewerSetting.LAYER_KEY_SHARED
                : TreeViewerSetting.LAYER_KEY_LEVEL + layer;
    }

    private int findLayer(String layerKey) {

        for (int layer = 0; layer < layer2Shown.length; layer++)
            if (toKey(layer).equals(layerKey))
                return layer;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Accessible \\

    // The topmost layer whose eye is open, INDEX_NOT_FOUND while every one is shut
    public int getShownLayer() {

        for (int layer = 0; layer < layer2Shown.length; layer++)
            if (layer2Shown[layer] && isListed(layer))
                return layer;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    public String getName(int layer) {

        if (layer == EditorSetting.TREE_VIEWER_LAYER_SHARED)
            return TreeViewerSetting.LAYER_NAME_SHARED;

        if (layer == EngineSetting.TREE_LOD_FULL)
            return TreeViewerSetting.LAYER_NAME_FULL;

        return TreeViewerSetting.LAYER_NAME_LEVEL + layer;
    }

    public int getRevision() {
        return revision + treeViewerManager.getPreviewRevision() + treeViewerManager.getRevision();
    }
}
