package com.AdventureRPG.core.shaderpipeline.texturemanager;

import java.awt.image.BufferedImage;

import com.AdventureRPG.core.kernel.InstanceFrame;
import com.AdventureRPG.core.util.Exceptions.GraphicException;

class TextureTileInstance extends InstanceFrame {

    // Internal
    final int id;
    final String name;
    final String atlas;

    // Image
    private final BufferedImage[] imageLayers;

    // Atlas
    private int atlasX;
    private int atlasY;

    TextureTileInstance(
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

    // Image \\

    void setImage(BufferedImage image, int layer) {

        if (layer < 0 || layer >= imageLayers.length || imageLayers[layer] != null)
            throw new GraphicException.TextureAliasException(
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
