package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class TerrainFeatureStruct extends StructPackage {

    /*
     * Every biome-blended control that shapes and hollows the ground at one
     * position: coastal reach and the sea's own share, detail, ridge and cliff
     * layers with their coverage masks already applied, sea cliffs, cave
     * controls and the still water standing over it. Filled in place by
     * TerrainShapeUtility.resolveFeatures(), or blended from four filled
     * corners, so sampling never allocates. Detail, ridges and cliffs are kept
     * as layers, one per wavelength or step, so biomes of different scales
     * blend what their noise and terraces produce rather than the scale
     * itself: on a world this wide the slightest change of wavelength moves
     * the noise to an unrelated place. Still water never blends between
     * levels: a blend takes the level of its strongest corner and only the
     * weight of the corners that share it.
     */

    public static final int LAKE_LEVEL_UNDEFINED = EngineSetting.LAKE_LEVEL_UNDEFINED;
    public static final int LAYERS_MAX = EngineSetting.TERRAIN_FEATURE_LAYERS_MAX;

    // Coast
    float coastalWeight;
    float oceanWeight;
    float coastCliffHeightBlocks;
    float coastOverhangBlocks;
    float seaCaves;

    // Detail Layers
    final float[] detailWavelengthBlocks = new float[LAYERS_MAX];
    final float[] detailAmplitudeBlocks = new float[LAYERS_MAX];
    int detailLayerCount;

    // Ridge Layers
    final float[] ridgeWavelengthBlocks = new float[LAYERS_MAX];
    final float[] ridgeAmplitudeBlocks = new float[LAYERS_MAX];
    int ridgeLayerCount;

    // Cliff Layers
    final float[] cliffStepBlocks = new float[LAYERS_MAX];
    final float[] cliffStrength = new float[LAYERS_MAX];
    int cliffLayerCount;

    // Caves
    float caveTunnels;
    float caveCaverns;
    float caveNoodles;
    float caveLakes;
    float caveMinHeightBlocks;
    float caveMaxDepthBlocks;
    float caveEntrances;

    // Still Water
    int lakeLevelBlocks;
    float lakeWeight;

    // Blend \\

    public void blendBilinear(
            TerrainFeatureStruct corner00,
            TerrainFeatureStruct corner10,
            TerrainFeatureStruct corner01,
            TerrainFeatureStruct corner11,
            float tx,
            float tz) {

        float weight00 = (1f - tx) * (1f - tz);
        float weight10 = tx * (1f - tz);
        float weight01 = (1f - tx) * tz;
        float weight11 = tx * tz;

        coastalWeight = interpolate(
                corner00.coastalWeight, corner10.coastalWeight,
                corner01.coastalWeight, corner11.coastalWeight,
                weight00, weight10, weight01, weight11);
        oceanWeight = interpolate(
                corner00.oceanWeight, corner10.oceanWeight,
                corner01.oceanWeight, corner11.oceanWeight,
                weight00, weight10, weight01, weight11);
        coastCliffHeightBlocks = interpolate(
                corner00.coastCliffHeightBlocks, corner10.coastCliffHeightBlocks,
                corner01.coastCliffHeightBlocks, corner11.coastCliffHeightBlocks,
                weight00, weight10, weight01, weight11);
        coastOverhangBlocks = interpolate(
                corner00.coastOverhangBlocks, corner10.coastOverhangBlocks,
                corner01.coastOverhangBlocks, corner11.coastOverhangBlocks,
                weight00, weight10, weight01, weight11);
        seaCaves = interpolate(
                corner00.seaCaves, corner10.seaCaves,
                corner01.seaCaves, corner11.seaCaves,
                weight00, weight10, weight01, weight11);

        clearLayers();

        blendLayers(corner00, weight00);
        blendLayers(corner10, weight10);
        blendLayers(corner01, weight01);
        blendLayers(corner11, weight11);

        caveTunnels = interpolate(
                corner00.caveTunnels, corner10.caveTunnels,
                corner01.caveTunnels, corner11.caveTunnels,
                weight00, weight10, weight01, weight11);
        caveCaverns = interpolate(
                corner00.caveCaverns, corner10.caveCaverns,
                corner01.caveCaverns, corner11.caveCaverns,
                weight00, weight10, weight01, weight11);
        caveNoodles = interpolate(
                corner00.caveNoodles, corner10.caveNoodles,
                corner01.caveNoodles, corner11.caveNoodles,
                weight00, weight10, weight01, weight11);
        caveLakes = interpolate(
                corner00.caveLakes, corner10.caveLakes,
                corner01.caveLakes, corner11.caveLakes,
                weight00, weight10, weight01, weight11);
        caveMinHeightBlocks = interpolate(
                corner00.caveMinHeightBlocks, corner10.caveMinHeightBlocks,
                corner01.caveMinHeightBlocks, corner11.caveMinHeightBlocks,
                weight00, weight10, weight01, weight11);
        caveMaxDepthBlocks = interpolate(
                corner00.caveMaxDepthBlocks, corner10.caveMaxDepthBlocks,
                corner01.caveMaxDepthBlocks, corner11.caveMaxDepthBlocks,
                weight00, weight10, weight01, weight11);
        caveEntrances = interpolate(
                corner00.caveEntrances, corner10.caveEntrances,
                corner01.caveEntrances, corner11.caveEntrances,
                weight00, weight10, weight01, weight11);

        blendLake(corner00, corner10, corner01, corner11, weight00, weight10, weight01, weight11);
    }

    private static float interpolate(
            float value00, float value10, float value01, float value11,
            float weight00, float weight10, float weight01, float weight11) {
        return value00 * weight00 + value10 * weight10 + value01 * weight01 + value11 * weight11;
    }

    private void blendLayers(TerrainFeatureStruct corner, float weight) {

        for (int i = 0; i < corner.detailLayerCount; i++)
            addDetailLayer(corner.detailWavelengthBlocks[i], corner.detailAmplitudeBlocks[i] * weight);

        for (int i = 0; i < corner.ridgeLayerCount; i++)
            addRidgeLayer(corner.ridgeWavelengthBlocks[i], corner.ridgeAmplitudeBlocks[i] * weight);

        for (int i = 0; i < corner.cliffLayerCount; i++)
            addCliffLayer(corner.cliffStepBlocks[i], corner.cliffStrength[i] * weight);
    }

    // Layers \\

    void clearLayers() {
        detailLayerCount = 0;
        ridgeLayerCount = 0;
        cliffLayerCount = 0;
    }

    void addDetailLayer(float wavelengthBlocks, float amplitudeBlocks) {
        detailLayerCount = mergeLayer(
                detailWavelengthBlocks, detailAmplitudeBlocks, detailLayerCount, wavelengthBlocks, amplitudeBlocks);
    }

    void addRidgeLayer(float wavelengthBlocks, float amplitudeBlocks) {
        ridgeLayerCount = mergeLayer(
                ridgeWavelengthBlocks, ridgeAmplitudeBlocks, ridgeLayerCount, wavelengthBlocks, amplitudeBlocks);
    }

    void addCliffLayer(float stepBlocks, float strength) {
        cliffLayerCount = mergeLayer(cliffStepBlocks, cliffStrength, cliffLayerCount, stepBlocks, strength);
    }

    // Merges into the layer of the same scale, displacing the weakest once every layer is taken; returns the count
    private static int mergeLayer(float[] scales, float[] weights, int count, float scale, float weight) {

        if (weight <= 0f)
            return count;

        for (int i = 0; i < count; i++) {
            if (scales[i] == scale) {
                weights[i] += weight;
                return count;
            }
        }

        if (count < LAYERS_MAX) {
            scales[count] = scale;
            weights[count] = weight;
            return count + 1;
        }

        int weakest = 0;

        for (int i = 1; i < LAYERS_MAX; i++)
            if (weights[i] < weights[weakest])
                weakest = i;

        if (weights[weakest] < weight) {
            scales[weakest] = scale;
            weights[weakest] = weight;
        }

        return count;
    }

    private void blendLake(
            TerrainFeatureStruct corner00,
            TerrainFeatureStruct corner10,
            TerrainFeatureStruct corner01,
            TerrainFeatureStruct corner11,
            float weight00,
            float weight10,
            float weight01,
            float weight11) {

        TerrainFeatureStruct strongest = corner00;

        if (corner10.lakeWeight > strongest.lakeWeight)
            strongest = corner10;

        if (corner01.lakeWeight > strongest.lakeWeight)
            strongest = corner01;

        if (corner11.lakeWeight > strongest.lakeWeight)
            strongest = corner11;

        lakeLevelBlocks = strongest.lakeLevelBlocks;

        if (lakeLevelBlocks == LAKE_LEVEL_UNDEFINED) {
            lakeWeight = 0f;
            return;
        }

        lakeWeight = sharedLakeWeight(corner00, weight00)
                + sharedLakeWeight(corner10, weight10)
                + sharedLakeWeight(corner01, weight01)
                + sharedLakeWeight(corner11, weight11);
    }

    private float sharedLakeWeight(TerrainFeatureStruct corner, float weight) {
        return corner.lakeLevelBlocks == lakeLevelBlocks ? corner.lakeWeight * weight : 0f;
    }

    // Accessible \\

    public float getCoastalWeight() {
        return coastalWeight;
    }

    public float getCoastOverhangBlocks() {
        return coastOverhangBlocks;
    }

    public float getSeaCaves() {
        return seaCaves;
    }

    public float getCaveTunnels() {
        return caveTunnels;
    }

    public float getCaveCaverns() {
        return caveCaverns;
    }

    public float getCaveNoodles() {
        return caveNoodles;
    }

    public float getCaveLakes() {
        return caveLakes;
    }

    public int getCaveMinHeightBlocks() {
        return Math.round(caveMinHeightBlocks);
    }

    public int getCaveMaxDepthBlocks() {
        return Math.round(caveMaxDepthBlocks);
    }

    public boolean hasCaveEntrances() {
        return caveEntrances >= EngineSetting.CAVE_ENTRANCE_SHARE;
    }

    public boolean hasCaves() {
        return caveTunnels > 0f || caveCaverns > 0f || caveNoodles > 0f || caveLakes > 0f;
    }

    public int getLakeLevelBlocks() {
        return lakeLevelBlocks;
    }

    public float getLakeWeight() {
        return lakeWeight;
    }

    public boolean hasLake() {
        return lakeLevelBlocks != LAKE_LEVEL_UNDEFINED && lakeWeight > 0f;
    }
}
