package application.bootstrap.worldpipeline.biome;

import engine.root.StructPackage;

public class BiomeCliffStruct extends StructPackage {

    /*
     * How a biome breaks its ground into cliffs: the height of each step its
     * slopes are terraced into, how strongly the terracing takes hold, and the
     * share of the biome a broad noise mask lets it reach. NONE leaves the
     * ground untouched.
     */

    public static final BiomeCliffStruct NONE = new BiomeCliffStruct(0f, 0f, 0f);

    private final float stepBlocks;
    private final float strength;
    private final float coverage;

    public BiomeCliffStruct(float stepBlocks, float strength, float coverage) {
        this.stepBlocks = stepBlocks;
        this.strength = strength;
        this.coverage = coverage;
    }

    // Accessible \\

    public float getStepBlocks() {
        return stepBlocks;
    }

    public float getStrength() {
        return strength;
    }

    public float getCoverage() {
        return coverage;
    }

    public boolean hasCliffs() {
        return stepBlocks > 0f && strength > 0f && coverage > 0f;
    }
}
