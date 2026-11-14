package editor.bootstrap.charactereditorpipeline.charactermesh;

import java.io.File;

import application.bootstrap.shaderpipeline.texturemanager.AliasStruct;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import engine.assets.image.Pixmap;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterPaintCanvasStruct extends StructPackage {

    /*
     * One map of one character texture open for painting: the tile it
     * paints and the alias whose map and channels it fills, the image file
     * it saves to beside the texture, its paint layers — the first the image
     * itself, each above it laid over it by its own alpha and opacity, each
     * an image document so every stroke is an undo step — and the flattened
     * result the tile is baked from while it is painted.
     */

    // Identity
    private final String textureName;
    private final int mapIndex;
    private final File file;

    // Tile — the tile painted, the alias the map is, and the map's size
    private final int tileID;
    private final AliasStruct alias;
    private final int width;
    private final int height;
    private final int defaultColor;

    // Layers — bottom first, the image itself the base; the number the next layer is named by
    private final ObjectArrayList<ImageDocumentInstance> layers;
    private final ObjectArrayList<String> layerNames;
    private final FloatArrayList opacities;
    private int activeLayer;
    private int nextLayerNumber;
    private boolean layersChanged;

    // Composite — the flattened map
    private final Pixmap composite;

    // Constructor \\

    public CharacterPaintCanvasStruct(
            String textureName,
            int mapIndex,
            File file,
            int tileID,
            AliasStruct alias,
            int width,
            int height,
            int defaultColor) {

        // Identity
        this.textureName = textureName;
        this.mapIndex = mapIndex;
        this.file = file;

        // Tile
        this.tileID = tileID;
        this.alias = alias;
        this.width = width;
        this.height = height;
        this.defaultColor = defaultColor;

        // Layers
        this.layers = new ObjectArrayList<>();
        this.layerNames = new ObjectArrayList<>();
        this.opacities = new FloatArrayList();

        // Composite
        this.composite = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    }

    // Layers \\

    public void addLayer(ImageDocumentInstance layer, String layerName, float opacity) {
        layers.add(layer);
        layerNames.add(layerName);
        opacities.add(opacity);
        nextLayerNumber++;
    }

    public ImageDocumentInstance removeLayer(int layerIndex) {

        opacities.removeFloat(layerIndex);
        layerNames.remove(layerIndex);

        return layers.remove(layerIndex);
    }

    public void setOpacity(int layerIndex, float opacity) {
        opacities.set(layerIndex, opacity);
    }

    public void setActiveLayer(int activeLayer) {
        this.activeLayer = activeLayer;
    }

    public void markLayersChanged(boolean layersChanged) {
        this.layersChanged = layersChanged;
    }

    // Whether anything painted or layered has not yet been written to the image
    public boolean isDirty() {

        if (layersChanged)
            return true;

        for (int i = 0; i < layers.size(); i++)
            if (layers.get(i).isDirty())
                return true;

        return false;
    }

    // Accessible \\

    public String getTextureName() {
        return textureName;
    }

    public int getMapIndex() {
        return mapIndex;
    }

    public File getFile() {
        return file;
    }

    public int getTileID() {
        return tileID;
    }

    public AliasStruct getAlias() {
        return alias;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    // What the map holds where nothing paints it, RGBA8888
    public int getDefaultColor() {
        return defaultColor;
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

    public int getActiveLayer() {
        return activeLayer;
    }

    public int getNextLayerNumber() {
        return nextLayerNumber;
    }

    public Pixmap getComposite() {
        return composite;
    }

}
