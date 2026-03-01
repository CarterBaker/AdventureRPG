package com.internal.bootstrap.shaderpipeline.texturemanager;

import java.awt.image.BufferedImage;

import com.internal.core.engine.DataPackage;

/*
 * Bootstrap-only container for a fully composited atlas image for one alias
 * layer. Held during GPU upload only; image data is cleared immediately after
 * to free heap. Must not be held after bootstrap completes.
 */
public class TextureAtlasData extends DataPackage {

    // Internal
    private int atlasSize;
    private BufferedImage atlas;

    // Internal \\

    void constructor(int atlasSize, BufferedImage atlas) {
        this.atlasSize = atlasSize;
        this.atlas = atlas;
    }

    // Disposal \\

    void clearImage() {
        this.atlas = null;
    }

    // Accessible \\

    int getAtlasSize() {
        return atlasSize;
    }

    BufferedImage getAtlas() {
        return atlas;
    }
}