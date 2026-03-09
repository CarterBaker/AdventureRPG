package com.internal.bootstrap.shaderpipeline.Texture;

import java.awt.image.BufferedImage;

import com.internal.core.engine.DataPackage;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Bootstrap-only container grouping all alias layers for one texture array
 * directory alongside the tile coordinate map used for UV registration.
 * Also carries the set of alias IDs actually found in this atlas — used by
 * seedUBO to write only the uniforms this atlas provides. Atlas image data
 * is cleared after GPU upload. Must not be held after bootstrap completes.
 */
public class TextureArrayData extends DataPackage {

    // Internal
    private int id;
    private String name;
    private int atlasPixelSize;
    private TextureAtlasData[] textureArray;

    // Tiles
    private Object2ObjectOpenHashMap<String, TextureTileData> tileCoordinateMap;

    // Alias tracking — only the IDs present in this atlas's source files
    private IntSet foundAliasIds;

    // Internal \\

    public void constructor(int id, String name, int atlasPixelSize, TextureAtlasData[] textureArray) {
        this.id = id;
        this.name = name;
        this.atlasPixelSize = atlasPixelSize;
        this.textureArray = textureArray;
        this.tileCoordinateMap = new Object2ObjectOpenHashMap<>();
        this.foundAliasIds = new IntOpenHashSet();
    }

    // Alias Tracking \\

    public void registerFoundAlias(int aliasId) {
        foundAliasIds.add(aliasId);
    }

    public IntSet getFoundAliasIds() {
        return foundAliasIds;
    }

    // Disposal \\

    public void clearAtlases() {
        for (int i = 0; i < textureArray.length; i++) {
            textureArray[i].clearImage();
            textureArray[i] = null;
        }
    }

    // Tiles \\

    public void registerTile(TextureTileData tile) {
        tileCoordinateMap.put(tile.getName(), tile);
    }

    public Object2ObjectOpenHashMap<String, TextureTileData> getTileCoordinateMap() {
        return tileCoordinateMap;
    }

    // Accessible \\

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAtlasPixelSize() {
        return atlasPixelSize;
    }

    public BufferedImage[] getRawImageArray() {
        BufferedImage[] layers = new BufferedImage[textureArray.length];
        for (int i = 0; i < textureArray.length; i++)
            layers[i] = textureArray[i].getAtlas();
        return layers;
    }
}