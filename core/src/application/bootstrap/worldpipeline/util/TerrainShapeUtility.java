package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.LinearSpline;
import application.bootstrap.worldpipeline.biome.BiomeHandle;

public final class TerrainShapeUtility extends EngineUtility {

        /*
         * Combines three shared, seamlessly-wrapped fractal noise fields —
         * continentalness, erosion, and peaks-valleys — with each nearby biome's
         * own response curves to produce a macro ground-height contribution per
         * world column. The noise fields are sampled identically everywhere
         * regardless of biome, so neighboring chunks never see a seam in the
         * underlying noise texture; only the curves that translate a sampled
         * value into a height or amplitude differ per biome, and those are
         * blended across a small kernel of nearby biomes (see BiomeManager.
         * getBiomeBlendWeights) rather than committed to hard the instant the
         * map crosses a biome boundary. Continentalness alone decides base
         * height (and therefore the shoreline, at the point its spline crosses
         * sea level); erosion decides how much amplitude peaks-valleys may
         * spend; peaks-valleys — a ridged transform of its own raw noise —
         * spends that budget shaping ridgelines and valleys. A separate
         * high-frequency detail layer adds small-scale roughness on top, with
         * its own biome-tunable amplitude and wavelength.
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

        private TerrainShapeUtility() {
                throw new AssertionError("Utility class cannot be instantiated");
        }

        /*
         * Blends terrain height across a small kernel of nearby biomes instead
         * of committing to one biome's curve outright. continentalness/erosion/
         * peaks-valleys noise is sampled exactly once — it is a shared spatial
         * field, identical for every biome at this position — then each
         * candidate biome's own splines are evaluated against that same sample
         * and weight-summed into the final height. This is what keeps a
         * chunk's height from snapping the instant the map crosses a biome
         * boundary: neighboring chunks share most of the same kernel and
         * therefore land on nearly the same blended result.
         */
        public static float computeMacroShapeBlocks(
                        long seed, double worldX, double worldZ, double worldWidthBlocks, double worldHeightBlocks,
                        BiomeHandle[] blendBiomes, float[] blendWeights) {

                double spatialAngle = (worldX / worldWidthBlocks) * (Math.PI * 2.0);
                double cosAngle = Math.cos(spatialAngle);
                double sinAngle = Math.sin(spatialAngle);

                float continentalness = TerrainWrapNoiseUtility.sampleFractal(
                                seed ^ EngineSetting.TERRAIN_CONTINENTALNESS_SEED_SALT,
                                cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                                EngineSetting.TERRAIN_CONTINENTALNESS_WAVELENGTH_BLOCKS,
                                EngineSetting.TERRAIN_CONTINENTALNESS_OCTAVES,
                                EngineSetting.TERRAIN_CONTINENTALNESS_PERSISTENCE,
                                EngineSetting.TERRAIN_CONTINENTALNESS_LACUNARITY);

                float erosion = TerrainWrapNoiseUtility.sampleFractal(
                                seed ^ EngineSetting.TERRAIN_EROSION_SEED_SALT,
                                cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                                EngineSetting.TERRAIN_EROSION_WAVELENGTH_BLOCKS,
                                EngineSetting.TERRAIN_EROSION_OCTAVES,
                                EngineSetting.TERRAIN_EROSION_PERSISTENCE,
                                EngineSetting.TERRAIN_EROSION_LACUNARITY);

                float peaksValleysRaw = TerrainWrapNoiseUtility.sampleFractal(
                                seed ^ EngineSetting.TERRAIN_PV_SEED_SALT,
                                cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                                EngineSetting.TERRAIN_PV_WAVELENGTH_BLOCKS,
                                EngineSetting.TERRAIN_PV_OCTAVES,
                                EngineSetting.TERRAIN_PV_PERSISTENCE,
                                EngineSetting.TERRAIN_PV_LACUNARITY);

                float ridge = 1f - Math.abs(peaksValleysRaw);

                float blendedHeight = 0f;

                for (int i = 0; i < blendBiomes.length; i++) {

                        BiomeHandle biome = blendBiomes[i];

                        float baseHeight = biome.getContinentalnessSpline().evaluate(continentalness);
                        float erosionAmplitude = biome.getErosionSpline().evaluate(erosion);
                        float peaksValleysContribution = biome.getPeaksValleysSpline().evaluate(ridge)
                                        * erosionAmplitude;

                        float heightAboveSeaLevel = (baseHeight - EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS)
                                        + peaksValleysContribution;

                        float biomeHeight = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS
                                        + heightAboveSeaLevel * biome.getTerrainHeightScale();

                        blendedHeight += biomeHeight * blendWeights[i];
                }

                return blendedHeight;
        }

        /*
         * Small-scale surface roughness. Wavelength and amplitude are blended
         * as scalar parameters across the same biome kernel used for macro
         * shape, then sampled once with the blended values — cheaper than
         * re-sampling a separate detail noise field per candidate biome, and
         * the amplitude here is small enough (a few blocks) that blending the
         * parameters rather than the noise output is visually indistinguishable.
         */
        public static float computeDetailBlocks(
                        long seed, double worldX, double worldZ, double worldWidthBlocks, double worldHeightBlocks,
                        BiomeHandle[] blendBiomes, float[] blendWeights) {

                double spatialAngle = (worldX / worldWidthBlocks) * (Math.PI * 2.0);
                double cosAngle = Math.cos(spatialAngle);
                double sinAngle = Math.sin(spatialAngle);

                float blendedWavelength = 0f;
                float blendedAmplitude = 0f;

                for (int i = 0; i < blendBiomes.length; i++) {
                        BiomeHandle biome = blendBiomes[i];
                        float weight = blendWeights[i];
                        blendedWavelength += biome.getDetailWavelengthBlocks() * weight;
                        blendedAmplitude += biome.getDetailAmplitudeBlocks() * biome.getTerrainHeightScale() * weight;
                }

                float detail = TerrainWrapNoiseUtility.sampleFractal(
                                seed ^ EngineSetting.TERRAIN_DETAIL_SEED_SALT,
                                cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                                blendedWavelength,
                                EngineSetting.TERRAIN_DETAIL_OCTAVES,
                                EngineSetting.TERRAIN_DETAIL_PERSISTENCE,
                                EngineSetting.TERRAIN_DETAIL_LACUNARITY);

                return detail * blendedAmplitude;
        }

        public static int finalizeGroundHeightBlocks(float macroShapeBlocks, float detailBlocks) {

                float finalHeight = macroShapeBlocks + detailBlocks;

                return Math.round(Math.max(
                                EngineSetting.TERRAIN_MIN_HEIGHT_BLOCKS,
                                Math.min(EngineSetting.TERRAIN_MAX_HEIGHT_BLOCKS, finalHeight)));
        }
}