package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

class TemperatureSystem extends SystemPackage {

    /*
     * Computes ambient temperature from the season-blended base and
     * variance, shaped by a diurnal curve, a slow shared drift, and a daily
     * swing drawn from the day's own seed so some days run cold and others
     * warm, then cooled by local precipitation and offset by the local
     * weather's modifier. The result is scaled in absolute terms by the
     * star the calendar's world orbits, so a closer or brighter star runs
     * the whole climate hotter. The season values ease across the year and
     * the drift runs on the world's own clock, so temperature carries on
     * from wherever it left off between sessions instead of restarting.
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
