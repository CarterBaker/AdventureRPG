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
     * weathermanager package (the overhead cloud grid, OverheadManager)
     * that needs to resolve weather at an arbitrary coordinate. A caller
     * that wants a smoothly-reblending value every frame (fog color,
     * distant cloud tint) can read low/high/blendFactor directly. A caller
     * that wants a stable, persistent identity instead (a physical cloud
     * object that shouldn't re-roll every frame) should read getPrimary()
     * once and hold onto the result itself — this struct never remembers
     * anything between calls; it is overwritten fresh every resolution.
     *
     * getPrimaryIntensity() derives a continuous "how strongly is the
     * currently-primary weather actually expressed here" value from the
     * same blendFactor, rather than adding any new state. Within pool
     * entry i's band, blendFactor sweeps 0 (the start of that entry's
     * share of the noise range) to 1 (the boundary where it crosses into
     * the next entry) — so intensity is highest at the "purest" point of
     * whichever side getPrimary() currently reports, and dips to exactly
     * zero right at the crossover point where primary identity would
     * flip. A caller holding a persistent identity (an overhead cell) can
     * sample this every frame to let that identity's presence visibly
     * strengthen and weaken as the underlying noise field drifts, without
     * ever needing to know it just crossed — or is about to cross — into
     * a neighboring weather's territory.
     *
     * getIntensityFor(handle) is the fix for a real bug that existed when
     * every caller had to fall back on getPrimaryIntensity() even for a
     * handle that was NOT (or was no longer) the currently-favored side: a
     * caller holding a persistent identity across many resolutions (an
     * overhead cell) needs the intensity of ITS OWN handle specifically,
     * not of whichever side the noise happens to favor on this particular
     * call. getPrimaryIntensity() silently answers the latter question —
     * fine for a caller that re-derives its identity from getPrimary()
     * every single call, but wrong for a caller that resolved getPrimary()
     * once, kept it, and is only checking back in later. getIntensityFor()
     * answers the former question directly, and returns 0 for a handle
     * that is neither the low nor high side of this band at all.
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

    /*
     * How strongly getPrimary()'s current answer is actually expressed at
     * this coordinate, in [0, 1]. 1.0 means noise sits at the purest point
     * of the primary weather's own share of the band; 0.0 means noise
     * sits exactly at the boundary where primary would flip to the other
     * side. Continuous and symmetric around that boundary, so a caller
     * sampling this repeatedly at the same coordinate sees the value rise
     * and fall smoothly as the underlying noise field evolves — the basis
     * for a physical weather system visibly strengthening, weakening, and
     * ultimately dissipating rather than only ever popping between fixed
     * states.
     */
    public float getPrimaryIntensity() {

        if (blendFactor < 0.5f)
            return 1f - (blendFactor / 0.5f);

        return (blendFactor - 0.5f) / 0.5f;
    }

    /*
     * Intensity of a SPECIFIC weather handle within this resolved band,
     * regardless of whether that handle currently reads as "primary". See
     * the class comment above for why this differs from
     * getPrimaryIntensity() and when a caller needs one over the other.
     * Returns 0 for a handle that is neither this band's low nor high side
     * — it simply has no presence at this resolution at all.
     */
    public float getIntensityFor(WeatherHandle handle) {

        if (handle == low)
            return Math.max(0f, 1f - (blendFactor / 0.5f));

        if (handle == high)
            return Math.max(0f, (blendFactor - 0.5f) / 0.5f);

        return 0f;
    }
}