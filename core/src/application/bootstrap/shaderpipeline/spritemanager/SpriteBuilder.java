package application.bootstrap.shaderpipeline.spritemanager;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import engine.root.BuilderPackage;
import engine.util.io.JsonUtility;

/*
 * Loads raw images from disk and parses companion border JSON files.
 * Image loading and border parsing are separated so InternalLoader owns
 * the full SpriteData construction with all fields available. A companion
 * may set "stretch": true so the sliced center scales instead of tiling —
 * for artwork with gradients that must span the whole element.
 */
class SpriteBuilder extends BuilderPackage {

    // Load \\

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

    float[] parseCompanionBorder(File imageFile) {

        JsonObject json = loadCompanionJson(imageFile);

        if (json == null || !json.has("border"))
            return new float[] { 0, 0, 0, 0 };

        JsonArray b = json.getAsJsonArray("border");

        return new float[] {
                b.get(0).getAsFloat(),
                b.get(1).getAsFloat(),
                b.get(2).getAsFloat(),
                b.get(3).getAsFloat()
        };
    }

    boolean parseCompanionStretch(File imageFile) {

        JsonObject json = loadCompanionJson(imageFile);

        return json != null && JsonUtility.getBoolean(json, "stretch", false);
    }

    private JsonObject loadCompanionJson(File imageFile) {

        File jsonFile = getCompanionJson(imageFile);

        return jsonFile.exists() ? JsonUtility.loadJsonObject(jsonFile) : null;
    }

    private File getCompanionJson(File imageFile) {
        String path = imageFile.getPath();
        int dot = path.lastIndexOf('.');
        return new File((dot >= 0 ? path.substring(0, dot) : path) + ".json");
    }
}