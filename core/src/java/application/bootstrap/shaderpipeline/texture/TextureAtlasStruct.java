package application.bootstrap.shaderpipeline.texture;

import java.awt.image.BufferedImage;

import engine.root.StructPackage;

public class TextureAtlasStruct extends StructPackage {

    /*
     * Bootstrap container for one fully composited font atlas layer. Held
     * during GPU upload only — the image is cleared immediately after to
     * free heap. GCs with the font builder once the font is built.
     */

    // Identity
    private final int atlasSize;
    private BufferedImage atlas;

    // Constructor \\

    public TextureAtlasStruct(int atlasSize, BufferedImage atlas) {
        this.atlasSize = atlasSize;
        this.atlas = atlas;
    }

    // Management \\

    void clearImage() {
        this.atlas = null;
    }

    // Accessible \\

    public int getAtlasSize() {
        return atlasSize;
    }

    BufferedImage getAtlas() {
        return atlas;
    }
}