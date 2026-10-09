package application.bootstrap.worldpipeline.tree;

import engine.root.StructPackage;

public class TreeLeafStruct extends StructPackage {

    /*
     * A species' foliage: the radius of one leaf cluster at maturity and how
     * flat it sits, the share of its clusters a tree keeps, its texture and
     * the color the texture is tinted with, a second texture and color a
     * share of its clusters carry instead for blossom, fruit or glow, and how
     * far a weeping species' leaves hang below its twig tips.
     */

    private final float radiusBlocks;
    private final float squash;
    private final float density;
    private final String textureName;
    private final int color;
    private final String accentTextureName;
    private final int accentColor;
    private final float accentChance;
    private final float hangBlocks;

    public TreeLeafStruct(
            float radiusBlocks,
            float squash,
            float density,
            String textureName,
            int color,
            String accentTextureName,
            int accentColor,
            float accentChance,
            float hangBlocks) {

        this.radiusBlocks = radiusBlocks;
        this.squash = squash;
        this.density = density;
        this.textureName = textureName;
        this.color = color;
        this.accentTextureName = accentTextureName;
        this.accentColor = accentColor;
        this.accentChance = accentChance;
        this.hangBlocks = hangBlocks;
    }

    // Accessible \\

    public float getRadiusBlocks() {
        return radiusBlocks;
    }

    public float getSquash() {
        return squash;
    }

    public float getDensity() {
        return density;
    }

    public String getTextureName() {
        return textureName;
    }

    public int getColor() {
        return color;
    }

    public String getAccentTextureName() {
        return accentTextureName;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public float getAccentChance() {
        return accentChance;
    }

    public float getHangBlocks() {
        return hangBlocks;
    }
}
