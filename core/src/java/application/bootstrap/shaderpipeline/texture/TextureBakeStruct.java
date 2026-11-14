package application.bootstrap.shaderpipeline.texture;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class TextureBakeStruct extends StructPackage {

    /*
     * One baked tile as the bake writes it and the game reads it: its source
     * checksum, the size of its top tier, the average colour and reveal the
     * far tier stands in with, and the block-compressed mip chains of every
     * map, per tier it fills, at the fallback size, and for the extra set
     * when the tile carries one. Chains run from the largest level down to a
     * single texel, levels back to back.
     */

    // Identity
    private final String tileName;
    private final long sourceChecksum;
    private final int size;

    // Far
    private final int averageColor;
    private final TextureRevealStruct reveal;

    // Maps
    private final byte[][][] tierMaps;
    private final byte[][] fallbackMaps;
    private final byte[] extraMap;

    // Constructor \\

    public TextureBakeStruct(
            String tileName,
            long sourceChecksum,
            int size,
            int averageColor,
            TextureRevealStruct reveal,
            byte[][][] tierMaps,
            byte[][] fallbackMaps,
            byte[] extraMap) {

        // Identity
        this.tileName = tileName;
        this.sourceChecksum = sourceChecksum;
        this.size = size;

        // Far
        this.averageColor = averageColor;
        this.reveal = reveal;

        // Maps
        this.tierMaps = tierMaps;
        this.fallbackMaps = fallbackMaps;
        this.extraMap = extraMap;
    }

    // Accessible \\

    public String getTileName() {
        return tileName;
    }

    public long getSourceChecksum() {
        return sourceChecksum;
    }

    public int getSize() {
        return size;
    }

    // How many tiers the tile fills, every tier up to its own size
    public int getTierCount() {

        int count = 0;

        while (count < EngineSetting.TEXTURE_TIER_COUNT && EngineSetting.TEXTURE_TIER_SIZES[count] <= size)
            count++;

        return count;
    }

    public int getAverageColor() {
        return averageColor;
    }

    public TextureRevealStruct getReveal() {
        return reveal;
    }

    public byte[] getTierMap(int tier, TextureMapType mapType) {
        return tierMaps[tier][mapType.ordinal()];
    }

    public byte[] getFallbackMap(TextureMapType mapType) {
        return fallbackMaps[mapType.ordinal()];
    }

    public byte[] getExtraMap() {
        return extraMap;
    }
}
