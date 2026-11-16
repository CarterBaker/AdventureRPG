package editor.bootstrap.imagepipeline.imagelayer;

import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import engine.assets.image.Pixmap;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ImageLayerStackStruct extends StructPackage {

    /*
     * A layered image, the one model every editor that paints an image works
     * on, as a painting tool's document is: its size, its layers bottom
     * first, each an image document so every stroke is an undo step, with
     * its name, opacity and whether it is shown, the layer painted, and the
     * flattened result ImageManager lays the layers into. Where the base
     * keeps its own alpha, as a texture map's does, the layers above it only
     * colour it; otherwise every layer is laid over the one below by its
     * alpha, the base included. The layer revision moves whenever the stack,
     * its pick or its eyes do.
     */

    // Size
    private final int width;
    private final int height;
    private final boolean baseAlphaKept;

    // Layers — bottom first; the number the next layer is named by
    private final ObjectArrayList<ImageDocumentInstance> layers;
    private final ObjectArrayList<String> layerNames;
    private final FloatArrayList opacities;
    private final BooleanArrayList visibilities;
    private int activeLayer;
    private int nextLayerNumber;
    private boolean layersChanged;
    private int layerRevision;

    // Composite — the flattened image
    private final Pixmap composite;

    // Constructor \\

    public ImageLayerStackStruct(int width, int height, boolean baseAlphaKept) {

        // Size
        this.width = width;
        this.height = height;
        this.baseAlphaKept = baseAlphaKept;

        // Layers
        this.layers = new ObjectArrayList<>();
        this.layerNames = new ObjectArrayList<>();
        this.opacities = new FloatArrayList();
        this.visibilities = new BooleanArrayList();

        // Composite
        this.composite = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    }

    // Layers \\

    public void addLayer(ImageDocumentInstance layer, String layerName, float opacity) {
        addLayer(layer, layerName, opacity, true);
    }

    public void addLayer(ImageDocumentInstance layer, String layerName, float opacity, boolean visible) {
        layers.add(layer);
        layerNames.add(layerName);
        opacities.add(opacity);
        visibilities.add(visible);
        nextLayerNumber++;
        layerRevision++;
    }

    public ImageDocumentInstance removeLayer(int layerIndex) {
        opacities.removeFloat(layerIndex);
        visibilities.removeBoolean(layerIndex);
        layerNames.remove(layerIndex);
        layerRevision++;
        return layers.remove(layerIndex);
    }

    public void setOpacity(int layerIndex, float opacity) {
        opacities.set(layerIndex, opacity);
    }

    public void setLayerName(int layerIndex, String layerName) {
        layerNames.set(layerIndex, layerName);
        layerRevision++;
    }

    public void setActiveLayer(int activeLayer) {
        this.activeLayer = activeLayer;
        layerRevision++;
    }

    public void setLayerVisible(int layerIndex, boolean visible) {
        visibilities.set(layerIndex, visible);
        layerRevision++;
    }

    public void setNextLayerNumber(int nextLayerNumber) {
        this.nextLayerNumber = nextLayerNumber;
    }

    public void markLayersChanged(boolean layersChanged) {
        this.layersChanged = layersChanged;
    }

    // Whether anything painted or layered has not yet been saved
    public boolean isDirty() {

        if (layersChanged)
            return true;

        for (int i = 0; i < layers.size(); i++)
            if (layers.get(i).isDirty())
                return true;

        return false;
    }

    // Accessible \\

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    // True where the base keeps its own alpha and the layers above only colour it
    public boolean isBaseAlphaKept() {
        return baseAlphaKept;
    }

    public int getLayerCount() {
        return layers.size();
    }

    public ImageDocumentInstance getLayer(int layerIndex) {
        return layers.get(layerIndex);
    }

    public String getLayerName(int layerIndex) {
        return layerNames.get(layerIndex);
    }

    public float getOpacity(int layerIndex) {
        return opacities.getFloat(layerIndex);
    }

    public boolean isLayerVisible(int layerIndex) {
        return visibilities.getBoolean(layerIndex);
    }

    public int getLayerRevision() {
        return layerRevision;
    }

    public int getActiveLayer() {
        return activeLayer;
    }

    public int getNextLayerNumber() {
        return nextLayerNumber;
    }

    public boolean isLayersChanged() {
        return layersChanged;
    }

    public Pixmap getComposite() {
        return composite;
    }
}
