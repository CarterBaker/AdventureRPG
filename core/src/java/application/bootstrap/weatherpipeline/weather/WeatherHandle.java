package application.bootstrap.weatherpipeline.weather;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import engine.root.HandlePackage;

public class WeatherHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded weather definition, owned by
     * WeatherManager. A weather may define no clouds at all (clear skies), in
     * which case getCloudCount() is 0. Cloud entries are read by index against
     * the parallel lists on WeatherData. Severity orders a biome's pool so the
     * weather image grades from calm to stormy.
     */

    private WeatherData weatherData;

    public void constructor(WeatherData weatherData) {
        this.weatherData = weatherData;
    }

    public WeatherData getWeatherData() {
        return weatherData;
    }

    public String getWeatherName() {
        return weatherData.getWeatherName();
    }

    public short getWeatherID() {
        return weatherData.getWeatherID();
    }

    // Clouds \\

    public int getCloudCount() {
        return weatherData.getCloudHandles().size();
    }

    public boolean hasClouds() {
        return !weatherData.getCloudHandles().isEmpty();
    }

    public CloudHandle getCloudHandle(int index) {
        return weatherData.getCloudHandles().get(index);
    }

    public float getCloudCoverage(int index) {
        return weatherData.getCloudCoverages().getFloat(index);
    }

    public float getCloudDensityScale(int index) {
        return weatherData.getCloudDensityScales().getFloat(index);
    }

    // Atmosphere \\

    public float getCloudCoverage() {
        return weatherData.getCloudCoverage();
    }

    public float getCloudDensityMultiplier() {
        return weatherData.getCloudDensityMultiplier();
    }

    public float getPrecipitationIntensity() {
        return weatherData.getPrecipitationIntensity();
    }

    public float getWindSpeedScale() {
        return weatherData.getWindSpeedScale();
    }

    public float getWindTurbulenceScale() {
        return weatherData.getWindTurbulenceScale();
    }

    public float getFogDensityScale() {
        return weatherData.getFogDensityScale();
    }

    public float getHumidity() {
        return weatherData.getHumidity();
    }

    public float getVisibility() {
        return weatherData.getVisibility();
    }

    public float getTemperatureModifier() {
        return weatherData.getTemperatureModifier();
    }

    // Severity \\

    public float getSeverity() {
        return weatherData.getCloudCoverage() + weatherData.getPrecipitationIntensity();
    }
}
