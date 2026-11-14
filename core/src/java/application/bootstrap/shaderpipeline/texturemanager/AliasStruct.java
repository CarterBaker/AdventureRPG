package application.bootstrap.shaderpipeline.texturemanager;

import java.awt.Color;

import application.bootstrap.shaderpipeline.texture.TextureMapType;
import engine.root.StructPackage;

public class AliasStruct extends StructPackage {

    /*
     * One alias type: its default fill colour, the baked map its images go
     * into and the first channel of that map they fill. The bake reads it to
     * pack a tile's images, and the editor to paint one back.
     */

    // Identity
    private final String aliasType;
    private final Color defaultColor;
    private final TextureMapType mapType;
    private final int channel;

    // Constructor \\

    public AliasStruct(String aliasType, Color defaultColor, TextureMapType mapType, int channel) {
        this.aliasType = aliasType;
        this.defaultColor = defaultColor;
        this.mapType = mapType;
        this.channel = channel;
    }

    // Accessible \\

    public String getAliasType() {
        return aliasType;
    }

    public Color getAliasColor() {
        return defaultColor;
    }

    public TextureMapType getMapType() {
        return mapType;
    }

    public int getChannel() {
        return channel;
    }
}
