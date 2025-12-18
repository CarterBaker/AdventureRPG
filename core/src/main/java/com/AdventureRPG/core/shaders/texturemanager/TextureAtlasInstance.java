package com.AdventureRPG.core.shaders.texturemanager;

import java.awt.image.BufferedImage;

import com.AdventureRPG.core.engine.InstanceFrame;

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
