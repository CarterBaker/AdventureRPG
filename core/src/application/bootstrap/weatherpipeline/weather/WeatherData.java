package application.bootstrap.weatherpipeline.weather;

import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeatherData extends DataPackage {

    /*
     * Immutable weather definition loaded from JSON — the condition-level
     * atmosphere values for one named weather, its chance-weighted cloud
     * pool, and its own list of suggested next weathers. cloudCoverage is
     * the fraction of sky/area this weather's clouds occupy; cloudDensityMultiplier
     * separately scales how thick/opaque those clouds read, independent of
     * how much area they cover. visualScale controls how large this weather
     * reads in the sky.
     */

    private final String weatherName;
    private final short weatherID;

    private final ObjectArrayList<CloudChanceStruct> cloudEntries;
    private final ObjectArrayList<NextWeatherChanceStruct> nextWeatherChances;

    private final float cloudCoverage;
    private final float cloudDensityMultiplier;
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
            ObjectArrayList<NextWeatherChanceStruct> nextWeatherChances,
            float cloudCoverage,
            float cloudDensityMultiplier,
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
        this.nextWeatherChances = nextWeatherChances;
        this.cloudCoverage = cloudCoverage;
        this.cloudDensityMultiplier = cloudDensityMultiplier;
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

    public ObjectArrayList<NextWeatherChanceStruct> getNextWeatherChances() {
        return nextWeatherChances;
    }

    public float getCloudCoverage() {
        return cloudCoverage;
    }

    public float getCloudDensityMultiplier() {
        return cloudDensityMultiplier;
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