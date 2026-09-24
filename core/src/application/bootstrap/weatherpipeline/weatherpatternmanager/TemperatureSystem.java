package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

class TemperatureSystem extends SystemPackage {

    /*
     * Computes ambient temperature from the season-blended base and
     * variance, shaped by a diurnal curve and a slow shared drift, cooled
     * by a local weather instance's precipitation and offset by its
     * temperature modifier. The season values ease across the year and the
     * drift runs on the world's own clock, so temperature carries on from
     * wherever it left off between sessions instead of restarting.
     */

    // Internal
    private ClockManager clockManager;
    private SeasonManager seasonManager;

    // Drift
    private double driftSeconds;

    // Internal \\

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
        this.seasonManager = get(SeasonManager.class);
    }

    void advanceClock() {
        this.driftSeconds = clockManager.getClockHandle().getWorldSecondsElapsed();
    }

    // Temperature \\

    float computeTemperature(WeatherInstance localPattern, double visualTimeOfDay) {

        float baseTemperature = seasonManager.getBlendedBaseTemperature();
        float temperatureVariance = seasonManager.getBlendedTemperatureVariance();

        float diurnalOffset = computeDiurnalOffset(visualTimeOfDay) * temperatureVariance;
        float driftOffset = (float) Math.sin(driftSeconds * EngineSetting.TEMPERATURE_DRIFT_FREQUENCY)
                * 0.5f * temperatureVariance;

        float precipitationIntensity = localPattern != null ? localPattern.getBlendedPrecipitationIntensity() : 0f;
        float temperatureModifier = localPattern != null ? localPattern.getBlendedTemperatureModifier() : 0f;

        float precipitationCooling = precipitationIntensity * EngineSetting.TEMPERATURE_PRECIPITATION_COOLING;

        return baseTemperature + diurnalOffset + driftOffset - precipitationCooling + temperatureModifier;
    }

    private float computeDiurnalOffset(double visualTimeOfDay) {
        double angle = (visualTimeOfDay - EngineSetting.TEMPERATURE_DIURNAL_PEAK_TIME) * Math.PI * 2.0;
        return (float) Math.cos(angle);
    }
}
