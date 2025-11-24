package com.AdventureRPG.Core.RenderPipeline.TextureManager;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import com.AdventureRPG.Core.Bootstrap.InstanceFrame;

class TextureArrayInstance extends InstanceFrame {

    // Internal
    final int id;
    final int atlasSize;
    private final TextureAtlasInstance[] textureArray;

    // Tiles
    private final Map<String, TextureTileInstance> tileCoordinateMap;

    TextureArrayInstance(
            int id,
            int atlasSize,
            TextureAtlasInstance[] textureArray) {

        // Internal
        this.id = id;
        this.atlasSize = atlasSize;
        this.textureArray = textureArray;

        // Tiles
        this.tileCoordinateMap = new HashMap<>();
    }

    // Tiles \\

    void registerTile(int x, int y, TextureTileInstance tile) {
        String key = x + "," + y;
        tileCoordinateMap.put(key, tile);
    }

    TextureTileInstance getTileAt(int x, int y) {
        String key = x + "," + y;
        return tileCoordinateMap.get(key);
    }

    Map<String, TextureTileInstance> getTileCoordinateMap() {
        return tileCoordinateMap;
    }

    BufferedImage[] getRawImageArray() {

        BufferedImage[] layers = new BufferedImage[textureArray.length];

        for (int i = 0; i < textureArray.length; i++)
            layers[i] = textureArray[i].atlas();

        return layers;
    }
}
