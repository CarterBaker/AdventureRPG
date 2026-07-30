package application.bootstrap.weatherpipeline.weathermanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.cloudmanager.CloudManager;
import application.bootstrap.weatherpipeline.weather.WeatherData;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WeatherBuilder extends BuilderPackage {

    /*
     * Parses weather JSON into a WeatherData and wraps it in a WeatherHandle.
     * "clouds" and "nextWeatherChances" are both optional and stored as
     * parallel fastutil lists rather than a wrapper type per entry.
     * nextWeatherNames are stored as bare names rather than resolved
     * handles — resolving them at build time would let two weathers that
     * suggest each other deadlock the bootstrap loader, so resolution
     * happens by name comparison instead, lazily, whenever WeatherManager
     * consults them. Bootstrap-only and on-demand.
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

        ObjectArrayList<CloudHandle> cloudHandles = new ObjectArrayList<>();
        FloatArrayList cloudChances = new FloatArrayList();
        FloatArrayList cloudAltitudeOverrides = new FloatArrayList();
        FloatArrayList cloudDensityMultipliers = new FloatArrayList();
        parseClouds(json, cloudHandles, cloudChances, cloudAltitudeOverrides, cloudDensityMultipliers);

        ObjectArrayList<String> nextWeatherNames = new ObjectArrayList<>();
        FloatArrayList nextWeatherChances = new FloatArrayList();
        parseNextWeatherChances(json, nextWeatherNames, nextWeatherChances);

        float cloudCoverage = parseFloat(json, "cloudCoverage", 0f);
        float cloudDensityMultiplier = parseFloat(json, "cloudDensityMultiplier", 1f);
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
                cloudHandles,
                cloudChances,
                cloudAltitudeOverrides,
                cloudDensityMultipliers,
                nextWeatherNames,
                nextWeatherChances,
                cloudCoverage,
                cloudDensityMultiplier,
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

    private void parseClouds(
            JsonObject json,
            ObjectArrayList<CloudHandle> outHandles,
            FloatArrayList outChances,
            FloatArrayList outAltitudeOverrides,
            FloatArrayList outDensityMultipliers) {

        if (!json.has("clouds"))
            return;

        JsonArray cloudsArray = json.getAsJsonArray("clouds");

        for (JsonElement element : cloudsArray)
            parseCloudEntry(
                    element.getAsJsonObject(), outHandles, outChances, outAltitudeOverrides, outDensityMultipliers);
    }

    private void parseCloudEntry(
            JsonObject entryObject,
            ObjectArrayList<CloudHandle> outHandles,
            FloatArrayList outChances,
            FloatArrayList outAltitudeOverrides,
            FloatArrayList outDensityMultipliers) {

        String cloudName = JsonUtility.validateString(entryObject, "name");
        CloudHandle cloudHandle = cloudManager.getCloudHandleFromCloudName(cloudName);

        float chance = entryObject.has("chance")
                ? entryObject.get("chance").getAsFloat()
                : 1.0f;

        float altitudeOverride = entryObject.has("altitudeOverride")
                ? entryObject.get("altitudeOverride").getAsFloat()
                : WeatherData.NO_ALTITUDE_OVERRIDE;

        float densityMultiplier = entryObject.has("densityMultiplier")
                ? entryObject.get("densityMultiplier").getAsFloat()
                : WeatherData.DEFAULT_CLOUD_DENSITY_MULTIPLIER;

        if (densityMultiplier < 0f)
            throwException("Cloud entry \"" + cloudName + "\" has a negative densityMultiplier: " + densityMultiplier);

        outHandles.add(cloudHandle);
        outChances.add(chance);
        outAltitudeOverrides.add(altitudeOverride);
        outDensityMultipliers.add(densityMultiplier);
    }

    private void parseNextWeatherChances(
            JsonObject json,
            ObjectArrayList<String> outNames,
            FloatArrayList outChances) {

        if (!json.has("nextWeatherChances"))
            return;

        JsonArray array = json.getAsJsonArray("nextWeatherChances");

        for (JsonElement element : array) {

            JsonObject entryObject = element.getAsJsonObject();
            String weatherName = JsonUtility.validateString(entryObject, "name");
            float chance = entryObject.has("chance")
                    ? entryObject.get("chance").getAsFloat()
                    : 1.0f;

            outNames.add(weatherName);
            outChances.add(chance);
        }
    }

    private float parseFloat(JsonObject json, String field, float fallback) {

        if (!json.has(field))
            return fallback;

        return json.get(field).getAsFloat();
    }
}