package editor.bootstrap.layerspipeline.layers;

public interface LayerViewsProvider {

    /*
     * A window whose layer stack can be listed more than one way, as a
     * docker's footer switches its views. While it is the window worked in
     * last the Layers tab shows a button for each view at its foot, the one
     * shown marked; a press shows that view, after which the window lists its
     * stack, its title and its New Layer and Delete Layer actions for it.
     */

    int getLayerViewCount();

    String getLayerViewLabel(int view);

    int getLayerView();

    void showLayerView(int view);
}
