package com.internal.bootstrap.shaderpipeline.Texture;

import java.awt.image.BufferedImage;

import com.internal.core.util.atlas.AtlasTileData;

/*
 * Bootstrap-only container for a single named texture tile. Holds per-alias
 * source images during the build phase. Derives its own pixel dimensions from
 * the first image assigned — all subsequent alias layers for the same tile
 * must match that size exactly. Must not be held after bootstrap completes.
 */
public class TextureTileData extends AtlasTileData {

    // Internal
    private int id;
    private String name;
    private String atlas;

    // Image
    private BufferedImage[] imageLayers;

    // Internal \\

    public void constructor(int id, String name, String atlas, int aliasCount) {
        this.id = id;
        this.name = name;
        this.atlas = atlas;
        this.imageLayers = new BufferedImage[aliasCount];
    }

    // Image \\

    public void setImage(BufferedImage image, int layer) {

        if (layer < 0 || layer >= imageLayers.length)
            throwException("Layer index out of bounds on tile '" + name + "': " + layer);
        if (imageLayers[layer] != null)
            throwException("Layer " + layer + " already set on tile '" + name + "'");

        if (getTileWidth() == 0)
            setTileDimensions(image.getWidth(), image.getHeight());
        else if (image.getWidth() != getTileWidth() || image.getHeight() != getTileHeight())
            throwException("Alias layer size mismatch on tile '" + name
                    + "': expected " + getTileWidth() + "x" + getTileHeight()
                    + ", got " + image.getWidth() + "x" + image.getHeight());

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