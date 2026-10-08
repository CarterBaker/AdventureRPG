package application.bootstrap.worldpipeline.biome;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class BiomeBlendStruct extends StructPackage {

    /*
     * One position's resolved biome influence: the distinct biomes reaching it
     * and their normalized weights, filled in place so field sampling never
     * allocates. Past BIOME_FIELD_MAX_CONTRIBUTORS the smallest weight is
     * displaced, and weight moved into shore buffers is tallied separately,
     * in total and per biome on both sides of the move, so the biome a
     * position naturally belongs to stays readable beneath its shore buffer.
     */

    private static final int CAPACITY = EngineSetting.BIOME_FIELD_MAX_CONTRIBUTORS;

    private final BiomeHandle[] biomes = new BiomeHandle[CAPACITY];
    private final float[] weights = new float[CAPACITY];
    private final float[] bufferShares = new float[CAPACITY];
    private final float[] convertedShares = new float[CAPACITY];

    private int count;
    private float weightSum;
    private float bufferWeight;

    // Accumulation \\

    public void reset() {

        for (int i = 0; i < count; i++)
            biomes[i] = null;

        count = 0;
        weightSum = 0f;
        bufferWeight = 0f;
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
            bufferShares[count] = 0f;
            convertedShares[count] = 0f;
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
        bufferShares[smallest] = 0f;
        convertedShares[smallest] = 0f;
    }

    public void normalize() {

        if (count == 0 || weightSum <= 0f)
            return;

        float inverse = 1f / weightSum;

        for (int i = 0; i < count; i++) {
            weights[i] *= inverse;
            bufferShares[i] *= inverse;
            convertedShares[i] *= inverse;
        }

        bufferWeight *= inverse;
        weightSum = 1f;
    }

    public void convertToBuffer(int index, BiomeHandle bufferBiome, float fraction) {

        float converted = weights[index] * fraction;

        if (converted <= 0f)
            return;

        weights[index] -= converted;
        convertedShares[index] += converted;
        weightSum -= converted;
        bufferWeight += converted;

        accumulate(bufferBiome, converted);

        for (int i = 0; i < count; i++) {
            if (biomes[i] == bufferBiome) {
                bufferShares[i] = Math.min(weights[i], bufferShares[i] + converted);
                return;
            }
        }
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

    // The weight a biome holds by itself, as it stood before any shore buffer was converted into or out of it
    public float getNaturalWeight(int index) {
        return weights[index] - bufferShares[index] + convertedShares[index];
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

    // The land biome the position belongs to beneath any shore buffer, the dominant biome where no land reaches
    public BiomeHandle getDominantLandBiome() {

        BiomeHandle dominant = getDominantBiome();
        float dominantWeight = 0f;

        for (int i = 0; i < count; i++) {

            float naturalWeight = getNaturalWeight(i);

            if (!biomes[i].hasOceanWater() && naturalWeight > dominantWeight) {
                dominantWeight = naturalWeight;
                dominant = biomes[i];
            }
        }

        return dominant;
    }

    public float getOceanWeight() {

        float oceanWeight = 0f;

        for (int i = 0; i < count; i++)
            if (biomes[i].hasOceanWater())
                oceanWeight += weights[i];

        return oceanWeight;
    }

    public float getCoastalWeight() {
        return getOceanWeight() + bufferWeight;
    }
}