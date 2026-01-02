package com.AdventureRPG.core.shaderpipeline.texturemanager;

import java.awt.image.BufferedImage;

import com.AdventureRPG.core.engine.InstancePackage;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class TextureArrayInstance extends InstancePackage {

    // Internal
    private int id;
    private String name;
    private int atlasSize;
    private TextureAtlasInstance[] textureArray;

    // Tiles
    private Object2ObjectOpenHashMap<String, TextureTileInstance> tileCoordinateMap;

    void init(
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

    // Accessible \\

    int getID() {
        return id;
    }

    String getName() {
        return name;
    }

    int getAtlasSize() {
        return atlasSize;
    }

    // Tiles \\

    void createTile(int x, int y, TextureTileInstance tile) {
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
            layers[i] = textureArray[i].getAtlas();

        return layers;
    }
}
