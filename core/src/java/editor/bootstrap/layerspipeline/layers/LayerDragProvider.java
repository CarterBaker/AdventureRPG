package editor.bootstrap.layerspipeline.layers;

import application.kernel.windowpipeline.window.WindowInstance;

public interface LayerDragProvider {

    /*
     * A window some of whose layers are picked up and carried out of the
     * Layers tab, as a bin's clips are dragged onto a timeline. Every frame a
     * draggable layer's row is held, the key of the layer pressed and the
     * Layers tab's window are handed here, from the press until it is let
     * go; a layer let go where it was pressed is chosen.
     */

    void dragLayer(String layerKey, WindowInstance window);
}
