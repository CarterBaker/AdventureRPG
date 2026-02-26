package com.internal.bootstrap.shaderpipeline.spritemanager;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import com.internal.core.engine.SystemPackage;

class InternalBuildSystem extends SystemPackage {

    // Build \\

    SpriteData buildSpriteData(File file, String spriteName) {

        BufferedImage image = loadImage(file);

        SpriteData spriteData = create(SpriteData.class);
        spriteData.constructor(spriteName, image);
        return spriteData;
    }

    // Image Loading \\

    private BufferedImage loadImage(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null)
                throwException("Image file could not be read: " + file.getAbsolutePath());
            return image;
        } catch (Exception e) {
            throwException("Failed to load sprite image: " + file.getAbsolutePath(), e);
            return null;
        }
    }
}