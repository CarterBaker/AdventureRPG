package application.bootstrap.weatherpipeline.weather;

import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class WeatherHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded weather definition. Registered and
     * owned by WeatherManager. Delegates all accessors through WeatherData.
     */

    // Internal
    private WeatherData weatherData;

    // Constructor \\

    public void constructor(WeatherData weatherData) {
        this.weatherData = weatherData;
    }

    // Accessible \\

    public WeatherData getWeatherData() {
        return weatherData;
    }

    public String getWeatherName() {
        return weatherData.getWeatherName();
    }

    public short getWeatherID() {
        return weatherData.getWeatherID();
    }

    public CloudType getCloudType() {
        return weatherData.getCloudType();
    }

    public float getCloudCoverage() {
        return weatherData.getCloudCoverage();
    }

    public Vector3 getCloudColor() {
        return weatherData.getCloudColor();
    }

    public float getPrecipitationIntensity() {
        return weatherData.getPrecipitationIntensity();
    }

    public float getWindSpeedScale() {
        return weatherData.getWindSpeedScale();
    }

    public float getFogDensityScale() {
        return weatherData.getFogDensityScale();
    }

    public float getChance() {
        return weatherData.getChance();
    }
}