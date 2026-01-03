package com.AdventureRPG.core.shaderpipeline.texturemanager;

import java.awt.image.BufferedImage;

import com.AdventureRPG.core.engine.InstancePackage;

public class TextureTileInstance extends InstancePackage {

    // Internal
    private int id;
    private String name;
    private String atlas;

    // Image
    private BufferedImage[] imageLayers;

    // Atlas
    private int atlasX;
    private int atlasY;

    void constructor(
            int id,
            String name,
            String atlas,
            int aliasCount) {

        // Internal
        this.id = id;
        this.name = name;
        this.atlas = atlas;

        // Image
        this.imageLayers = new BufferedImage[aliasCount];
    }

    // Accessible \\

    int getID() {
        return id;
    }

    String getName() {
        return name;
    }

    String getAtlas() {
        return atlas;
    }

    // Image \\

    void setImage(BufferedImage image, int layer) {

        if (layer < 0 || layer >= imageLayers.length || imageLayers[layer] != null)
            throwException(
                    "There was a problem trying to set an image to later " + layer);

        imageLayers[layer] = image;
    }

    BufferedImage getImage(int layer) {
        return imageLayers[layer];
    }

    // Atlas \\

    public void setAtlasPosition(int x, int y) {
        this.atlasX = x;
        this.atlasY = y;
    }

    public int getAtlasX() {
        return atlasX;
    }

    public int getAtlasY() {
        return atlasY;
    }
}
