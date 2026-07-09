// WeatherBuilder.java
package application.bootstrap.weatherpipeline.weathermanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.cloudmanager.CloudManager;
import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.WeatherData;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WeatherBuilder extends BuilderPackage {

    /*
     * Parses weather JSON into a WeatherData and wraps it in a WeatherHandle.
     * The "clouds" array is optional — a weather that omits it, or declares
     * it empty, spawns no cloud objects and paints nothing extra on the sky,
     * which is exactly what a genuinely clear weather is (see
     * standard/ClearWeather.json). Any entries that are present each carry a
     * relative chance and an optional altitude override, and are resolved
     * through CloudManager, the same on-demand-capable lookup pattern used
     * for shaders, textures, and every other cross-manager reference in
     * bootstrap. humidity and visibility both fall back to neutral defaults
     * when omitted, same as every other atmosphere field here.
     * windTurbulenceScale defaults to
     * EngineSetting.DEFAULT_WEATHER_WIND_TURBULENCE_SCALE (a neutral 1.0)
     * when omitted, so existing weather JSON without the field keeps its
     * old wind behavior unchanged. temperatureModifier defaults to 0 (no
     * adjustment) when omitted — see WeatherData's own doc comment for what
     * it represents. Bootstrap-only and on-demand.
     */

    // Internal
    private CloudManager cloudManager;

    // Base \\

    @Override
    protected void get() {
        this.cloudManager = get(CloudManager.class);
    }

    // Build \\

    WeatherHandle build(File file, File root) {

        String weatherName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short weatherID = RegistryUtility.toShortID(weatherName);

        JsonObject json = JsonUtility.loadJsonObject(file);

        ObjectArrayList<CloudChanceStruct> cloudEntries = parseClouds(json);
        float cloudCoverage = parseFloat(json, "cloudCoverage", 0f);
        float precipitationIntensity = parseFloat(json, "precipitationIntensity", 0f);
        float windSpeedScale = parseFloat(json, "windSpeedScale", 1f);
        float windTurbulenceScale = parseFloat(json, "windTurbulenceScale",
                EngineSetting.DEFAULT_WEATHER_WIND_TURBULENCE_SCALE);
        float fogDensityScale = parseFloat(json, "fogDensityScale", 1f);
        float humidity = parseFloat(json, "humidity", 0.5f);
        float visibility = parseFloat(json, "visibility", 1f);
        float temperatureModifier = parseFloat(json, "temperatureModifier", 0f);

        WeatherData weatherData = new WeatherData(
                weatherName,
                weatherID,
                cloudEntries,
                cloudCoverage,
                precipitationIntensity,
                windSpeedScale,
                windTurbulenceScale,
                fogDensityScale,
                humidity,
                visibility,
                temperatureModifier);

        WeatherHandle weatherHandle = create(WeatherHandle.class);
        weatherHandle.constructor(weatherData);

        return weatherHandle;
    }

    // Parsing \\

    private ObjectArrayList<CloudChanceStruct> parseClouds(JsonObject json) {

        if (!json.has("clouds"))
            return new ObjectArrayList<>();

        JsonArray cloudsArray = json.getAsJsonArray("clouds");
        ObjectArrayList<CloudChanceStruct> entries = new ObjectArrayList<>(cloudsArray.size());

        for (JsonElement element : cloudsArray)
            entries.add(parseCloudEntry(element.getAsJsonObject()));

        return entries;
    }

    private CloudChanceStruct parseCloudEntry(JsonObject entryObject) {

        String cloudName = JsonUtility.validateString(entryObject, "name");
        CloudHandle cloudHandle = cloudManager.getCloudHandleFromCloudName(cloudName);

        float chance = entryObject.has("chance")
                ? entryObject.get("chance").getAsFloat()
                : 1.0f;

        float altitudeOverride = entryObject.has("altitudeOverride")
                ? entryObject.get("altitudeOverride").getAsFloat()
                : CloudChanceStruct.NO_ALTITUDE_OVERRIDE;

        return new CloudChanceStruct(cloudHandle, chance, altitudeOverride);
    }

    private float parseFloat(JsonObject json, String field, float fallback) {

        if (!json.has(field))
            return fallback;

        return json.get(field).getAsFloat();
    }
}