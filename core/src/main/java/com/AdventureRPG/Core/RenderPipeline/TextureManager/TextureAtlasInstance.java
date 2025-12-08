package com.AdventureRPG.core.renderpipeline.texturemanager;

import java.awt.image.BufferedImage;

import com.AdventureRPG.core.kernel.InstanceFrame;

class TextureAtlasInstance extends InstanceFrame {

    // Internal
    final int atlasSize;
    private final BufferedImage atlas;

    TextureAtlasInstance(
            int atlasSize,
            BufferedImage atlas) {

        // Internal
        this.atlasSize = atlasSize;
        this.atlas = atlas;
    }

    BufferedImage atlas() {
        return atlas;
    }
}
