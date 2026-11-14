package application.bootstrap.geometrypipeline.skinnedbuffer;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class SkinnedVariantStruct extends StructPackage {

    /*
     * The material variants one look offers — an overlay's styles, a worn
     * feature's variants: each a name, the tile of its image set in the
     * character atlas (every map the image carries shares the tile) and how
     * far its relief rises in blocks, with the variant worn by default. A draw
     * picks one by index and writes its tile, the default standing in for an
     * index the look does not offer. Immutable once built.
     */

    // Variants — names, four tile floats each (u0, u1, v0, v1) and a relief depth each
    private final String[] names;
    private final float[] tiles;
    private final float[] reliefs;
    private final int defaultVariant;

    // Constructor \\

    public SkinnedVariantStruct(String[] names, float[] tiles, float[] reliefs, int defaultVariant) {

        // Variants
        this.names = names;
        this.tiles = tiles;
        this.reliefs = reliefs;
        this.defaultVariant = defaultVariant;
    }

    // Write \\

    // The variant worn: the one picked when the look offers it, else the default
    public int resolve(int variantIndex) {
        return variantIndex >= 0 && variantIndex < names.length ? variantIndex : defaultVariant;
    }

    public void writeTile(float[] out, int offset, int variantIndex) {
        System.arraycopy(tiles, resolve(variantIndex) * EngineSetting.COLOR_CHANNEL_COUNT, out, offset,
                EngineSetting.COLOR_CHANNEL_COUNT);
    }

    public float getRelief(int variantIndex) {
        return reliefs[resolve(variantIndex)];
    }

    // Accessible \\

    public int getCount() {
        return names.length;
    }

    public String getName(int variantIndex) {
        return names[variantIndex];
    }

    // A variant's place by its name, INDEX_NOT_FOUND for a name the look does not offer
    public int find(String name) {

        for (int i = 0; i < names.length; i++)
            if (names[i].equals(name))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    public int getDefault() {
        return defaultVariant;
    }
}
