package editor.runtime.menueventsmanager.menus;

import editor.bootstrap.layerspipeline.layersmanager.LayersManager;
import engine.root.BranchPackage;

public class LayersBranch extends BranchPackage {

    /*
     * Menu event handlers for the Layers tab's rows. Each hands back the key
     * of the layer it shows: a row picks its layer for the window worked in
     * last, a group's marker folds or opens it, and a layer's eye shows or
     * hides it.
     */

    // Internal
    private LayersManager layersManager;

    // Base \\

    @Override
    protected void get() {
        this.layersManager = get(LayersManager.class);
    }

    // Layers \\

    public void selectLayer(String layerKey) {
        layersManager.selectLayer(layerKey);
    }

    public void toggleLayer(String layerKey) {
        layersManager.toggleExpanded(layerKey);
    }

    public void toggleVisible(String layerKey) {
        layersManager.toggleLayerVisible(layerKey);
    }
}
