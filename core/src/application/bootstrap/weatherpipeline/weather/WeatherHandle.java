package application.bootstrap.weatherpipeline.weather;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeatherHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded weather definition, owned by
     * WeatherManager. A weather may define no clouds at all (clear
     * skies), in which case getCloudCount() is 0. Cloud entries and
     * next-weather suggestions are read by index against parallel
     * fastutil lists on WeatherData rather than a wrapper type per entry.
     * nextWeatherChances are consulted only as a bias on top of the
     * biome/season pool's own noise-driven pick — see
     * WeatherManager.resolveWeatherBandTowardHorizonBiased().
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

    public float getCloudChance(int index) {
        return weatherData.getCloudChances().getFloat(index);
    }

    public float getCloudDensityMultiplier(int index) {
        return weatherData.getCloudDensityMultipliers().getFloat(index);
    }

    public float getCloudEffectiveAltitude(int index) {
        float override = weatherData.getCloudAltitudeOverrides().getFloat(index);
        return override >= 0f ? override : getCloudHandle(index).getBaseAltitude();
    }

    public int getPrimaryCloudIndex() {

        FloatArrayList chances = weatherData.getCloudChances();

        if (chances.isEmpty())
            return -1;

        int best = 0;

        for (int i = 1; i < chances.size(); i++)
            if (chances.getFloat(i) > chances.getFloat(best))
                best = i;

        return best;
    }

    public int pickCloudIndex(float noise01) {

        FloatArrayList chances = weatherData.getCloudChances();

        if (chances.isEmpty())
            return -1;

        float total = 0f;

        for (int i = 0; i < chances.size(); i++)
            total += Math.max(0f, chances.getFloat(i));

        if (total <= 0f)
            return 0;

        float target = Math.max(0f, Math.min(1f, noise01)) * total;
        float cumulative = 0f;

        for (int i = 0; i < chances.size(); i++) {
            cumulative += Math.max(0f, chances.getFloat(i));
            if (target <= cumulative)
                return i;
        }

        return chances.size() - 1;
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

    public float getVisualScale() {
        return weatherData.getVisualScale();
    }

    public float getTemperatureModifier() {
        return weatherData.getTemperatureModifier();
    }

    // Next Weather Suggestions \\

    public boolean hasNextWeatherSuggestions() {
        return !weatherData.getNextWeatherNames().isEmpty();
    }

    public float getNextWeatherChanceFor(WeatherHandle candidate) {

        ObjectArrayList<String> names = weatherData.getNextWeatherNames();
        FloatArrayList chances = weatherData.getNextWeatherChances();

        for (int i = 0; i < names.size(); i++)
            if (names.get(i).equals(candidate.getWeatherName()))
                return chances.getFloat(i);

        return 0f;
    }
}