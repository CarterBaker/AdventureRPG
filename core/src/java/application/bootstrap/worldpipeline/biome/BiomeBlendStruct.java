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
     * Each biome keeps the crest of the patches it fills and, when it holds
     * still water relative to its host, the lake it belongs to and the level
     * resolved for it. The base list holds the ground the height is laid on:
     * every absolute biome's own weight, and every relative biome's weight
     * credited to the absolute biome it is chained beneath, so a relative
     * patch's heart still stands on its host's ground.
     */

    public static final int INDEX_NONE = EngineSetting.INDEX_NOT_FOUND;

    private static final int CAPACITY = EngineSetting.BIOME_FIELD_MAX_CONTRIBUTORS;
    private static final int WATER_LEVEL_UNDEFINED = EngineSetting.LAKE_LEVEL_UNDEFINED;

    // Contributors
    private final BiomeHandle[] biomes = new BiomeHandle[CAPACITY];
    private final float[] weights = new float[CAPACITY];
    private final float[] bufferShares = new float[CAPACITY];
    private final float[] convertedShares = new float[CAPACITY];
    private final float[] crests = new float[CAPACITY];

    // Still Water
    private final int[] waterLevels = new int[CAPACITY];
    private final long[] lakeHashes = new long[CAPACITY];
    private final double[] lakeCentersX = new double[CAPACITY];
    private final double[] lakeCentersZ = new double[CAPACITY];
    private final int[] lakeOffsets = new int[CAPACITY];

    // Base
    private final BiomeHandle[] baseBiomes = new BiomeHandle[CAPACITY];
    private final float[] baseWeights = new float[CAPACITY];

    private int count;
    private float weightSum;
    private float bufferWeight;
    private int baseCount;
    private float baseWeightSum;

    // Accumulation \\

    public void reset() {

        for (int i = 0; i < count; i++)
            biomes[i] = null;

        for (int i = 0; i < baseCount; i++)
            baseBiomes[i] = null;

        count = 0;
        weightSum = 0f;
        bufferWeight = 0f;
        baseCount = 0;
        baseWeightSum = 0f;
    }

    // Returns the biome's index, or INDEX_NONE when its weight was too small to hold a place
    public int accumulate(BiomeHandle biome, float weight, float crest) {

        if (biome == null || weight <= 0f)
            return INDEX_NONE;

        for (int i = 0; i < count; i++) {
            if (biomes[i] == biome) {
                crests[i] = (crests[i] * weights[i] + crest * weight) / (weights[i] + weight);
                weights[i] += weight;
                weightSum += weight;
                return i;
            }
        }

        if (count < CAPACITY) {
            place(count, biome, weight, crest);
            weightSum += weight;
            return count++;
        }

        int smallest = 0;

        for (int i = 1; i < CAPACITY; i++)
            if (weights[i] < weights[smallest])
                smallest = i;

        if (weights[smallest] >= weight)
            return INDEX_NONE;

        weightSum += weight - weights[smallest];
        place(smallest, biome, weight, crest);

        return smallest;
    }

    private void place(int index, BiomeHandle biome, float weight, float crest) {
        biomes[index] = biome;
        weights[index] = weight;
        bufferShares[index] = 0f;
        convertedShares[index] = 0f;
        crests[index] = crest;
        waterLevels[index] = biome.hasRelativeStillWater() ? WATER_LEVEL_UNDEFINED : biome.getWaterLevelBlocks();
        lakeHashes[index] = 0L;
        lakeCentersX[index] = 0.0;
        lakeCentersZ[index] = 0.0;
        lakeOffsets[index] = 0;
    }

    public void accumulateBase(BiomeHandle baseBiome, float weight) {

        if (baseBiome == null || weight <= 0f)
            return;

        baseWeightSum += weight;

        for (int i = 0; i < baseCount; i++) {
            if (baseBiomes[i] == baseBiome) {
                baseWeights[i] += weight;
                return;
            }
        }

        if (baseCount < CAPACITY) {
            baseBiomes[baseCount] = baseBiome;
            baseWeights[baseCount] = weight;
            baseCount++;
            return;
        }

        int smallest = 0;

        for (int i = 1; i < CAPACITY; i++)
            if (baseWeights[i] < baseWeights[smallest])
                smallest = i;

        if (baseWeights[smallest] >= weight) {
            baseWeightSum -= weight;
            return;
        }

        baseWeightSum -= baseWeights[smallest];
        baseBiomes[smallest] = baseBiome;
        baseWeights[smallest] = weight;
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

        if (baseWeightSum <= 0f)
            return;

        float baseInverse = 1f / baseWeightSum;

        for (int i = 0; i < baseCount; i++)
            baseWeights[i] *= baseInverse;

        baseWeightSum = 1f;
    }

    public void convertToBuffer(int index, BiomeHandle bufferBiome, float fraction) {

        float converted = weights[index] * fraction;

        if (converted <= 0f)
            return;

        weights[index] -= converted;
        convertedShares[index] += converted;
        weightSum -= converted;
        bufferWeight += converted;

        accumulate(bufferBiome, converted, crests[index]);

        for (int i = 0; i < count; i++) {
            if (biomes[i] == bufferBiome) {
                bufferShares[i] = Math.min(weights[i], bufferShares[i] + converted);
                return;
            }
        }
    }

    // The ground converts by the same fraction, so a relative biome's host takes its shore beneath it
    public void convertBaseToBuffer(int index, BiomeHandle bufferBiome, float fraction) {

        float converted = baseWeights[index] * fraction;

        if (converted <= 0f)
            return;

        baseWeights[index] -= converted;
        baseWeightSum -= converted;

        accumulateBase(bufferBiome, converted);
    }

    // Still Water \\

    public void setLake(int index, long lakeHash, double centerX, double centerZ, int offsetBlocks) {
        lakeHashes[index] = lakeHash;
        lakeCentersX[index] = centerX;
        lakeCentersZ[index] = centerZ;
        lakeOffsets[index] = offsetBlocks;
    }

    public void setWaterLevelBlocks(int index, int waterLevelBlocks) {
        waterLevels[index] = waterLevelBlocks;
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

    // 1 where a relative biome's offset holds in full, falling toward 0 where it gathers away from a rolling edge
    public float getCrest(int index) {
        return crests[index];
    }

    public int getWaterLevelBlocks(int index) {
        return waterLevels[index];
    }

    public long getLakeHash(int index) {
        return lakeHashes[index];
    }

    public double getLakeCenterX(int index) {
        return lakeCentersX[index];
    }

    public double getLakeCenterZ(int index) {
        return lakeCentersZ[index];
    }

    public int getLakeOffsetBlocks(int index) {
        return lakeOffsets[index];
    }

    public int getBaseCount() {
        return baseCount;
    }

    public BiomeHandle getBaseBiome(int index) {
        return baseBiomes[index];
    }

    public float getBaseWeight(int index) {
        return baseWeights[index];
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
