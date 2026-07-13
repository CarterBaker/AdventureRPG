package application.bootstrap.weatherpipeline.weather;

import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeatherData extends DataPackage {

    /*
     * Immutable weather definition loaded from JSON — the condition-level
     * atmosphere values for one named weather, plus its chance-weighted
     * cloud pool. visualScale controls how large this weather reads in the
     * sky independently of cloudCoverage, which controls how dense/opaque
     * it reads.
     */

    private final String weatherName;
    private final short weatherID;

    private final ObjectArrayList<CloudChanceStruct> cloudEntries;

    private final float cloudCoverage;
    private final float precipitationIntensity;
    private final float windSpeedScale;
    private final float windTurbulenceScale;
    private final float fogDensityScale;
    private final float humidity;
    private final float visibility;
    private final float visualScale;

    private final float temperatureModifier;

    public WeatherData(
            String weatherName,
            short weatherID,
            ObjectArrayList<CloudChanceStruct> cloudEntries,
            float cloudCoverage,
            float precipitationIntensity,
            float windSpeedScale,
            float windTurbulenceScale,
            float fogDensityScale,
            float humidity,
            float visibility,
            float visualScale,
            float temperatureModifier) {

        this.weatherName = weatherName;
        this.weatherID = weatherID;
        this.cloudEntries = cloudEntries;
        this.cloudCoverage = cloudCoverage;
        this.precipitationIntensity = precipitationIntensity;
        this.windSpeedScale = windSpeedScale;
        this.windTurbulenceScale = windTurbulenceScale;
        this.fogDensityScale = fogDensityScale;
        this.humidity = humidity;
        this.visibility = visibility;
        this.visualScale = visualScale;
        this.temperatureModifier = temperatureModifier;
    }

    public String getWeatherName() {
        return weatherName;
    }

    public short getWeatherID() {
        return weatherID;
    }

    public ObjectArrayList<CloudChanceStruct> getCloudEntries() {
        return cloudEntries;
    }

    public float getCloudCoverage() {
        return cloudCoverage;
    }

    public float getPrecipitationIntensity() {
        return precipitationIntensity;
    }

    public float getWindSpeedScale() {
        return windSpeedScale;
    }

    public float getWindTurbulenceScale() {
        return windTurbulenceScale;
    }

    public float getFogDensityScale() {
        return fogDensityScale;
    }

    public float getHumidity() {
        return humidity;
    }

    public float getVisibility() {
        return visibility;
    }

    public float getVisualScale() {
        return visualScale;
    }

    public float getTemperatureModifier() {
        return temperatureModifier;
    }
}