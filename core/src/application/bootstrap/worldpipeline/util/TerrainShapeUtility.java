package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.LinearSpline;

public final class TerrainShapeUtility extends EngineUtility {

    /*
     * Combines the continentalness, erosion and peaks-valleys noise fields with
     * the biome field to produce ground height. The noise is identical
     * everywhere; only each biome's curves differ, weighted by its share at the
     * position, plus a blended small-scale detail layer.
     */

    public static final LinearSpline DEFAULT_CONTINENTALNESS_SPLINE = new LinearSpline(
            EngineSetting.TERRAIN_CONTINENTALNESS_SPLINE_X,
            EngineSetting.TERRAIN_CONTINENTALNESS_SPLINE_HEIGHT_BLOCKS);

    public static final LinearSpline DEFAULT_EROSION_SPLINE = new LinearSpline(
            EngineSetting.TERRAIN_EROSION_SPLINE_X,
            EngineSetting.TERRAIN_EROSION_SPLINE_AMPLITUDE_BLOCKS);

    public static final LinearSpline DEFAULT_PEAKS_VALLEYS_SPLINE = new LinearSpline(
            EngineSetting.TERRAIN_PV_SPLINE_X,
            EngineSetting.TERRAIN_PV_SPLINE_CONTRIBUTION);

    public static float computeMacroShapeBlocks(
            long seed, double worldX, double worldZ, double worldWidthBlocks, double worldHeightBlocks,
            BiomeBlendStruct blend) {

        if (blend.isEmpty())
            return EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS;

        double spatialAngle = (worldX / worldWidthBlocks) * (Math.PI * 2.0);
        double cosAngle = Math.cos(spatialAngle);
        double sinAngle = Math.sin(spatialAngle);

        float continentalness = TerrainNoiseUtility.sampleFractal(
                seed ^ EngineSetting.TERRAIN_CONTINENTALNESS_SEED_SALT,
                cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                EngineSetting.TERRAIN_CONTINENTALNESS_WAVELENGTH_BLOCKS,
                EngineSetting.TERRAIN_CONTINENTALNESS_OCTAVES,
                EngineSetting.TERRAIN_CONTINENTALNESS_PERSISTENCE,
                EngineSetting.TERRAIN_CONTINENTALNESS_LACUNARITY);

        float erosion = TerrainNoiseUtility.sampleFractal(
                seed ^ EngineSetting.TERRAIN_EROSION_SEED_SALT,
                cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                EngineSetting.TERRAIN_EROSION_WAVELENGTH_BLOCKS,
                EngineSetting.TERRAIN_EROSION_OCTAVES,
                EngineSetting.TERRAIN_EROSION_PERSISTENCE,
                EngineSetting.TERRAIN_EROSION_LACUNARITY);

        float peaksValleysRaw = TerrainNoiseUtility.sampleFractal(
                seed ^ EngineSetting.TERRAIN_PV_SEED_SALT,
                cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                EngineSetting.TERRAIN_PV_WAVELENGTH_BLOCKS,
                EngineSetting.TERRAIN_PV_OCTAVES,
                EngineSetting.TERRAIN_PV_PERSISTENCE,
                EngineSetting.TERRAIN_PV_LACUNARITY);

        float ridge = 1f - Math.abs(peaksValleysRaw);

        float blendedHeight = 0f;

        for (int i = 0; i < blend.getCount(); i++) {

            BiomeHandle biome = blend.getBiome(i);

            float baseHeight = biome.getContinentalnessSpline().evaluate(continentalness);
            float erosionAmplitude = biome.getErosionSpline().evaluate(erosion);
            float peaksValleysContribution = biome.getPeaksValleysSpline().evaluate(ridge)
                    * erosionAmplitude;

            float heightAboveSeaLevel = (baseHeight - EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS)
                    + peaksValleysContribution;

            float biomeHeight = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS
                    + heightAboveSeaLevel * biome.getTerrainHeightScale();

            blendedHeight += biomeHeight * blend.getWeight(i);
        }

        return blendedHeight;
    }

    public static float computeDetailAmplitudeBlocks(BiomeBlendStruct blend) {

        float amplitude = 0f;

        for (int i = 0; i < blend.getCount(); i++) {
            BiomeHandle biome = blend.getBiome(i);
            amplitude += biome.getDetailAmplitudeBlocks() * biome.getTerrainHeightScale()
                    * blend.getWeight(i);
        }

        return amplitude;
    }

    public static float computeDetailWavelengthBlocks(BiomeBlendStruct blend) {

        if (blend.isEmpty())
            return EngineSetting.TERRAIN_DETAIL_WAVELENGTH_BLOCKS;

        float wavelength = 0f;

        for (int i = 0; i < blend.getCount(); i++)
            wavelength += blend.getBiome(i).getDetailWavelengthBlocks() * blend.getWeight(i);

        return wavelength;
    }

    public static float computeDetailBlocks(
            long seed, double worldX, double worldZ, double worldWidthBlocks, double worldHeightBlocks,
            float wavelengthBlocks, float amplitudeBlocks) {

        if (amplitudeBlocks == 0f)
            return 0f;

        double spatialAngle = (worldX / worldWidthBlocks) * (Math.PI * 2.0);
        double cosAngle = Math.cos(spatialAngle);
        double sinAngle = Math.sin(spatialAngle);

        float detail = TerrainNoiseUtility.sampleFractal(
                seed ^ EngineSetting.TERRAIN_DETAIL_SEED_SALT,
                cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                wavelengthBlocks,
                EngineSetting.TERRAIN_DETAIL_OCTAVES,
                EngineSetting.TERRAIN_DETAIL_PERSISTENCE,
                EngineSetting.TERRAIN_DETAIL_LACUNARITY);

        return detail * amplitudeBlocks;
    }

    public static int finalizeGroundHeightBlocks(float macroShapeBlocks, float detailBlocks) {
        return Math.round(clampGroundHeightBlocks(macroShapeBlocks, detailBlocks));
    }

    public static float clampGroundHeightBlocks(float macroShapeBlocks, float detailBlocks) {

        float finalHeight = macroShapeBlocks + detailBlocks;

        return Math.max(
                EngineSetting.TERRAIN_MIN_HEIGHT_BLOCKS,
                Math.min(EngineSetting.TERRAIN_MAX_HEIGHT_BLOCKS, finalHeight));
    }
}