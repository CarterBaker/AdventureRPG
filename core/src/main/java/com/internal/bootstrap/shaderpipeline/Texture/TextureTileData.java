package com.internal.bootstrap.shaderpipeline.Texture;

import java.awt.image.BufferedImage;

import com.internal.core.engine.DataPackage;

/*
 * Bootstrap-only container for a single named texture tile. Holds per-alias
 * source images and atlas position during the build phase. Must not be held
 * after bootstrap completes.
 */
public class TextureTileData extends DataPackage {

    // Internal
    private int id;
    private String name;
    private String atlas;

    // Image
    private BufferedImage[] imageLayers;

    // Atlas
    private int atlasX;
    private int atlasY;

    // Internal \\

    public void constructor(int id, String name, String atlas, int aliasCount) {
        this.id = id;
        this.name = name;
        this.atlas = atlas;
        this.imageLayers = new BufferedImage[aliasCount];
    }

    // Image \\

    public void setImage(BufferedImage image, int layer) {
        if (layer < 0 || layer >= imageLayers.length || imageLayers[layer] != null)
            throwException("There was a problem trying to set an image to layer " + layer);
        imageLayers[layer] = image;
    }

    public BufferedImage getImage(int layer) {
        return imageLayers[layer];
    }

    public void clearImages() {
        for (int i = 0; i < imageLayers.length; i++)
            imageLayers[i] = null;
    }

    // Atlas \\

    public void setAtlasPosition(int x, int y) {
        this.atlasX = x;
        this.atlasY = y;
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

    public int getAtlasX() {
        return atlasX;
    }

    public int getAtlasY() {
        return atlasY;
    }
}