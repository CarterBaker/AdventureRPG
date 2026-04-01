package program.bootstrap.shaderpipeline.texture;

import java.awt.image.BufferedImage;
import program.core.engine.StructPackage;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class TextureArrayStruct extends StructPackage {

    /*
     * Bootstrap container grouping all alias layers for one texture array
     * directory. Carries the tile coordinate map and the set of alias IDs
     * actually found in source files — used by seedUBO to write only the
     * uniforms this atlas provides. Cleared and GCs with the loader after
     * GPU upload.
     */

    // Identity
    private final int id;
    private final String name;
    private final int atlasPixelSize;

    // Layers
    private TextureAtlasStruct[] textureArray;

    // Tiles
    private final Object2ObjectOpenHashMap<String, TextureTileStruct> tileCoordinateMap;

    // Alias Tracking
    private final IntSet foundAliasIds;

    // Constructor \\

    public TextureArrayStruct(
            int id,
            String name,
            int atlasPixelSize,
            TextureAtlasStruct[] textureArray) {

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

    // Tiles \\

    public void registerTile(TextureTileStruct tile) {
        tileCoordinateMap.put(tile.getName(), tile);
    }

    public Object2ObjectOpenHashMap<String, TextureTileStruct> getTileCoordinateMap() {
        return tileCoordinateMap;
    }

    // Disposal \\

    public void clearAtlases() {
        for (int i = 0; i < textureArray.length; i++) {
            textureArray[i].clearImage();
            textureArray[i] = null;
        }
    }

    // Accessible \\

    public BufferedImage[] getRawImageArray() {
        BufferedImage[] layers = new BufferedImage[textureArray.length];
        for (int i = 0; i < textureArray.length; i++)
            layers[i] = textureArray[i].getAtlas();
        return layers;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAtlasPixelSize() {
        return atlasPixelSize;
    }
}