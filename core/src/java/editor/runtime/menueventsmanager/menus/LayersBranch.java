package editor.runtime.menueventsmanager.menus;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.layerspipeline.layersmanager.LayersManager;
import engine.root.BranchPackage;

public class LayersBranch extends BranchPackage {

    /*
     * Menu event handlers for the Layers tab's rows. Each hands back the key
     * of the layer it shows: a row picks its layer for the window worked in
     * last, or a draggable layer's row carries it out of the tab, a group's marker folds or opens it, and a layer's eye shows or
     * hides it; the footer's icons add a layer and delete the one picked, and
     * its view buttons hand back the view they show.
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

    public void dragLayer(String layerKey, WindowInstance window) {
        layersManager.dragLayer(layerKey, window);
    }

    public void toggleLayer(String layerKey) {
        layersManager.toggleExpanded(layerKey);
    }

    public void toggleVisible(String layerKey) {
        layersManager.toggleLayerVisible(layerKey);
    }

    public void addLayer() {
        layersManager.addLayer();
    }

    public void removeLayer() {
        layersManager.removeLayer();
    }

    public void showView(String view) {
        layersManager.showLayerView(Integer.parseInt(view));
    }
}
