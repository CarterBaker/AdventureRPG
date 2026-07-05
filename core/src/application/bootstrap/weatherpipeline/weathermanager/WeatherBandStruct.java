package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.StructPackage;

public class WeatherBandStruct extends StructPackage {

    /*
     * Raw result of resolving a world coordinate against a chance-weighted
     * weather pool — see RegionSampleBranch.resolveBand() and
     * WeatherManager.resolveWeatherBand(). Exposes the discrete pair of
     * weathers noise currently sits between and how far across that pair's
     * blend band it sits.
     *
     * Public because it is the return channel for any system outside the
     * weathermanager package (an overhead cloud grid, eventually) that
     * needs to resolve weather at an arbitrary coordinate. A caller that
     * wants a smoothly-reblending value every frame (fog color, distant
     * cloud tint) can read low/high/blendFactor directly. A caller that
     * wants a stable, persistent identity instead (a physical cloud object
     * that shouldn't re-roll every frame) should read getPrimary() once and
     * hold onto the result itself — this struct never remembers anything
     * between calls; it is overwritten fresh every resolution.
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
     * The single weather this coordinate currently sits closer to —
     * whichever side of the band blendFactor has crossed. Intended for
     * callers that want one stable identity rather than a continuous
     * blend.
     */
    public WeatherHandle getPrimary() {
        return blendFactor < 0.5f ? low : high;
    }
}