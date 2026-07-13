package application.bootstrap.weatherpipeline.weather;

import engine.root.HandlePackage;
import engine.util.random.WeightedChanceUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeatherHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded weather definition, owned by
     * WeatherManager. A weather may define no clouds at all (clear skies),
     * in which case hasClouds() is false and getPrimaryCloud()/pickCloud()
     * return null rather than throwing.
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

    public ObjectArrayList<CloudChanceStruct> getCloudEntries() {
        return weatherData.getCloudEntries();
    }

    public boolean hasClouds() {
        return !weatherData.getCloudEntries().isEmpty();
    }

    public CloudChanceStruct getPrimaryCloud() {

        ObjectArrayList<CloudChanceStruct> entries = weatherData.getCloudEntries();

        if (entries.isEmpty())
            return null;

        CloudChanceStruct best = entries.get(0);

        for (int i = 1; i < entries.size(); i++)
            if (entries.get(i).getChance() > best.getChance())
                best = entries.get(i);

        return best;
    }

    public CloudChanceStruct pickCloud(float noise01) {

        ObjectArrayList<CloudChanceStruct> entries = weatherData.getCloudEntries();

        if (entries.isEmpty())
            return null;

        return WeightedChanceUtility.pickWeighted(entries, noise01);
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

    public float getVisualScale() {
        return weatherData.getVisualScale();
    }

    public float getTemperatureModifier() {
        return weatherData.getTemperatureModifier();
    }
}