package application.bootstrap.shaderpipeline.texture;

import engine.root.StructPackage;

public class TextureRevealStruct extends StructPackage {

    /*
     * How a tile shows itself as a covering grows over a face: per level from
     * zero to the full level, the share of its texels a full draw shows,
     * those its growth map has reached and its albedo holds opaque, and the
     * average albedo of every texel it shows at its full level, whole and
     * weighted by how far each takes a biome's tint. The texture builder
     * bakes it from the tile's images, so a distant face and a map pixel can
     * stand in for a covering's full draw without a single texture read.
     */

    // Reveal
    private final float[] levelShares;
    private final int revealColor;
    private final int tintableColor;

    // Constructor \\

    public TextureRevealStruct(float[] levelShares, int revealColor, int tintableColor) {
        this.levelShares = levelShares;
        this.revealColor = revealColor;
        this.tintableColor = tintableColor;
    }

    // Accessible \\

    public float getLevelShare(int level) {
        return levelShares[level];
    }

    public int getRevealColor() {
        return revealColor;
    }

    public int getTintableColor() {
        return tintableColor;
    }
}
