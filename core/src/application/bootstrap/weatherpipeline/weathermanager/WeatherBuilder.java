package application.bootstrap.weatherpipeline.weathermanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.cloudmanager.CloudManager;
import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.NextWeatherChanceStruct;
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
     * "clouds" and "nextWeatherChances" are both optional. nextWeatherChances
     * are stored as bare names rather than resolved handles — resolving them
     * at build time would let two weathers that suggest each other deadlock
     * the bootstrap loader, so resolution happens by name comparison instead,
     * lazily, whenever WeatherManager consults them. Bootstrap-only and
     * on-demand.
     */

    private CloudManager cloudManager;

    @Override
    protected void get() {
        this.cloudManager = get(CloudManager.class);
    }

    WeatherHandle build(File file, File root) {

        String weatherName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short weatherID = RegistryUtility.toShortID(weatherName);

        JsonObject json = JsonUtility.loadJsonObject(file);

        ObjectArrayList<CloudChanceStruct> cloudEntries = parseClouds(json);
        ObjectArrayList<NextWeatherChanceStruct> nextWeatherChances = parseNextWeatherChances(json);
        float cloudCoverage = parseFloat(json, "cloudCoverage", 0f);
        float precipitationIntensity = parseFloat(json, "precipitationIntensity", 0f);
        float windSpeedScale = parseFloat(json, "windSpeedScale", 1f);
        float windTurbulenceScale = parseFloat(json, "windTurbulenceScale",
                EngineSetting.DEFAULT_WEATHER_WIND_TURBULENCE_SCALE);
        float fogDensityScale = parseFloat(json, "fogDensityScale", 1f);
        float humidity = parseFloat(json, "humidity", 0.5f);
        float visibility = parseFloat(json, "visibility", 1f);
        float visualScale = parseFloat(json, "visualScale", 1f);
        float temperatureModifier = parseFloat(json, "temperatureModifier", 0f);

        WeatherData weatherData = new WeatherData(
                weatherName,
                weatherID,
                cloudEntries,
                nextWeatherChances,
                cloudCoverage,
                precipitationIntensity,
                windSpeedScale,
                windTurbulenceScale,
                fogDensityScale,
                humidity,
                visibility,
                visualScale,
                temperatureModifier);

        WeatherHandle weatherHandle = create(WeatherHandle.class);
        weatherHandle.constructor(weatherData);

        return weatherHandle;
    }

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

    private ObjectArrayList<NextWeatherChanceStruct> parseNextWeatherChances(JsonObject json) {

        if (!json.has("nextWeatherChances"))
            return new ObjectArrayList<>();

        JsonArray array = json.getAsJsonArray("nextWeatherChances");
        ObjectArrayList<NextWeatherChanceStruct> entries = new ObjectArrayList<>(array.size());

        for (JsonElement element : array) {

            JsonObject entryObject = element.getAsJsonObject();
            String weatherName = JsonUtility.validateString(entryObject, "name");
            float chance = entryObject.has("chance")
                    ? entryObject.get("chance").getAsFloat()
                    : 1.0f;

            entries.add(new NextWeatherChanceStruct(weatherName, chance));
        }

        return entries;
    }

    private float parseFloat(JsonObject json, String field, float fallback) {

        if (!json.has(field))
            return fallback;

        return json.get(field).getAsFloat();
    }
}