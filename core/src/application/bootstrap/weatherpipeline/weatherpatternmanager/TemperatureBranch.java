package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.season.SeasonHandle;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class TemperatureBranch extends BranchPackage {

    /*
     * Computes live ambient temperature from the active season's base
     * temperature and variance, shaped by a diurnal curve and a slow drift,
     * cooled by the current local weather pattern's precipitation intensity
     * and offset by its temperature modifier — both blended across any
     * in-progress transition. Driven each frame by WeatherPatternManager
     * once its local pattern has been resolved.
     */

    private ClockManager clockManager;
    private SeasonManager seasonManager;

    private String lastSeasonName;
    private SeasonHandle activeSeason;

    private float elapsedTime;
    private float currentTemperature;

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
        this.seasonManager = get(SeasonManager.class);
    }

    // Temperature \\

    void updateTemperature(WeatherPatternStruct localPattern) {

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

        float precipitationIntensity = localPattern != null ? localPattern.getBlendedPrecipitationIntensity() : 0f;
        float temperatureModifier = localPattern != null ? localPattern.getBlendedTemperatureModifier() : 0f;

        float precipitationCooling = precipitationIntensity * EngineSetting.TEMPERATURE_PRECIPITATION_COOLING;

        this.currentTemperature = baseTemperature + diurnalOffset + driftOffset - precipitationCooling
                + temperatureModifier;
    }

    private void resolveActiveSeason() {

        String currentSeasonName = clockManager.getClockHandle().getCurrentSeason();

        if (currentSeasonName == null || currentSeasonName.equals(lastSeasonName))
            return;

        lastSeasonName = currentSeasonName;
        activeSeason = seasonManager.getSeasonHandleFromSeasonName(currentSeasonName);
    }

    private float computeDiurnalOffset() {

        double visualTimeOfDay = clockManager.getPrimaryLocationTime().getVisualTimeOfDay();
        double angle = (visualTimeOfDay - EngineSetting.TEMPERATURE_DIURNAL_PEAK_TIME) * Math.PI * 2.0;

        return (float) Math.cos(angle);
    }

    // Accessible \\

    float getCurrentTemperature() {
        return currentTemperature;
    }
}