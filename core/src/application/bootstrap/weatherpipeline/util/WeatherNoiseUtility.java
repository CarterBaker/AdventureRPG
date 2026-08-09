// WeatherNoiseUtility.java
package application.bootstrap.weatherpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;
import engine.util.mathematics.extras.SeamlessAxisNoiseUtility;

public final class WeatherNoiseUtility extends EngineUtility {

        /*
         * Continuous 2D weather noise field sampled per chunk coordinate. World
         * X wraps by riding the same trick that already makes it scroll with
         * the world's rotation: chunkX is embedded as a point on a circle
         * before sampling, so a full lap always lands back on the same value.
         * World Z wraps through the shared SeamlessAxisNoiseUtility — the same
         * primitive TerrainWrapNoiseUtility uses — which blends only within a
         * thin margin of the true seam instead of across the whole map, so
         * crossing it never pops while the rest of the world pays for a single
         * layered sample instead of two.
         *
         * All shaping constants (frequencies, weights, the two hash-mixing
         * salts) live on EngineSetting's WEATHER_NOISE_* / WEATHER_HASH_SALT_*
         * fields — nothing tunable is declared locally in this file.
         */

        private WeatherNoiseUtility() {
                throw new AssertionError("Utility class cannot be instantiated");
        }

        public static float sample(
                        long seed,
                        double chunkX, double chunkZ,
                        double worldWidthChunks,
                        double worldHeightChunks,
                        double wavelengthChunks,
                        double rotationPhase,
                        double driftZ,
                        double meanderWaveNumber,
                        double meanderAmplitudeChunks,
                        double meanderPhase) {

                double effectiveWavelength = Math.min(
                                wavelengthChunks,
                                Math.max(worldWidthChunks / EngineSetting.WEATHER_NOISE_MIN_CYCLES_AROUND_WORLD,
                                                0.001));

                double spatialAngle = (chunkX / worldWidthChunks) * (Math.PI * 2.0);
                double angle = spatialAngle + rotationPhase;

                double embeddingRadius = worldWidthChunks / (Math.PI * 2.0 * effectiveWavelength);
                double ex = Math.cos(angle) * embeddingRadius;
                double ey = Math.sin(angle) * embeddingRadius;

                double meander = Math.sin(spatialAngle * meanderWaveNumber + meanderPhase) * meanderAmplitudeChunks;
                double zWavelength = effectiveWavelength / EngineSetting.WEATHER_NOISE_CROSS_STREAM_COMPRESSION;
                double rawZ = chunkZ + driftZ + meander;

                float raw = SeamlessAxisNoiseUtility.sample(
                                rawZ, zWavelength, worldHeightChunks, EngineSetting.NOISE_SEAM_BLEND_WAVELENGTHS,
                                ez -> layeredNoise(seed, ex, ey, ez));

                return clamp01(raw * 0.5f + 0.5f);
        }

        private static float layeredNoise(long seed, double ex, double ey, double ez) {

                float macro = NoiseUtility.noise3_ImproveXY(
                                seed ^ EngineSetting.WEATHER_HASH_SALT_PRIMARY,
                                ex * EngineSetting.WEATHER_NOISE_MACRO_FREQUENCY,
                                ey * EngineSetting.WEATHER_NOISE_MACRO_FREQUENCY,
                                ez * EngineSetting.WEATHER_NOISE_MACRO_FREQUENCY);
                float base = NoiseUtility.noise3_ImproveXY(seed, ex, ey, ez);
                float detail = NoiseUtility.noise3_ImproveXY(
                                seed ^ EngineSetting.WEATHER_HASH_SALT_SECONDARY,
                                ex * EngineSetting.WEATHER_NOISE_DETAIL_FREQUENCY,
                                ey * EngineSetting.WEATHER_NOISE_DETAIL_FREQUENCY,
                                ez * EngineSetting.WEATHER_NOISE_DETAIL_FREQUENCY);

                float combined = base + macro * EngineSetting.WEATHER_NOISE_MACRO_WEIGHT
                                + detail * EngineSetting.WEATHER_NOISE_DETAIL_WEIGHT;

                return combined
                                / (1f + EngineSetting.WEATHER_NOISE_MACRO_WEIGHT
                                                + EngineSetting.WEATHER_NOISE_DETAIL_WEIGHT);
        }

        private static float clamp01(float v) {
                return Math.max(0f, Math.min(1f, v));
        }
}