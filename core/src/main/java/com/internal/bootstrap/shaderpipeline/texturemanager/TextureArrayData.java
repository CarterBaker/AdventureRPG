package com.internal.bootstrap.shaderpipeline.texturemanager;

import java.awt.image.BufferedImage;

import com.internal.core.engine.DataPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Bootstrap-only container grouping all alias layers for one texture array
 * directory alongside the tile coordinate map used for UV registration.
 * Atlas image data is cleared after GPU upload. Must not be held after
 * bootstrap completes.
 */
public class TextureArrayData extends DataPackage {

    // Internal
    private int id;
    private String name;
    private int atlasSize;
    private TextureAtlasData[] textureArray;

    // Tiles
    private Object2ObjectOpenHashMap<String, TextureTileData> tileCoordinateMap;

    // Internal \\

    void constructor(int id, String name, int atlasSize, TextureAtlasData[] textureArray) {
        this.id = id;
        this.name = name;
        this.atlasSize = atlasSize;
        this.textureArray = textureArray;
        this.tileCoordinateMap = new Object2ObjectOpenHashMap<>();
    }

    // Disposal \\

    void clearAtlases() {
        for (int i = 0; i < textureArray.length; i++) {
            textureArray[i].clearImage();
            textureArray[i] = null;
        }
    }

    // Tiles \\

    void createTile(int x, int y, TextureTileData tile) {
        tileCoordinateMap.put(x + "," + y, tile);
    }

    TextureTileData getTileAt(int x, int y) {
        return tileCoordinateMap.get(x + "," + y);
    }

    Object2ObjectOpenHashMap<String, TextureTileData> getTileCoordinateMap() {
        return tileCoordinateMap;
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

    BufferedImage[] getRawImageArray() {
        BufferedImage[] layers = new BufferedImage[textureArray.length];
        for (int i = 0; i < textureArray.length; i++)
            layers[i] = textureArray[i].getAtlas();
        return layers;
    }
}