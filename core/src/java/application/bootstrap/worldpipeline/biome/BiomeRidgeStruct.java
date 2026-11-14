package application.bootstrap.worldpipeline.biome;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class BiomeRidgeStruct extends StructPackage {

    /*
     * The ridged layer a biome raises along its high ground: how many blocks
     * its sharp, branching spines and gullies add, and how far apart they run.
     * NONE adds nothing.
     */

    public static final BiomeRidgeStruct NONE = new BiomeRidgeStruct(
            0f, EngineSetting.DEFAULT_BIOME_RIDGE_WAVELENGTH_BLOCKS);

    private final float amplitudeBlocks;
    private final float wavelengthBlocks;

    public BiomeRidgeStruct(float amplitudeBlocks, float wavelengthBlocks) {
        this.amplitudeBlocks = amplitudeBlocks;
        this.wavelengthBlocks = wavelengthBlocks;
    }

    // Accessible \\

    public float getAmplitudeBlocks() {
        return amplitudeBlocks;
    }

    public float getWavelengthBlocks() {
        return wavelengthBlocks;
    }
}
