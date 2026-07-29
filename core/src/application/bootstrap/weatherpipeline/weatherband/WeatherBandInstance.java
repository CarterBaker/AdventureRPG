package application.bootstrap.weatherpipeline.weatherband;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.InstancePackage;

public class WeatherBandInstance extends InstancePackage {

    /*
     * Raw result of resolving a world coordinate against a chance-weighted
     * weather pool. Exposes the discrete pair of weathers noise currently
     * sits between and how far across that pair's blend band it sits.
     * Recycled scratch — a caller wanting a stable, persistent identity
     * should read getPrimary() once and hold the result itself.
     */

    private WeatherHandle low;
    private WeatherHandle high;
    private float blendFactor;

    // Assignment \\

    public void assign(WeatherHandle low, WeatherHandle high, float blendFactor) {
        this.low = low;
        this.high = high;
        this.blendFactor = blendFactor;
    }

    // Accessible \\

    public WeatherHandle getLow() {
        return low;
    }

    public WeatherHandle getHigh() {
        return high;
    }

    public float getBlendFactor() {
        return blendFactor;
    }

    public WeatherHandle getPrimary() {
        return blendFactor < 0.5f ? low : high;
    }

    public float getPrimaryIntensity() {

        if (blendFactor < 0.5f)
            return 1f - (blendFactor / 0.5f);

        return (blendFactor - 0.5f) / 0.5f;
    }

    public float getIntensityFor(WeatherHandle handle) {

        if (handle == low)
            return Math.max(0f, 1f - (blendFactor / 0.5f));

        if (handle == high)
            return Math.max(0f, (blendFactor - 0.5f) / 0.5f);

        return 0f;
    }
}