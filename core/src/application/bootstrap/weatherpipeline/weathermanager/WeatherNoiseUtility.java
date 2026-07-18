package application.bootstrap.weatherpipeline.weathermanager;

import engine.root.EngineUtility;
import engine.util.mathematics.extras.NoiseUtility;

/*
 * Continuous 2D weather noise field, seamless across the world's X wrap and
 * non-repeating along Z. chunkX sits on a small circle in noise space — a
 * full lap always lands on the same (ex, ey), so the field is bit-exact
 * seamless across the wrap. The circle is small relative to a real lap, so
 * one noise feature spans a wide arc — that's what stretches weather into
 * long, current-like bands along the rotation axis instead of round blobs.
 * The meander term rides on that same embedding angle, so it travels around
 * the world as a wave rather than shifting every band by the same amount —
 * different longitudes wobble out of phase, giving the bands a genuine
 * current-like wander instead of straight parallel stripes.
 * chunkZ itself is folded in as an independent third dimension — never
 * embedded on the circle, so it never wraps or grows patterns with distance
 * — and sampled at a tighter frequency than X so bands stay narrow crossways.
 */
final class WeatherNoiseUtility extends EngineUtility {

    private static final double MIN_CYCLES_AROUND_WORLD = 4.0;
    private static final double CROSS_STREAM_COMPRESSION = 3.2;
    private static final double MACRO_FREQUENCY = 0.36;
    private static final float MACRO_WEIGHT = 0.55f;
    private static final double DETAIL_FREQUENCY = 3.2;
    private static final float DETAIL_WEIGHT = 0.26f;
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
            double driftZ,
            double meanderWaveNumber,
            double meanderAmplitudeChunks,
            double meanderPhase) {

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
        // long and thin rather than round. The meander wave rides on the
        // same embedding angle so its wobble travels with the rotation
        // rather than shifting every longitude in lockstep.
        double meander = Math.sin(angle * meanderWaveNumber + meanderPhase) * meanderAmplitudeChunks;
        double ez = (chunkZ + driftZ + meander) / (effectiveWavelength / CROSS_STREAM_COMPRESSION);

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