package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.StructPackage;

public class WeatherBandStruct extends StructPackage {

    /*
     * Raw result of resolving a world coordinate against a chance-weighted
     * weather pool. Exposes the discrete pair of weathers noise currently
     * sits between and how far across that pair's blend band it sits.
     * Never remembers anything between calls — a caller that wants a
     * stable, persistent identity should read getPrimary() once and hold
     * the result itself.
     */

    private WeatherHandle low;
    private WeatherHandle high;
    private float blendFactor;

    // Internal \\

    void set(WeatherHandle low, WeatherHandle high, float blendFactor) {
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

    /*
     * The single weather this coordinate currently sits closer to.
     */
    public WeatherHandle getPrimary() {
        return blendFactor < 0.5f ? low : high;
    }

    /*
     * How strongly getPrimary()'s current answer is expressed at this
     * coordinate, in [0, 1] — 1.0 at the purest point of the primary
     * weather's own share of the band, 0.0 right at the boundary where
     * primary would flip.
     */
    public float getPrimaryIntensity() {

        if (blendFactor < 0.5f)
            return 1f - (blendFactor / 0.5f);

        return (blendFactor - 0.5f) / 0.5f;
    }

    /*
     * Intensity of a specific weather handle within this resolved band,
     * regardless of whether it currently reads as primary. Returns 0 for a
     * handle that is neither this band's low nor high side.
     */
    public float getIntensityFor(WeatherHandle handle) {

        if (handle == low)
            return Math.max(0f, 1f - (blendFactor / 0.5f));

        if (handle == high)
            return Math.max(0f, (blendFactor - 0.5f) / 0.5f);

        return 0f;
    }
}