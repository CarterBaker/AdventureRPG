package application.bootstrap.worldpipeline.biome;

public enum ProbableBiomePlacement {

    /*
     * Where a probable biome appears inside the biome that chains it. A
     * biome's ARPG names the placement by the lower-case constant name.
     */

    SCATTER, // Shaped patches spread across the parent, together covering its chance of the parent
    CENTER // A core at the middle of each of the parent's own patches, following their outline
}
