package application.bootstrap.weatherpipeline.weathermanager;

import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;

/*
 * Continuous 2D weather noise field, seamless across the world's X wrap and
 * non-repeating along Z. chunkX is embedded on a small circle in noise
 * space — a full lap always lands on the exact same (ex, ey), so the field
 * is bit-exact seamless across the wrap regardless of anything else here.
 * Because the embedding circle is small relative to a real lap, one noise
 * feature spans a wide arc — that's what stretches weather into long,
 * current-like bands running along the rotation axis instead of round
 * blobs. chunkZ is folded in as a genuinely independent third noise
 * dimension — never embedded on the circle, so it can never distort the X
 * embedding and never itself wraps or grows patterns with distance — and is
 * sampled at a tighter frequency than X so bands stay narrow crossways.
 */
final class WeatherNoiseUtility extends EngineUtility {

    private static final double MIN_CYCLES_AROUND_WORLD = 4.0;
    private static final double CROSS_STREAM_COMPRESSION = 2.6;
    private static final double MACRO_FREQUENCY = 0.42;
    private static final float MACRO_WEIGHT = 0.55f;
    private static final double DETAIL_FREQUENCY = 3.2;
    private static final float DETAIL_WEIGHT = 0.32f;
    private static final long MACRO_SEED_MIX = 0x2545F4914F6CDD1DL;
    private static final long DETAIL_SEED_MIX = 0x9E3779B97F4A7C15L;

    private WeatherNoiseUtility() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    static float sample(
            long seed,
            double chunkX, double chunkZ,
            double worldWidthChunks,
            double wavelengthChunks,
            double rotationPhase,
            double driftZ) {

        double effectiveWavelength = Math.min(
                wavelengthChunks,
                Math.max(worldWidthChunks / MIN_CYCLES_AROUND_WORLD, 0.001));

        // Tangential (X) embedding — periodic by construction, and the
        // only place X frequency is set. Never touched by Z.
        double angle = (chunkX / worldWidthChunks) * (Math.PI * 2.0) + rotationPhase;
        double embeddingRadius = worldWidthChunks / (Math.PI * 2.0 * effectiveWavelength);
        double ex = Math.cos(angle) * embeddingRadius;
        double ey = Math.sin(angle) * embeddingRadius;

        // Cross-stream (Z) — an independent third dimension, sampled at a
        // tighter frequency than the tangential embedding so bands read as
        // long and thin rather than round.
        double ez = (chunkZ + driftZ) / (effectiveWavelength / CROSS_STREAM_COMPRESSION);

        float macro = NoiseUtility.noise3_ImproveXY(
                seed ^ MACRO_SEED_MIX, ex * MACRO_FREQUENCY, ey * MACRO_FREQUENCY, ez * MACRO_FREQUENCY);
        float base = NoiseUtility.noise3_ImproveXY(seed, ex, ey, ez);
        float detail = NoiseUtility.noise3_ImproveXY(
                seed ^ DETAIL_SEED_MIX, ex * DETAIL_FREQUENCY, ey * DETAIL_FREQUENCY, ez * DETAIL_FREQUENCY);

        float raw = base + macro * MACRO_WEIGHT + detail * DETAIL_WEIGHT;
        float normalized = raw / (1f + MACRO_WEIGHT + DETAIL_WEIGHT);

        return clamp01(normalized * 0.5f + 0.5f);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}