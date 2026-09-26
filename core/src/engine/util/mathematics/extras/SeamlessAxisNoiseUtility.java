package engine.util.mathematics.extras;

import engine.root.EngineUtility;

public final class SeamlessAxisNoiseUtility extends EngineUtility {

    /*
     * Wraps a 3D noise field seamlessly along one linear axis by blending
     * toward the one-period-back sample only within a thin margin of the seam,
     * so everywhere else costs a single sample. sample3D() inlines the blend
     * for the terrain hot path.
     */

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