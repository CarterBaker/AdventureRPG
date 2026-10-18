package application.bootstrap.worldpipeline.biome;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class ProbableEdgeStruct extends StructPackage {

    /*
     * How a probable biome's patches meet the ground around them: the width
     * of the fade across their outline where it rolls and where it breaks
     * sharp, the share of the outline that breaks sharp, in stretches a
     * fraction of a patch across that fade into the rolling stretches beside
     * them, the share of a relative biome's full rise a sharp stretch climbs
     * at once before its crown carries on, and how far edge noise roughens
     * the outline. Bands and roughness are shares of a patch's own outline
     * metric, so they scale with it. DEFAULT keeps one soft fade all the way
     * around.
     */

    public static final ProbableEdgeStruct DEFAULT = new ProbableEdgeStruct(
            EngineSetting.BIOME_PROBABLE_DEFAULT_EDGE_BAND,
            EngineSetting.BIOME_PROBABLE_DEFAULT_EDGE_BAND,
            EngineSetting.BIOME_PROBABLE_DEFAULT_EDGE_SHARP_SHARE,
            EngineSetting.BIOME_PROBABLE_DEFAULT_EDGE_SHARP_WAVELENGTH,
            EngineSetting.BIOME_PROBABLE_DEFAULT_EDGE_SHARP_RISE,
            EngineSetting.BIOME_PROBABLE_DEFAULT_EDGE_ROUGHNESS);

    private final float softBand;
    private final float sharpBand;
    private final float sharpShare;
    private final float sharpWavelength;
    private final float sharpRise;
    private final float roughness;

    public ProbableEdgeStruct(
            float softBand,
            float sharpBand,
            float sharpShare,
            float sharpWavelength,
            float sharpRise,
            float roughness) {

        this.softBand = softBand;
        this.sharpBand = sharpBand;
        this.sharpShare = sharpShare;
        this.sharpWavelength = sharpWavelength;
        this.sharpRise = sharpRise;
        this.roughness = roughness;
    }

    // Accessible \\

    public float getSoftBand() {
        return softBand;
    }

    public float getSharpBand() {
        return sharpBand;
    }

    public float getSharpShare() {
        return sharpShare;
    }

    public float getSharpWavelength() {
        return sharpWavelength;
    }

    public float getSharpRise() {
        return sharpRise;
    }

    public float getRoughness() {
        return roughness;
    }

    public float getMaxBand() {
        return Math.max(softBand, sharpBand);
    }

    public boolean hasSharpStretches() {
        return sharpShare > 0f;
    }
}
