package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

class TemperatureSystem extends SystemPackage {

    /*
     * Computes ambient temperature from the season-blended base and variance, a
     * diurnal curve, a slow shared drift and a seeded daily swing, cooled by
     * precipitation and offset by local weather, then scaled by the calendar's
     * star. Driven by the world clock, so it carries on between sessions.
     */

    // Internal
    private ClockManager clockManager;
    private SeasonManager seasonManager;

    // Drift
    private double driftSeconds;

    // Daily
    private float dailySwing;

    // Star
    private float starTemperatureScale;

    // Internal \\

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
        this.seasonManager = get(SeasonManager.class);
    }

    void advanceClock() {

        this.driftSeconds = clockManager.getClockHandle().getWorldSecondsElapsed();
        this.dailySwing = clockManager.getDailyRandom(EngineSetting.TEMPERATURE_DAILY_STREAM);
        this.starTemperatureScale = clockManager.getCalendarHandle().getStarTemperatureScale();
    }

    // Temperature \\

    float computeTemperature(WeatherInstance localPattern, double visualTimeOfDay) {

        float baseTemperature = seasonManager.getBlendedBaseTemperature();
        float temperatureVariance = seasonManager.getBlendedTemperatureVariance();

        float diurnalOffset = computeDiurnalOffset(visualTimeOfDay) * temperatureVariance;
        float driftOffset = (float) Math.sin(driftSeconds * EngineSetting.TEMPERATURE_DRIFT_FREQUENCY)
                * 0.5f * temperatureVariance;
        float dailyOffset = dailySwing * EngineSetting.TEMPERATURE_DAILY_VARIANCE_SCALE * temperatureVariance;

        float precipitationIntensity = localPattern != null ? localPattern.getBlendedPrecipitationIntensity() : 0f;
        float temperatureModifier = localPattern != null ? localPattern.getBlendedTemperatureModifier() : 0f;

        float precipitationCooling = precipitationIntensity * EngineSetting.TEMPERATURE_PRECIPITATION_COOLING;

        float temperature = baseTemperature + diurnalOffset + driftOffset + dailyOffset
                - precipitationCooling + temperatureModifier;

        return applyStar(temperature);
    }

    private float computeDiurnalOffset(double visualTimeOfDay) {
        double angle = (visualTimeOfDay - EngineSetting.TEMPERATURE_DIURNAL_PEAK_TIME) * Math.PI * 2.0;
        return (float) Math.cos(angle);
    }

    // Star \\

    private float applyStar(float temperature) {
        float kelvin = temperature + EngineSetting.TEMPERATURE_KELVIN_OFFSET;
        return kelvin * starTemperatureScale - EngineSetting.TEMPERATURE_KELVIN_OFFSET;
    }
}
