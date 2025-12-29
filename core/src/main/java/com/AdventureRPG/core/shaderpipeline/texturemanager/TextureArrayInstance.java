package com.AdventureRPG.core.shaderpipeline.texturemanager;

import java.awt.image.BufferedImage;

import com.AdventureRPG.core.engine.InstanceFrame;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class TextureArrayInstance extends InstanceFrame {

    // Internal
    final int id;
    final String name;
    final int atlasSize;
    private final TextureAtlasInstance[] textureArray;

    // Tiles
    private final Object2ObjectOpenHashMap<String, TextureTileInstance> tileCoordinateMap;

    TextureArrayInstance(
            int id,
            String name,
            int atlasSize,
            TextureAtlasInstance[] textureArray) {

        // Internal
        this.id = id;
        this.name = name;
        this.atlasSize = atlasSize;
        this.textureArray = textureArray;

        // Tiles
        this.tileCoordinateMap = new Object2ObjectOpenHashMap<>();
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

    Object2ObjectOpenHashMap<String, TextureTileInstance> getTileCoordinateMap() {
        return tileCoordinateMap;
    }

    BufferedImage[] getRawImageArray() {

        BufferedImage[] layers = new BufferedImage[textureArray.length];

        for (int i = 0; i < textureArray.length; i++)
            layers[i] = textureArray[i].atlas();

        return layers;
    }
}
