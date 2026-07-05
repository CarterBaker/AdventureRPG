package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.season.SeasonHandle;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class TemperatureBranch extends BranchPackage {

    /*
     * Computes live ambient temperature from the active named season's
     * base temperature and variance (see SeasonData.getBaseTemperature()/
     * getTemperatureVariance()), shaped by a diurnal day/night curve and a
     * slow deterministic drift, then cooled by the current precipitation
     * intensity — a storm measurably drops the temperature, a clear sky
     * does not.
     *
     * CPU-side only for now — nothing on the GPU reads temperature yet.
     * Gameplay systems (cold damage, freezing water, crop growth, whatever
     * else needs it) read WeatherManager.getCurrentTemperature() directly.
     * If a shader effect ever needs it (a frost overlay, breath fog that
     * reacts to cold, etc.), push it through a TemperatureData UBO at that
     * point, mirroring TimeData's own .glsl + .json pair — trivial to add
     * later, not worth plumbing before anything actually consumes it.
     *
     * Driven explicitly by WeatherManager.update() rather than its own
     * automatic update() cascade, since it needs that same frame's freshly
     * sampled center weather sample from RegionSampleBranch as an explicit
     * parameter — mirrors how ClockManager's tracker branches are driven
     * explicitly rather than relying on cascade ordering.
     */

    // Internal
    private ClockManager clockManager;
    private SeasonManager seasonManager;

    // Season Tracking
    private String lastSeasonName;
    private SeasonHandle activeSeason;

    // Time
    private float elapsedTime;

    // Output
    private float currentTemperature;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.clockManager = get(ClockManager.class);
        this.seasonManager = get(SeasonManager.class);
    }

    // Temperature \\

    void updateTemperature(WeatherSampleStruct centerSample) {

        elapsedTime += internal.getDeltaTime();

        resolveActiveSeason();

        float baseTemperature = EngineSetting.DEFAULT_BASE_TEMPERATURE;
        float temperatureVariance = 0f;

        if (activeSeason != null) {
            baseTemperature = activeSeason.getBaseTemperature();
            temperatureVariance = activeSeason.getTemperatureVariance();
        }

        float diurnalOffset = computeDiurnalOffset() * temperatureVariance;
        float driftOffset = (float) Math.sin(elapsedTime * EngineSetting.TEMPERATURE_DRIFT_FREQUENCY)
                * 0.5f * temperatureVariance;

        float precipitationCooling = centerSample.getPrecipitationIntensity()
                * EngineSetting.TEMPERATURE_PRECIPITATION_COOLING;

        this.currentTemperature = baseTemperature + diurnalOffset + driftOffset - precipitationCooling;
    }

    private void resolveActiveSeason() {

        String currentSeasonName = clockManager.getClockHandle().getCurrentSeason();

        if (currentSeasonName == null || currentSeasonName.equals(lastSeasonName))
            return;

        lastSeasonName = currentSeasonName;
        activeSeason = seasonManager.getSeasonHandleFromSeasonName(currentSeasonName);
    }

    /*
     * Bell-shaped curve over the daily cycle, peaking at
     * TEMPERATURE_DIURNAL_PEAK_TIME. Returns roughly [-1, 1].
     */
    private float computeDiurnalOffset() {

        double visualTimeOfDay = clockManager.getClockHandle().getVisualTimeOfDay();
        double angle = (visualTimeOfDay - EngineSetting.TEMPERATURE_DIURNAL_PEAK_TIME) * Math.PI * 2.0;

        return (float) Math.cos(angle);
    }

    // Accessible \\

    float getCurrentTemperature() {
        return currentTemperature;
    }
}