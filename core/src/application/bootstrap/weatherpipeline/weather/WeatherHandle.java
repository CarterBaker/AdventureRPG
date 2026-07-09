package application.bootstrap.weatherpipeline.weather;

import engine.root.HandlePackage;
import engine.util.random.WeightedChanceUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeatherHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded weather definition. Registered and
     * owned by WeatherManager. Delegates all accessors through WeatherData.
     * Clouds are exposed as the full chance-weighted list — callers that
     * need genuine per-instance variety (e.g. each overhead cell) should
     * pick from getCloudEntries() themselves via WeightedChanceUtility;
     * getPrimaryCloud() is a deterministic convenience for single-value
     * uses like horizon tinting, always returning the highest-chance entry.
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

    public ObjectArrayList<CloudChanceStruct> getCloudEntries() {
        return weatherData.getCloudEntries();
    }

    public CloudChanceStruct getPrimaryCloud() {

        ObjectArrayList<CloudChanceStruct> entries = weatherData.getCloudEntries();

        if (entries.isEmpty())
            throwException("Weather \"" + getWeatherName() + "\" has no clouds defined");

        CloudChanceStruct best = entries.get(0);

        for (int i = 1; i < entries.size(); i++)
            if (entries.get(i).getChance() > best.getChance())
                best = entries.get(i);

        return best;
    }

    public CloudChanceStruct pickCloud(float noise01) {
        return WeightedChanceUtility.pickWeighted(weatherData.getCloudEntries(), noise01);
    }

    public float getCloudCoverage() {
        return weatherData.getCloudCoverage();
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

    public float getHumidity() {
        return weatherData.getHumidity();
    }

    public float getVisibility() {
        return weatherData.getVisibility();
    }
}