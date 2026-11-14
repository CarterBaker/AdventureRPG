package editor.bootstrap.layerspipeline.layers;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public interface LayersProvider {

    /*
     * A window whose tools work on one layer of a stack at a time, as a
     * painting tool's do. While it is the window worked in last the Layers
     * tab lists its stack, top first, laid out again whenever the revision
     * moves; a click on a layer makes it the one the window's tools work on,
     * a click on a group opens what the group holds, and a click on a
     * layer's eye shows or hides it in the window. The title names the
     * window at the tab's head.
     */

    int getLayerRevision();

    String getLayerTitle();

    void buildLayers(ObjectArrayList<LayerNodeStruct> roots);

    void selectLayer(String layerKey);

    void toggleLayerVisible(String layerKey);
}
