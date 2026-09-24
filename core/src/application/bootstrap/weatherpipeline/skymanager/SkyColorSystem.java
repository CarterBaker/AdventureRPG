package application.bootstrap.weatherpipeline.skymanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.season.SkyPhaseStruct;
import application.bootstrap.weatherpipeline.util.SkyColorUtility;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SkyColorSystem extends SystemPackage {

    /*
     * The single authoritative source of every sky and cloud color. Each
     * frame the season-blended palette is resolved once, then for every
     * active grid it is placed at that grid's own solar elevation, pushed
     * cooler and pinker by cold air or warmer and hazier by heat, greyed by
     * overcast and humid local weather, and nudged by the clock's smoothly
     * varying daily noise. The result — dome gradient, sun-side glow,
     * anti-solar belt, three cloud tints, fog, and the blend strengths the
     * shaders scale them by — goes into that grid's SkyColorData UBO.
     */

    // Temperature Accents
    private static final Vector3 COLD_ACCENT = new Vector3(
            EngineSetting.SKY_TEMPERATURE_COLD_ACCENT_R,
            EngineSetting.SKY_TEMPERATURE_COLD_ACCENT_G,
            EngineSetting.SKY_TEMPERATURE_COLD_ACCENT_B);
    private static final Vector3 HOT_ACCENT = new Vector3(
            EngineSetting.SKY_TEMPERATURE_HOT_ACCENT_R,
            EngineSetting.SKY_TEMPERATURE_HOT_ACCENT_G,
            EngineSetting.SKY_TEMPERATURE_HOT_ACCENT_B);

    // Internal
    private UBOManager uboManager;
    private WorldStreamManager worldStreamManager;
    private ClockManager clockManager;
    private ClockHandle clockHandle;
    private SkyPaletteBranch skyPaletteBranch;

    // Working Palette
    private SkyPhaseStruct phase;
    private final Vector3 fogColor = new Vector3();
    private final Vector4 blend = new Vector4();

    // Working Strengths
    private float glowStrength;
    private float beltStrength;
    private float daylight;
    private float overcast;

    // Base \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.clockManager = get(ClockManager.class);
    }

    @Override
    protected void awake() {
        this.clockHandle = clockManager.getClockHandle();
    }

    // Assignment \\

    void assignData(SkyPaletteBranch skyPaletteBranch) {
        this.skyPaletteBranch = skyPaletteBranch;
        this.phase = skyPaletteBranch.createPhaseBuffer();
    }

    // Update \\

    @Override
    protected void update() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        int size = grids.size();

        if (size == 0)
            return;

        skyPaletteBranch.resolvePalette();

        Object[] elements = grids.elements();

        for (int i = 0; i < size; i++)
            pushData((GridInstance) elements[i]);
    }

    // Push \\

    private void pushData(GridInstance grid) {

        float solarElevation = (float) grid.getClockInstance().getSolarElevation();

        skyPaletteBranch.resolvePhase(solarElevation, phase);

        resolveCycle(solarElevation);
        applyTemperature(grid.getTemperatureInstance().getTemperature());
        applyWeather(grid.getWeatherInstance());
        applyDailyVariation(clockHandle.getRandomNoiseFromDay());
        resolveFog();

        UBOInstance skyColorUBO = grid.getSkyColorUBO();

        skyColorUBO.updateUniform(EngineSetting.UNIFORM_SKY_ZENITH_COLOR, phase.getZenith());
        skyColorUBO.updateUniform(EngineSetting.UNIFORM_SKY_HORIZON_COLOR, phase.getHorizon());
        skyColorUBO.updateUniform(EngineSetting.UNIFORM_SKY_GLOW_COLOR, phase.getGlow());
        skyColorUBO.updateUniform(EngineSetting.UNIFORM_SKY_BELT_COLOR, phase.getBelt());
        skyColorUBO.updateUniform(EngineSetting.UNIFORM_SKY_CLOUD_COLOR, phase.getCloud());
        skyColorUBO.updateUniform(EngineSetting.UNIFORM_SKY_CLOUD_LIGHT_COLOR, phase.getCloudLight());
        skyColorUBO.updateUniform(EngineSetting.UNIFORM_SKY_CLOUD_SHADOW_COLOR, phase.getCloudShadow());
        skyColorUBO.updateUniform(EngineSetting.UNIFORM_SKY_FOG_COLOR, fogColor);
        skyColorUBO.updateUniform(EngineSetting.UNIFORM_SKY_BLEND, blend.set(
                glowStrength, beltStrength, daylight, overcast));
        uboManager.push(skyColorUBO);
    }

    // Cycle \\

    /*
     * Glow peaks with the sun just below the horizon and lingers into
     * golden hour; the anti-solar belt lives only in the short band either
     * side of sunset and sunrise. Both are scaled by the season's own
     * strengths, and daylight ramps from late twilight to full day.
     */
    private void resolveCycle(float solarElevation) {

        this.glowStrength = skyPaletteBranch.getGlowStrength() * SkyColorUtility.bell(
                solarElevation,
                EngineSetting.SKY_GLOW_ELEVATION_START,
                EngineSetting.SKY_GLOW_ELEVATION_PEAK,
                EngineSetting.SKY_GLOW_ELEVATION_END);

        this.beltStrength = skyPaletteBranch.getBeltStrength() * SkyColorUtility.bell(
                solarElevation,
                EngineSetting.SKY_BELT_ELEVATION_START,
                EngineSetting.SKY_BELT_ELEVATION_PEAK,
                EngineSetting.SKY_BELT_ELEVATION_END);

        this.daylight = SkyColorUtility.smoothstep(
                EngineSetting.SKY_DAYLIGHT_ELEVATION_START,
                EngineSetting.SKY_DAYLIGHT_ELEVATION_END,
                solarElevation);
    }

    // Temperature \\

    /*
     * Cold air pulls the glow, belt, and sunlit cloud edges toward a pastel
     * pink, strengthens the belt, and clears the zenith to a deeper blue —
     * the cotton-candy winter sky. Heat pulls them toward amber, strengthens
     * the glow, and washes the horizon into a pale haze.
     */
    private void applyTemperature(float temperature) {

        float cold = 1f - SkyColorUtility.remapClamped(
                temperature,
                EngineSetting.SKY_TEMPERATURE_COLD_REFERENCE,
                EngineSetting.SKY_TEMPERATURE_MILD_REFERENCE);
        float heat = SkyColorUtility.remapClamped(
                temperature,
                EngineSetting.SKY_TEMPERATURE_MILD_REFERENCE,
                EngineSetting.SKY_TEMPERATURE_HOT_REFERENCE);

        float accent = EngineSetting.SKY_TEMPERATURE_ACCENT_STRENGTH;
        float cloudAccent = SkyColorUtility.clamp01(glowStrength) * EngineSetting.SKY_CLOUD_COLOR_ACCENT_STRENGTH;

        SkyColorUtility.lerpTowards(phase.getGlow(), COLD_ACCENT, cold * accent);
        SkyColorUtility.lerpTowards(phase.getGlow(), HOT_ACCENT, heat * accent);
        SkyColorUtility.lerpTowards(phase.getBelt(), COLD_ACCENT, cold * accent);
        SkyColorUtility.lerpTowards(phase.getCloudLight(), COLD_ACCENT, cold * cloudAccent);
        SkyColorUtility.lerpTowards(phase.getCloudLight(), HOT_ACCENT, heat * cloudAccent);

        SkyColorUtility.saturate(phase.getZenith(), 1f + cold * EngineSetting.SKY_TEMPERATURE_COLD_CLARITY);
        SkyColorUtility.haze(phase.getHorizon(), heat * EngineSetting.SKY_TEMPERATURE_HOT_HAZE * daylight);

        this.beltStrength *= 1f + cold * EngineSetting.SKY_TEMPERATURE_COLD_BELT_BOOST;
        this.glowStrength *= 1f + heat * EngineSetting.SKY_TEMPERATURE_HOT_GLOW_BOOST;
    }

    // Weather \\

    /*
     * Cloud cover and precipitation grey and dim the dome, damp the
     * twilight colors, and deepen cloud shade; humidity hazes the horizon.
     * A grid whose local weather has not been placed yet reads as clear.
     */
    private void applyWeather(WeatherInstance weather) {

        if (!weather.isConfigured()) {
            this.overcast = 0f;
            return;
        }

        this.overcast = SkyColorUtility.clamp01(
                weather.getBlendedCloudCoverage() * EngineSetting.SKY_OVERCAST_COVERAGE_WEIGHT
                        + weather.getBlendedPrecipitationIntensity()
                                * EngineSetting.SKY_OVERCAST_PRECIPITATION_WEIGHT);

        float saturation = 1f - overcast * EngineSetting.SKY_OVERCAST_DESATURATION;
        float dimming = 1f - overcast * EngineSetting.SKY_OVERCAST_DIMMING;
        float damping = 1f - overcast * EngineSetting.SKY_OVERCAST_GLOW_DAMPING;

        SkyColorUtility.saturate(phase.getZenith(), saturation);
        SkyColorUtility.saturate(phase.getHorizon(), saturation);
        SkyColorUtility.saturate(phase.getGlow(), saturation);
        SkyColorUtility.saturate(phase.getBelt(), saturation);

        phase.getZenith().multiply(dimming);
        phase.getHorizon().multiply(dimming);
        phase.getCloudShadow().multiply(1f - overcast * EngineSetting.SKY_OVERCAST_CLOUD_SHADE);

        SkyColorUtility.haze(
                phase.getHorizon(),
                weather.getBlendedHumidity() * EngineSetting.SKY_HUMIDITY_HAZE * daylight);

        this.glowStrength *= damping;
        this.beltStrength *= damping;
    }

    // Daily Variation \\

    /*
     * The clock's daily noise eases continuously from one day's value into
     * the next, so every term here is a smooth function of it: a gentle
     * rotating hue offset across the daytime dome and a small swing in how
     * vivid each day's glow and belt are.
     */
    private void applyDailyVariation(float dailyNoise) {

        double angle = dailyNoise * Math.PI * 2.0;

        float offsetR = (float) Math.cos(angle) * EngineSetting.SKY_DAILY_OFFSET_R * daylight;
        float offsetG = (float) Math.sin(angle) * EngineSetting.SKY_DAILY_OFFSET_G * daylight;
        float offsetB = (float) Math.cos(angle + EngineSetting.SKY_DAILY_OFFSET_B_PHASE)
                * EngineSetting.SKY_DAILY_OFFSET_B * daylight;

        phase.getZenith().add(offsetR, offsetG, offsetB);
        phase.getHorizon().add(offsetR, offsetG, offsetB);
        SkyColorUtility.clampPositive(phase.getZenith());
        SkyColorUtility.clampPositive(phase.getHorizon());

        this.glowStrength *= 1f + (dailyNoise - 0.5f) * EngineSetting.SKY_DAILY_GLOW_VARIANCE;
        this.beltStrength *= 1f + (float) Math.sin(angle) * 0.5f * EngineSetting.SKY_DAILY_BELT_VARIANCE;
    }

    // Fog \\

    private void resolveFog() {

        fogColor.set(phase.getHorizon());
        SkyColorUtility.lerpTowards(
                fogColor,
                phase.getGlow(),
                SkyColorUtility.clamp01(glowStrength) * EngineSetting.SKY_FOG_GLOW_TRANSFER);
        fogColor.add(EngineSetting.SKY_FOG_COLOR_LIFT * daylight);
    }
}
