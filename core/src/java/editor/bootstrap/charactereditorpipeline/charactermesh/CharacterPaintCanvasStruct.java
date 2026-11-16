package editor.bootstrap.charactereditorpipeline.charactermesh;

import java.io.File;

import application.bootstrap.shaderpipeline.texturemanager.AliasStruct;
import editor.bootstrap.imagepipeline.imagelayer.ImageLayerStackStruct;

public class CharacterPaintCanvasStruct extends ImageLayerStackStruct {

    /*
     * One map of one character texture open for painting: the tile it
     * paints and the alias whose map and channels it fills, the image file
     * it saves to beside the texture, and the shared layered image it is
     * painted as — the first layer the image itself, each above it laid over
     * it by its own alpha and opacity while it is shown, the base keeping
     * its own alpha — whose flattened result the tile is baked from while it
     * is painted.
     */

    // Identity
    private final String textureName;
    private final int mapIndex;
    private final File file;

    // Tile — the tile painted, the alias the map is, and what the map holds where nothing paints it
    private final int tileID;
    private final AliasStruct alias;
    private final int defaultColor;

    // Constructor \

    public CharacterPaintCanvasStruct(
            String textureName,
            int mapIndex,
            File file,
            int tileID,
            AliasStruct alias,
            int width,
            int height,
            int defaultColor) {

        super(width, height, true);

        // Identity
        this.textureName = textureName;
        this.mapIndex = mapIndex;
        this.file = file;

        // Tile
        this.tileID = tileID;
        this.alias = alias;
        this.defaultColor = defaultColor;
    }

    // Accessible \

    public String getTextureName() {
        return textureName;
    }

    public int getMapIndex() {
        return mapIndex;
    }

    public File getFile() {
        return file;
    }

    public int getTileID() {
        return tileID;
    }

    public AliasStruct getAlias() {
        return alias;
    }

    // What the map holds where nothing paints it, RGBA8888
    public int getDefaultColor() {
        return defaultColor;
    }
}
