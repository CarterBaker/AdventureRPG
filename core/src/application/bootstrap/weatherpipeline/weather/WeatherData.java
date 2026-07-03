package application.bootstrap.weatherpipeline.weather;

import engine.root.DataPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeatherData extends DataPackage {

    /*
     * Immutable weather definition loaded from JSON. Holds the condition-level
     * shader-facing values for one named weather plus the chance-weighted set
     * of clouds that may appear under this condition — see CloudChanceStruct.
     * Cloud shape, color, and default altitude live entirely on the
     * referenced CloudHandle; this class only ever carries per-condition
     * numbers. Owned by WeatherHandle for the engine lifetime.
     */

    // Internal
    private final String weatherName;
    private final short weatherID;

    // Clouds
    private final ObjectArrayList<CloudChanceStruct> cloudEntries;

    // Atmosphere
    private final float cloudCoverage;
    private final float precipitationIntensity;
    private final float windSpeedScale;
    private final float fogDensityScale;

    // Constructor \\

    public WeatherData(
            String weatherName,
            short weatherID,
            ObjectArrayList<CloudChanceStruct> cloudEntries,
            float cloudCoverage,
            float precipitationIntensity,
            float windSpeedScale,
            float fogDensityScale) {

        // Internal
        this.weatherName = weatherName;
        this.weatherID = weatherID;

        // Clouds
        this.cloudEntries = cloudEntries;

        // Atmosphere
        this.cloudCoverage = cloudCoverage;
        this.precipitationIntensity = precipitationIntensity;
        this.windSpeedScale = windSpeedScale;
        this.fogDensityScale = fogDensityScale;
    }

    // Accessible \\

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

    public float getFogDensityScale() {
        return fogDensityScale;
    }
}