package application.bootstrap.weatherpipeline.weather;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import engine.root.DataPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeatherData extends DataPackage {

    /*
     * Immutable weather definition loaded from JSON — the condition-level
     * atmosphere values for one named weather, its chance-weighted cloud
     * pool, and its own suggested next weathers. Cloud entries and next-
     * weather suggestions are parallel fastutil lists rather than a
     * wrapper type per entry. cloudCoverage is the fraction of sky/area
     * this weather's clouds occupy; cloudDensityMultiplier separately
     * scales how thick/opaque those clouds read, independent of how much
     * area they cover. visualScale controls how large this weather reads
     * in the sky.
     *
     * The "no altitude override" sentinel and the default per-cloud-entry
     * density multiplier both live on EngineSetting now (WEATHER_CLOUD_
     * NO_ALTITUDE_OVERRIDE / DEFAULT_CLOUD_ENTRY_DENSITY_MULTIPLIER) —
     * they're authoring/parsing constants, not data this class owns, so
     * they don't belong here.
     */

    private final String weatherName;
    private final short weatherID;

    private final ObjectArrayList<CloudHandle> cloudHandles;
    private final FloatArrayList cloudChances;
    private final FloatArrayList cloudAltitudeOverrides;
    private final FloatArrayList cloudDensityMultipliers;

    private final ObjectArrayList<String> nextWeatherNames;
    private final FloatArrayList nextWeatherChances;

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
            ObjectArrayList<CloudHandle> cloudHandles,
            FloatArrayList cloudChances,
            FloatArrayList cloudAltitudeOverrides,
            FloatArrayList cloudDensityMultipliers,
            ObjectArrayList<String> nextWeatherNames,
            FloatArrayList nextWeatherChances,
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
        this.cloudHandles = cloudHandles;
        this.cloudChances = cloudChances;
        this.cloudAltitudeOverrides = cloudAltitudeOverrides;
        this.cloudDensityMultipliers = cloudDensityMultipliers;
        this.nextWeatherNames = nextWeatherNames;
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

    public ObjectArrayList<CloudHandle> getCloudHandles() {
        return cloudHandles;
    }

    public FloatArrayList getCloudChances() {
        return cloudChances;
    }

    public FloatArrayList getCloudAltitudeOverrides() {
        return cloudAltitudeOverrides;
    }

    public FloatArrayList getCloudDensityMultipliers() {
        return cloudDensityMultipliers;
    }

    public ObjectArrayList<String> getNextWeatherNames() {
        return nextWeatherNames;
    }

    public FloatArrayList getNextWeatherChances() {
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