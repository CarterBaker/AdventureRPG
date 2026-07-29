package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.season.SeasonHandle;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weatherpattern.WeatherPatternInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

class TemperatureSystem extends SystemPackage {

    /*
     * Computes ambient temperature from the active season's base and
     * variance, shaped by a diurnal curve and a slow shared drift, cooled
     * by a local pattern's precipitation and offset by its temperature
     * modifier.
     */

    private ClockManager clockManager;
    private SeasonManager seasonManager;

    private String lastSeasonName;
    private SeasonHandle activeSeason;

    private float elapsedTime;

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
        this.seasonManager = get(SeasonManager.class);
    }

    void advanceClock() {
        elapsedTime += internal.getDeltaTime();
        resolveActiveSeason();
    }

    float computeTemperature(WeatherPatternInstance localPattern, double visualTimeOfDay) {

        float baseTemperature = EngineSetting.DEFAULT_BASE_TEMPERATURE;
        float temperatureVariance = 0f;

        if (activeSeason != null) {
            baseTemperature = activeSeason.getBaseTemperature();
            temperatureVariance = activeSeason.getTemperatureVariance();
        }

        float diurnalOffset = computeDiurnalOffset(visualTimeOfDay) * temperatureVariance;
        float driftOffset = (float) Math.sin(elapsedTime * EngineSetting.TEMPERATURE_DRIFT_FREQUENCY)
                * 0.5f * temperatureVariance;

        float precipitationIntensity = localPattern != null ? localPattern.getBlendedPrecipitationIntensity() : 0f;
        float temperatureModifier = localPattern != null ? localPattern.getBlendedTemperatureModifier() : 0f;

        float precipitationCooling = precipitationIntensity * EngineSetting.TEMPERATURE_PRECIPITATION_COOLING;

        return baseTemperature + diurnalOffset + driftOffset - precipitationCooling + temperatureModifier;
    }

    private void resolveActiveSeason() {

        String currentSeasonName = clockManager.getClockHandle().getCurrentSeason();

        if (currentSeasonName == null || currentSeasonName.equals(lastSeasonName))
            return;

        lastSeasonName = currentSeasonName;
        activeSeason = seasonManager.getSeasonHandleFromSeasonName(currentSeasonName);
    }

    private float computeDiurnalOffset(double visualTimeOfDay) {
        double angle = (visualTimeOfDay - EngineSetting.TEMPERATURE_DIURNAL_PEAK_TIME) * Math.PI * 2.0;
        return (float) Math.cos(angle);
    }
}