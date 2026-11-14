package application.bootstrap.shaderpipeline.texture;

import java.awt.image.BufferedImage;

import engine.assets.atlas.AtlasTileData;

public class TextureTileStruct extends AtlasTileData {

    /*
     * Bootstrap container for a single named font glyph tile packed into its
     * font's atlas. Holds its layer images while the atlas is composited and
     * derives its pixel dimensions from the first image assigned — every
     * later layer must match that size. GCs with the font builder once the
     * atlas is uploaded.
     * Extends AtlasTileData (external utility) rather than StructPackage —
     * naming exception due to the external base class.
     */

    // Identity
    private final String name;
    private final String atlas;

    // Images
    private BufferedImage[] imageLayers;

    // Constructor \\

    public TextureTileStruct(String name, String atlas, int aliasCount) {
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

    public String getName() {
        return name;
    }

    String getAtlas() {
        return atlas;
    }
}
