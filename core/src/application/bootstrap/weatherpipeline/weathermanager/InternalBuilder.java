package application.bootstrap.weatherpipeline.weathermanager;

import java.io.File;

import com.google.gson.JsonObject;

import application.bootstrap.weatherpipeline.weather.CloudType;
import application.bootstrap.weatherpipeline.weather.WeatherData;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.BuilderPackage;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses weather JSON into a WeatherData and wraps it in a WeatherHandle.
     * Bootstrap-only and on-demand.
     */

    // Build \\

    WeatherHandle build(File file, File root) {

        String weatherName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short weatherID = RegistryUtility.toShortID(weatherName);

        JsonObject json = JsonUtility.loadJsonObject(file);

        CloudType cloudType = parseCloudType(json);
        float cloudCoverage = parseFloat(json, "cloudCoverage", 0f);
        Vector3 cloudColor = parseColor(json, "cloudColor");
        float precipitationIntensity = parseFloat(json, "precipitationIntensity", 0f);
        float windSpeedScale = parseFloat(json, "windSpeedScale", 1f);
        float fogDensityScale = parseFloat(json, "fogDensityScale", 1f);
        float chance = parseFloat(json, "chance", 1f);

        WeatherData weatherData = new WeatherData(
                weatherName,
                weatherID,
                cloudType,
                cloudCoverage,
                cloudColor,
                precipitationIntensity,
                windSpeedScale,
                fogDensityScale,
                chance);

        WeatherHandle weatherHandle = create(WeatherHandle.class);
        weatherHandle.constructor(weatherData);

        return weatherHandle;
    }

    // Parsing \\

    private CloudType parseCloudType(JsonObject json) {

        if (!json.has("cloudType"))
            throwException("Weather JSON missing 'cloudType' field");

        return CloudType.valueOf(json.get("cloudType").getAsString());
    }

    private float parseFloat(JsonObject json, String field, float fallback) {

        if (!json.has(field))
            return fallback;

        return json.get(field).getAsFloat();
    }

    private Vector3 parseColor(JsonObject json, String field) {

        if (!json.has(field))
            return new Vector3(1f, 1f, 1f);

        JsonObject colorObject = json.getAsJsonObject(field);

        float r = colorObject.get("r").getAsFloat();
        float g = colorObject.get("g").getAsFloat();
        float b = colorObject.get("b").getAsFloat();

        return new Vector3(r, g, b);
    }
}