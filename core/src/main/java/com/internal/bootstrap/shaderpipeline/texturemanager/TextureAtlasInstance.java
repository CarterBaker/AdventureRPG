package com.internal.bootstrap.shaderpipeline.texturemanager;

import java.awt.image.BufferedImage;

import com.internal.core.engine.InstancePackage;

public class TextureAtlasInstance extends InstancePackage {

    // Internal
    private int atlasSize;
    private BufferedImage atlas;

    // Internal \\

    void constructor(
            int atlasSize,
            BufferedImage atlas) {

        // Internal
        this.atlasSize = atlasSize;
        this.atlas = atlas;
    }

    // Accessible \\

    int getAtlasSize() {
        return atlasSize;
    }

    BufferedImage getAtlas() {
        return atlas;
    }
}
