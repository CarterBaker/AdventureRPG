package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.season.SeasonHandle;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class TemperatureBranch extends BranchPackage {

    /*
     * Computes live ambient temperature from the active season's base
     * temperature and variance, shaped by a diurnal curve and a slow
     * drift, cooled by precipitation intensity, and offset by the
     * currently resolved weather's own temperatureModifier. CPU-side only
     * — nothing on the GPU reads temperature yet. Driven explicitly by
     * WeatherManager.update() with that frame's freshly sampled center
     * weather sample, rather than its own update() cascade.
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

        this.currentTemperature = baseTemperature + diurnalOffset + driftOffset - precipitationCooling
                + centerSample.getTemperatureModifier();
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