package application.bootstrap.worldpipeline.biome;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class BiomeBlendStruct extends StructPackage {

    /*
     * One world position's resolved biome influence — the distinct biomes
     * reaching that position and the normalized weight each one contributes.
     * Filled in place by BiomeManager.sampleBiomeField() and consumed by
     * TerrainShapeUtility, so a full chunk's worth of field sampling never
     * allocates. Contributions beyond BIOME_FIELD_MAX_CONTRIBUTORS displace
     * the smallest weight currently held, which is deterministic because the
     * accumulation order is fixed by the sampling kernels themselves.
     */

    private static final int CAPACITY = EngineSetting.BIOME_FIELD_MAX_CONTRIBUTORS;

    private final BiomeHandle[] biomes = new BiomeHandle[CAPACITY];
    private final float[] weights = new float[CAPACITY];

    private int count;
    private float weightSum;

    // Accumulation \\

    public void reset() {

        for (int i = 0; i < count; i++)
            biomes[i] = null;

        count = 0;
        weightSum = 0f;
    }

    public void accumulate(BiomeHandle biome, float weight) {

        if (biome == null || weight <= 0f)
            return;

        for (int i = 0; i < count; i++) {
            if (biomes[i] == biome) {
                weights[i] += weight;
                weightSum += weight;
                return;
            }
        }

        if (count < CAPACITY) {
            biomes[count] = biome;
            weights[count] = weight;
            weightSum += weight;
            count++;
            return;
        }

        int smallest = 0;

        for (int i = 1; i < CAPACITY; i++)
            if (weights[i] < weights[smallest])
                smallest = i;

        if (weights[smallest] >= weight)
            return;

        weightSum += weight - weights[smallest];
        biomes[smallest] = biome;
        weights[smallest] = weight;
    }

    public void normalize() {

        if (count == 0 || weightSum <= 0f)
            return;

        float inverse = 1f / weightSum;

        for (int i = 0; i < count; i++)
            weights[i] *= inverse;

        weightSum = 1f;
    }

    // Accessible \\

    public int getCount() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public BiomeHandle getBiome(int index) {
        return biomes[index];
    }

    public float getWeight(int index) {
        return weights[index];
    }

    public BiomeHandle getDominantBiome() {

        if (count == 0)
            return throwException("Biome blend was queried for its dominant biome before any biome was "
                    + "accumulated into it — sampleBiomeField() must run against this struct first.");

        int dominant = 0;

        for (int i = 1; i < count; i++)
            if (weights[i] > weights[dominant])
                dominant = i;

        return biomes[dominant];
    }

    /*
     * Share of this position's influence held by biomes authored as ocean.
     * WorldGenerationManager thresholds this per block column rather than
     * reading one biome's flag, so a coastline follows the blended field
     * instead of snapping to whichever biome happened to win the chunk.
     */
    public float getOceanWeight() {

        float oceanWeight = 0f;

        for (int i = 0; i < count; i++)
            if (biomes[i].hasOceanWater())
                oceanWeight += weights[i];

        return oceanWeight;
    }
}