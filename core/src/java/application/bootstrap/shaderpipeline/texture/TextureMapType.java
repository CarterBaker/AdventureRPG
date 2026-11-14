package application.bootstrap.shaderpipeline.texture;

import engine.root.EngineSetting;

public enum TextureMapType {

    /*
     * The maps a tile is baked into, each its own texture array on the GPU.
     * Albedo holds colour with cutout alpha; normal the tangent-space x and y
     * the shader rebuilds z from; material ambient occlusion, specular,
     * metalness and height; extra the growth, roughness, tint and emissive
     * only some tiles carry, read through a small set of its own. Each map
     * names how many channels an image fills, its GPU format, and how its
     * mips are filtered.
     */

    ALBEDO(EngineSetting.COLOR_CHANNEL_COUNT, EngineSetting.GL_COMPRESSED_RGBA_BPTC_UNORM,
            true, false, true),
    NORMAL(EngineSetting.TEXTURE_NORMAL_SOURCE_CHANNELS, EngineSetting.GL_COMPRESSED_RG_RGTC2,
            true, true, false),
    MATERIAL(EngineSetting.TEXTURE_SCALAR_SOURCE_CHANNELS, EngineSetting.GL_COMPRESSED_RGBA_BPTC_UNORM,
            true, false, false),
    EXTRA(EngineSetting.TEXTURE_SCALAR_SOURCE_CHANNELS, EngineSetting.GL_COMPRESSED_RGBA_BPTC_UNORM,
            false, false, false);

    // Values — the tiered maps come first, so their ordinals index every tier's arrays
    public static final TextureMapType[] VALUES = values();
    public static final int TIERED_COUNT = EXTRA.ordinal();

    // Internal
    private final int sourceChannels;
    private final int glFormat;
    private final boolean tiered;
    private final boolean normalized;
    private final boolean coveragePreserved;

    // Constructor \\

    TextureMapType(int sourceChannels, int glFormat, boolean tiered, boolean normalized, boolean coveragePreserved) {
        this.sourceChannels = sourceChannels;
        this.glFormat = glFormat;
        this.tiered = tiered;
        this.normalized = normalized;
        this.coveragePreserved = coveragePreserved;
    }

    // Accessible \\

    // How many channels of an image an alias of this map fills, from its first
    public int getSourceChannels() {
        return sourceChannels;
    }

    public int getGlFormat() {
        return glFormat;
    }

    public boolean isTwoChannel() {
        return glFormat == EngineSetting.GL_COMPRESSED_RG_RGTC2;
    }

    // Whether the map fills every tier's pool, or only the extra set
    public boolean isTiered() {
        return tiered;
    }

    // Whether texels are unit vectors, renormalized at every mip
    public boolean isNormalized() {
        return normalized;
    }

    // Whether mips keep the share of texels the cutout shows
    public boolean isCoveragePreserved() {
        return coveragePreserved;
    }
}
