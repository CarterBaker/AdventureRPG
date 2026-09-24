package application.bootstrap.weatherpipeline.weather;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import engine.root.DataPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeatherData extends DataPackage {

    /*
     * Immutable weather definition loaded from JSON — the condition-level
     * atmosphere values for one named weather and the cloud archetypes it
     * fills the sky with. Cloud entries are parallel fastutil lists: each
     * entry's resolved coverage (the weather's cloudCoverage shared out by
     * the entry's chance) and density scale (the entry's densityMultiplier
     * times the weather's cloudDensityMultiplier), exactly as the weather map
     * consumes them.
     */

    // Identity
    private final String weatherName;
    private final short weatherID;

    // Clouds
    private final ObjectArrayList<CloudHandle> cloudHandles;
    private final FloatArrayList cloudCoverages;
    private final FloatArrayList cloudDensityScales;

    // Atmosphere
    private final float cloudCoverage;
    private final float cloudDensityMultiplier;
    private final float precipitationIntensity;
    private final float windSpeedScale;
    private final float windTurbulenceScale;
    private final float fogDensityScale;
    private final float humidity;
    private final float visibility;
    private final float temperatureModifier;

    // Constructor \\

    public WeatherData(
            String weatherName,
            short weatherID,
            ObjectArrayList<CloudHandle> cloudHandles,
            FloatArrayList cloudCoverages,
            FloatArrayList cloudDensityScales,
            float cloudCoverage,
            float cloudDensityMultiplier,
            float precipitationIntensity,
            float windSpeedScale,
            float windTurbulenceScale,
            float fogDensityScale,
            float humidity,
            float visibility,
            float temperatureModifier) {

        this.weatherName = weatherName;
        this.weatherID = weatherID;
        this.cloudHandles = cloudHandles;
        this.cloudCoverages = cloudCoverages;
        this.cloudDensityScales = cloudDensityScales;
        this.cloudCoverage = cloudCoverage;
        this.cloudDensityMultiplier = cloudDensityMultiplier;
        this.precipitationIntensity = precipitationIntensity;
        this.windSpeedScale = windSpeedScale;
        this.windTurbulenceScale = windTurbulenceScale;
        this.fogDensityScale = fogDensityScale;
        this.humidity = humidity;
        this.visibility = visibility;
        this.temperatureModifier = temperatureModifier;
    }

    // Accessible \\

    public String getWeatherName() {
        return weatherName;
    }

    public short getWeatherID() {
        return weatherID;
    }

    public ObjectArrayList<CloudHandle> getCloudHandles() {
        return cloudHandles;
    }

    public FloatArrayList getCloudCoverages() {
        return cloudCoverages;
    }

    public FloatArrayList getCloudDensityScales() {
        return cloudDensityScales;
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

    public float getTemperatureModifier() {
        return temperatureModifier;
    }
}
