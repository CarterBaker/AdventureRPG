// WeatherData.java
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
     * numbers. humidity and visibility are read by the weather simulation
     * (biome/season/global-noise resolution) and by fog/haze rendering —
     * visibility is a multiplier where 1.0 is clear air and lower values
     * are hazier, distinct from fogDensityScale which tunes the distance
     * fog curve itself. windSpeedScale and windTurbulenceScale both feed
     * LocalWindBranch: windSpeedScale is a flat multiplier on the season's
     * base wind speed, while windTurbulenceScale scales the AMPLITUDE of
     * the gust oscillation and direction wobble underneath it — a storm
     * should feel chaotic and erratic, not just uniformly stronger, so the
     * two are kept as separate knobs rather than folded into one. Owned by
     * WeatherHandle for the engine lifetime.
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
    private final float windTurbulenceScale;
    private final float fogDensityScale;
    private final float humidity;
    private final float visibility;

    // Constructor \\

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
            float visibility) {

        // Internal
        this.weatherName = weatherName;
        this.weatherID = weatherID;

        // Clouds
        this.cloudEntries = cloudEntries;

        // Atmosphere
        this.cloudCoverage = cloudCoverage;
        this.precipitationIntensity = precipitationIntensity;
        this.windSpeedScale = windSpeedScale;
        this.windTurbulenceScale = windTurbulenceScale;
        this.fogDensityScale = fogDensityScale;
        this.humidity = humidity;
        this.visibility = visibility;
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
}