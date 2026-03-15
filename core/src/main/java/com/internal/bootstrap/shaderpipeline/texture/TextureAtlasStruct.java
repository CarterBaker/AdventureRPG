package com.internal.bootstrap.shaderpipeline.texture;

import java.awt.image.BufferedImage;
import com.internal.core.engine.StructPackage;

public class TextureAtlasStruct extends StructPackage {

    /*
     * Bootstrap container for a fully composited atlas image for one alias
     * layer. Held during GPU upload only — image cleared immediately after
     * to free heap. GCs with the loader when bootstrap completes.
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