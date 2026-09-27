package application.bootstrap.shaderpipeline.spritemanager;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class SpriteBuilder extends BuilderPackage {

    /*
     * Loads raw images from disk and parses companion border ARPG files.
     * Image loading and border parsing are separated so SpriteLoader owns
     * the full SpriteData construction with all fields available. A companion
     * may set "stretch": true so the sliced center scales instead of tiling —
     * for artwork with gradients that must span the whole element.
     */

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

        ArpgObjectStruct arpg = loadCompanionArpg(imageFile);

        if (arpg == null || !arpg.has("border"))
            return new float[] { 0, 0, 0, 0 };

        ArpgArrayStruct b = arpg.getAsArray("border");

        return new float[] {
                b.get(0).getAsFloat(),
                b.get(1).getAsFloat(),
                b.get(2).getAsFloat(),
                b.get(3).getAsFloat()
        };
    }

    boolean parseCompanionStretch(File imageFile) {

        ArpgObjectStruct arpg = loadCompanionArpg(imageFile);

        return arpg != null && ArpgUtility.getBoolean(arpg, "stretch", false);
    }

    private ArpgObjectStruct loadCompanionArpg(File imageFile) {

        File arpgFile = ArpgUtility.resolveCompanionFile(imageFile);

        return arpgFile.exists() ? ArpgUtility.loadObject(arpgFile) : null;
    }
}