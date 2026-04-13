package application.bootstrap.shaderpipeline.texture;

import java.awt.image.BufferedImage;

import engine.util.atlas.AtlasTileData;

public class TextureTileStruct extends AtlasTileData {

    /*
     * Bootstrap container for a single named texture tile. Holds per-alias
     * source images during the build phase. Derives pixel dimensions from the
     * first image assigned — all subsequent alias layers must match that size.
     * GCs with the loader after bootstrap completes.
     * Extends AtlasTileData (external utility) rather than StructPackage —
     * naming exception due to the external base class.
     */

    // Identity
    private final int id;
    private final String name;
    private final String atlas;

    // Images
    private BufferedImage[] imageLayers;

    // Constructor \\

    public TextureTileStruct(int id, String name, String atlas, int aliasCount) {
        this.id = id;
        this.name = name;
        this.atlas = atlas;
        this.imageLayers = new BufferedImage[aliasCount];
    }

    // Management \\

    public void setImage(BufferedImage image, int layer) {

        if (layer < 0 || layer >= imageLayers.length)
            throwException("Layer index out of bounds on tile '" + name + "': " + layer);

        if (imageLayers[layer] != null)
            throwException("Layer " + layer + " already set on tile '" + name + "'");

        if (getTileWidth() == 0)
            setTileDimensions(image.getWidth(), image.getHeight());
        else if (image.getWidth() != getTileWidth() || image.getHeight() != getTileHeight())
            throwException("Alias layer size mismatch on tile '" + name +
                    "': expected " + getTileWidth() + "x" + getTileHeight() +
                    ", got " + image.getWidth() + "x" + image.getHeight());

        imageLayers[layer] = image;
    }

    public BufferedImage getImage(int layer) {
        return imageLayers[layer];
    }

    public void clearImages() {
        for (int i = 0; i < imageLayers.length; i++)
            imageLayers[i] = null;
    }

    // Accessible \\

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    String getAtlas() {
        return atlas;
    }
}