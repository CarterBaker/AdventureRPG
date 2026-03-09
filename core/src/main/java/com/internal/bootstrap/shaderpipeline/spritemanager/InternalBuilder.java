package com.internal.bootstrap.shaderpipeline.spritemanager;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteData;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteHandle;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.JsonUtility;

/*
 * Loads a raw image from disk, wraps it in a SpriteData container,
 * and constructs the final SpriteHandle. Owns all created children
 * so they survive loader teardown.
 */
class InternalBuilder extends BuilderPackage {

    SpriteHandle build(File file, String spriteName, int gpuHandle, ModelInstance modelInstance) {
        BufferedImage image = loadImage(file);

        SpriteData spriteData = create(SpriteData.class);
        spriteData.constructor(spriteName, image);

        float[] border = parseCompanionBorder(file);

        SpriteHandle spriteHandle = create(SpriteHandle.class);
        spriteHandle.constructor(
                spriteName, gpuHandle,
                spriteData.getWidth(), spriteData.getHeight(),
                modelInstance,
                border[0], border[1], border[2], border[3]);

        return spriteHandle;
    }

    private float[] parseCompanionBorder(File imageFile) {
        File jsonFile = getCompanionJson(imageFile);
        if (!jsonFile.exists())
            return new float[] { 0, 0, 0, 0 };
        JsonObject json = JsonUtility.loadJsonObject(jsonFile);
        if (!json.has("border"))
            return new float[] { 0, 0, 0, 0 };
        JsonArray b = json.getAsJsonArray("border");
        return new float[] {
                b.get(0).getAsFloat(), // left
                b.get(1).getAsFloat(), // bottom
                b.get(2).getAsFloat(), // right
                b.get(3).getAsFloat() // top
        };
    }

    private File getCompanionJson(File imageFile) {
        String path = imageFile.getPath();
        int dot = path.lastIndexOf('.');
        return new File((dot >= 0 ? path.substring(0, dot) : path) + ".json");
    }

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