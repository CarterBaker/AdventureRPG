package com.internal.bootstrap.shaderpipeline.spritemanager;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteHandle;
import com.internal.core.engine.BuilderPackage;

/*
 * Loads a raw image from disk, wraps it in a SpriteData container,
 * and constructs the final SpriteHandle. Owns all created children
 * so they survive loader teardown.
 */
class InternalBuildSystem extends BuilderPackage {

    // Build \\

    SpriteHandle build(File file, String spriteName, int gpuHandle, ModelInstance modelInstance) {

        BufferedImage image = loadImage(file);

        SpriteData spriteData = create(SpriteData.class);
        spriteData.constructor(spriteName, image);

        SpriteHandle spriteHandle = create(SpriteHandle.class);
        spriteHandle.constructor(
                spriteName,
                gpuHandle,
                spriteData.getWidth(),
                spriteData.getHeight(),
                modelInstance);

        return spriteHandle;
    }

    // Image Loading \\

    BufferedImage loadImage(File file) {
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