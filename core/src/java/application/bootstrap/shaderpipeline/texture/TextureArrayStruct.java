package application.bootstrap.shaderpipeline.texture;

import java.awt.image.BufferedImage;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class TextureArrayStruct extends StructPackage {

    /*
     * Bootstrap container for one font atlas: its layers and the coordinate
     * map of every glyph tile packed into it. Cleared and GCs with the font
     * builder after GPU upload.
     */

    // Identity
    private final String name;
    private final int atlasPixelSize;

    // Layers
    private TextureAtlasStruct[] textureArray;

    // Tiles
    private final Object2ObjectOpenHashMap<String, TextureTileStruct> tileCoordinateMap;

    // Constructor \\

    public TextureArrayStruct(
            String name,
            int atlasPixelSize,
            TextureAtlasStruct[] textureArray) {

        this.name = name;
        this.atlasPixelSize = atlasPixelSize;
        this.textureArray = textureArray;
        this.tileCoordinateMap = new Object2ObjectOpenHashMap<>();
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

    public String getName() {
        return name;
    }

    public int getAtlasPixelSize() {
        return atlasPixelSize;
    }
}