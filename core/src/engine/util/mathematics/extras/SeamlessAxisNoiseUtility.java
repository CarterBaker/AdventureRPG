package engine.util.mathematics.extras;

import engine.root.EngineUtility;

public final class SeamlessAxisNoiseUtility extends EngineUtility {

    /*
     * Shared seamless axis-wrap helper for any 3D noise field that needs to
     * tile cleanly across one linear axis (world Z for terrain, the
     * cross-stream axis for weather) while a second axis is already wrapped
     * exactly via circular embedding. Rather than blending the direct
     * sample against the "one period back" sample across the entire axis
     * range — which pays for two full noise evaluations everywhere and
     * bleeds two unrelated samples together across the whole map, not just
     * at the seam — this only blends within a thin margin approaching the
     * true wrap point, sized in multiples of the sampled wavelength so the
     * transition itself reads as more noise rather than a visible seam.
     * Everywhere else, a single direct sample is used at full speed with no
     * quality tradeoff versus a plain non-wrapping sample. sample3D() inlines
     * that same blend for the noise3_ImproveXY case without an AxisSampler
     * closure, since terrain rides this path thousands of times per column.
     */

    private SeamlessAxisNoiseUtility() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    @FunctionalInterface
    public interface AxisSampler {
        float sample(double axisNoiseSpaceValue);
    }

    public static float sample(
            double rawAxis,
            double axisWavelength,
            double wrapPeriod,
            double marginWavelengths,
            AxisSampler sampler) {

        if (wrapPeriod <= 0.0)
            return sampler.sample(rawAxis / axisWavelength);

        double axis = wrapIntoRange(rawAxis, wrapPeriod);
        double margin = Math.min(axisWavelength * marginWavelengths, wrapPeriod * 0.5);
        double distanceFromSeam = wrapPeriod - axis;

        if (margin <= 0.0 || distanceFromSeam >= margin)
            return sampler.sample(axis / axisWavelength);

        float direct = sampler.sample(axis / axisWavelength);
        float wrapped = sampler.sample((axis - wrapPeriod) / axisWavelength);

        float t = (float) (1.0 - distanceFromSeam / margin);
        float eased = t * t * (3f - 2f * t);

        return direct * (1f - eased) + wrapped * eased;
    }

    public static float sample3D(
            double rawAxis,
            double axisWavelength,
            double wrapPeriod,
            double marginWavelengths,
            long seed,
            double ex,
            double ey) {

        if (wrapPeriod <= 0.0)
            return NoiseUtility.noise3_ImproveXY(seed, ex, ey, rawAxis / axisWavelength);

        double axis = wrapIntoRange(rawAxis, wrapPeriod);
        double margin = Math.min(axisWavelength * marginWavelengths, wrapPeriod * 0.5);
        double distanceFromSeam = wrapPeriod - axis;

        if (margin <= 0.0 || distanceFromSeam >= margin)
            return NoiseUtility.noise3_ImproveXY(seed, ex, ey, axis / axisWavelength);

        float direct = NoiseUtility.noise3_ImproveXY(seed, ex, ey, axis / axisWavelength);
        float wrapped = NoiseUtility.noise3_ImproveXY(seed, ex, ey, (axis - wrapPeriod) / axisWavelength);

        float t = (float) (1.0 - distanceFromSeam / margin);
        float eased = t * t * (3f - 2f * t);

        return direct * (1f - eased) + wrapped * eased;
    }

    private static double wrapIntoRange(double value, double range) {
        double wrapped = value % range;
        if (wrapped < 0)
            wrapped += range;
        return wrapped;
    }
}