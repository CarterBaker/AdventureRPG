package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.LinearSpline;

public final class TerrainShapeUtility extends EngineUtility {

        /*
         * Combines three independent, seamlessly-wrapped fractal noise fields —
         * continentalness, erosion, and peaks-valleys — into a macro ground-
         * height contribution per world column, the same layered approach
         * modern Minecraft uses for realistic macro terrain. Continentalness
         * alone decides the base height (so it alone draws the shoreline, at
         * the point its spline crosses sea level); erosion decides how much
         * amplitude peaks-valleys is allowed to contribute (mountainous where
         * erosion is low, flat where it's high); peaks-valleys — a ridged
         * transform of its own raw noise — spends that amplitude budget shaping
         * ridgelines and valleys. A separate high-frequency detail layer adds
         * small-scale surface roughness on top. None of this reads biome at
         * all, by design — biome only ever dresses the surface blocks
         * afterward, so painting a new biome onto the world PNG can never open
         * a seam in the terrain shape itself. Macro shape and detail are split
         * into separate entry points since they're tuned independently, but
         * WorldGenerationManager samples both computeMacroShapeBlocks() and
         * computeDetailBlocks() on their own coarse world-aligned grid and
         * bilinearly interpolates between samples rather than evaluating
         * either at full per-block resolution — see
         * EngineSetting.TERRAIN_MACRO_SAMPLE_STRIDE_BLOCKS and
         * TERRAIN_DETAIL_SAMPLE_STRIDE_BLOCKS for the stride each layer uses
         * and the error-margin reasoning behind it. terrainHeightScale is the
         * one biome-supplied knob into this otherwise biome-blind pipeline —
         * it scales the macro shape's deviation from sea level and the
         * detail layer's full amplitude alike, so a biome carrying 1.0
         * reproduces the exact height this class has always produced, and a
         * biome carrying 0.0 collapses to sea level everywhere with zero
         * variance, with no branch anywhere in this class aware that's what
         * happened.
         */

        private static final LinearSpline CONTINENTALNESS_HEIGHT_SPLINE = new LinearSpline(
                        EngineSetting.TERRAIN_CONTINENTALNESS_SPLINE_X,
                        EngineSetting.TERRAIN_CONTINENTALNESS_SPLINE_HEIGHT_BLOCKS);

        private static final LinearSpline EROSION_AMPLITUDE_SPLINE = new LinearSpline(
                        EngineSetting.TERRAIN_EROSION_SPLINE_X,
                        EngineSetting.TERRAIN_EROSION_SPLINE_AMPLITUDE_BLOCKS);

        private static final LinearSpline PEAKS_VALLEYS_SPLINE = new LinearSpline(
                        EngineSetting.TERRAIN_PV_SPLINE_X,
                        EngineSetting.TERRAIN_PV_SPLINE_CONTRIBUTION);

        private TerrainShapeUtility() {
                throw new AssertionError("Utility class cannot be instantiated");
        }

        public static float computeMacroShapeBlocks(
                        long seed, double worldX, double worldZ, double worldWidthBlocks, double worldHeightBlocks,
                        float terrainHeightScale) {

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

                float baseHeight = CONTINENTALNESS_HEIGHT_SPLINE.evaluate(continentalness);
                float erosionAmplitude = EROSION_AMPLITUDE_SPLINE.evaluate(erosion);
                float peaksValleysContribution = PEAKS_VALLEYS_SPLINE.evaluate(ridge) * erosionAmplitude;

                float heightAboveSeaLevel = (baseHeight - EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS)
                                + peaksValleysContribution;

                return EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS + heightAboveSeaLevel * terrainHeightScale;
        }

        public static float computeDetailBlocks(
                        long seed, double worldX, double worldZ, double worldWidthBlocks, double worldHeightBlocks,
                        float terrainHeightScale) {

                double spatialAngle = (worldX / worldWidthBlocks) * (Math.PI * 2.0);
                double cosAngle = Math.cos(spatialAngle);
                double sinAngle = Math.sin(spatialAngle);

                float detail = TerrainWrapNoiseUtility.sampleFractal(
                                seed ^ EngineSetting.TERRAIN_DETAIL_SEED_SALT,
                                cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                                EngineSetting.TERRAIN_DETAIL_WAVELENGTH_BLOCKS,
                                EngineSetting.TERRAIN_DETAIL_OCTAVES,
                                EngineSetting.TERRAIN_DETAIL_PERSISTENCE,
                                EngineSetting.TERRAIN_DETAIL_LACUNARITY);

                return detail * EngineSetting.TERRAIN_DETAIL_AMPLITUDE_BLOCKS * terrainHeightScale;
        }

        public static int finalizeGroundHeightBlocks(float macroShapeBlocks, float detailBlocks) {

                float finalHeight = macroShapeBlocks + detailBlocks;

                return Math.round(Math.max(
                                EngineSetting.TERRAIN_MIN_HEIGHT_BLOCKS,
                                Math.min(EngineSetting.TERRAIN_MAX_HEIGHT_BLOCKS, finalHeight)));
        }
}