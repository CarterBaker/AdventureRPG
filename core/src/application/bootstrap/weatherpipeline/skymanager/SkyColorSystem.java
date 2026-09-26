package application.bootstrap.weatherpipeline.skymanager;

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
     * The single source of sky and cloud color. Each frame resolves the season
     * palette, then per grid places it at the grid's solar elevation, tints it
     * by temperature and local weather, and gives each day a seeded character
     * that eases into the next. Writes the result to the grid's SkyColorData
     * UBO.
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

    // Working Temperature
    private float cold;
    private float heat;

    // Daily Character
    private float dailyGlowHue;
    private float dailyBeltHue;
    private float dailyDomeHue;
    private float dailyCloudHue;
    private float dailySaturation;
    private float dailyGlowStrength;
    private float dailyBeltStrength;
    private float dailyAir;

    // Base \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.clockManager = get(ClockManager.class);
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
        resolveDailyCharacter();

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
        applyDailyCharacter();
        applyWeather(grid.getWeatherInstance());
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

    private void applyTemperature(float temperature) {

        this.cold = 1f - SkyColorUtility.remapClamped(
                temperature,
                EngineSetting.SKY_TEMPERATURE_COLD_REFERENCE,
                EngineSetting.SKY_TEMPERATURE_MILD_REFERENCE);
        this.heat = SkyColorUtility.remapClamped(
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

    // Daily Character \\

    private void resolveDailyCharacter() {

        this.dailyGlowHue = clockManager.getDailyRandom(EngineSetting.SKY_DAILY_STREAM_GLOW_HUE);
        this.dailyBeltHue = clockManager.getDailyRandom(EngineSetting.SKY_DAILY_STREAM_BELT_HUE);
        this.dailyDomeHue = clockManager.getDailyRandom(EngineSetting.SKY_DAILY_STREAM_DOME_HUE);
        this.dailyCloudHue = clockManager.getDailyRandom(EngineSetting.SKY_DAILY_STREAM_CLOUD_HUE);
        this.dailySaturation = clockManager.getDailyRandom(EngineSetting.SKY_DAILY_STREAM_SATURATION);
        this.dailyGlowStrength = clockManager.getDailyRandom(EngineSetting.SKY_DAILY_STREAM_GLOW_STRENGTH);
        this.dailyBeltStrength = clockManager.getDailyRandom(EngineSetting.SKY_DAILY_STREAM_BELT_STRENGTH);
        this.dailyAir = clockManager.getDailyRandom(EngineSetting.SKY_DAILY_STREAM_AIR);
    }

    private void applyDailyCharacter() {

        float variety = skyPaletteBranch.getVariety();

        float glowHue = dailyGlowHue * EngineSetting.SKY_DAILY_GLOW_HUE_DEGREES * variety
                - cold * EngineSetting.SKY_DAILY_COLD_HUE_BIAS_DEGREES
                - heat * EngineSetting.SKY_DAILY_HOT_HUE_BIAS_DEGREES;
        float beltHue = dailyBeltHue * EngineSetting.SKY_DAILY_BELT_HUE_DEGREES * variety
                - cold * EngineSetting.SKY_DAILY_COLD_HUE_BIAS_DEGREES;
        float domeHue = dailyDomeHue * EngineSetting.SKY_DAILY_DOME_HUE_DEGREES * variety;
        float cloudHue = dailyCloudHue * EngineSetting.SKY_DAILY_CLOUD_HUE_DEGREES * variety;

        SkyColorUtility.rotateHue(phase.getGlow(), glowHue);
        SkyColorUtility.rotateHue(phase.getBelt(), beltHue);
        SkyColorUtility.rotateHue(phase.getZenith(), domeHue);
        SkyColorUtility.rotateHue(phase.getHorizon(), domeHue);
        SkyColorUtility.rotateHue(phase.getCloudLight(), cloudHue);
        SkyColorUtility.rotateHue(phase.getCloudShadow(), cloudHue);

        float saturation = 1f + dailySaturation * EngineSetting.SKY_DAILY_SATURATION_RANGE * variety;

        SkyColorUtility.saturate(phase.getZenith(), saturation);
        SkyColorUtility.saturate(phase.getHorizon(), saturation);
        SkyColorUtility.saturate(phase.getGlow(), saturation);
        SkyColorUtility.saturate(phase.getBelt(), saturation);

        this.glowStrength *= 1f + dailyGlowStrength * EngineSetting.SKY_DAILY_GLOW_STRENGTH_RANGE * variety;
        this.beltStrength *= 1f + dailyBeltStrength * EngineSetting.SKY_DAILY_BELT_STRENGTH_RANGE * variety;

        applyDailyAir(variety);
    }

    private void applyDailyAir(float variety) {

        float dust = SkyColorUtility.clamp01(dailyAir + heat * EngineSetting.SKY_DAILY_HOT_DUST_BIAS) * variety;
        float crisp = SkyColorUtility.clamp01(-dailyAir + cold * EngineSetting.SKY_DAILY_COLD_CRISP_BIAS) * variety;

        this.glowStrength *= 1f + dust * EngineSetting.SKY_DAILY_DUST_GLOW_BOOST;

        SkyColorUtility.haze(phase.getHorizon(), dust * EngineSetting.SKY_DAILY_DUST_HAZE * daylight);
        SkyColorUtility.saturate(phase.getZenith(), 1f + crisp * EngineSetting.SKY_DAILY_CRISP_CLARITY);
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
