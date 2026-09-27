package application.bootstrap.weatherpipeline.weathermanager;

import java.io.File;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.cloudmanager.CloudManager;
import application.bootstrap.weatherpipeline.weather.WeatherData;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class WeatherBuilder extends BuilderPackage {

    /*
     * Parses weather ARPG into a WeatherData and wraps it in a WeatherHandle.
     * "clouds" is optional. Each entry's chance shares the weather's
     * cloudCoverage out between its archetypes — the most likely entry
     * covers the full cloudCoverage and the rest scale down with their
     * chance — and each entry's densityMultiplier is folded into the
     * weather's cloudDensityMultiplier, so WeatherData stores exactly the
     * per-archetype coverage and density the weather map writes. Every
     * fallback for an omitted field comes from EngineSetting.
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

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        float cloudCoverage = parseFloat(arpg, "cloudCoverage", EngineSetting.DEFAULT_WEATHER_CLOUD_COVERAGE);
        float cloudDensityMultiplier = parseFloat(
                arpg, "cloudDensityMultiplier", EngineSetting.DEFAULT_WEATHER_CLOUD_DENSITY_MULTIPLIER);
        float precipitationIntensity = parseFloat(
                arpg, "precipitationIntensity", EngineSetting.DEFAULT_WEATHER_PRECIPITATION_INTENSITY);
        float windSpeedScale = parseFloat(arpg, "windSpeedScale", EngineSetting.DEFAULT_WEATHER_WIND_SPEED_SCALE);
        float windTurbulenceScale = parseFloat(
                arpg, "windTurbulenceScale", EngineSetting.DEFAULT_WEATHER_WIND_TURBULENCE_SCALE);
        float fogDensityScale = parseFloat(arpg, "fogDensityScale", EngineSetting.DEFAULT_WEATHER_FOG_DENSITY_SCALE);
        float humidity = parseFloat(arpg, "humidity", EngineSetting.DEFAULT_WEATHER_HUMIDITY);
        float visibility = parseFloat(arpg, "visibility", EngineSetting.DEFAULT_WEATHER_VISIBILITY);
        float temperatureModifier = parseFloat(
                arpg, "temperatureModifier", EngineSetting.DEFAULT_WEATHER_TEMPERATURE_MODIFIER);

        if (cloudCoverage < 0f || cloudCoverage > 1f)
            throwException("Weather \"" + weatherName + "\" cloudCoverage must be between 0.0 and 1.0, got: "
                    + cloudCoverage);

        ObjectArrayList<CloudHandle> cloudHandles = new ObjectArrayList<>();
        FloatArrayList cloudChances = new FloatArrayList();
        FloatArrayList cloudDensityScales = new FloatArrayList();
        parseClouds(arpg, weatherName, cloudDensityMultiplier, cloudHandles, cloudChances, cloudDensityScales);

        FloatArrayList cloudCoverages = resolveCloudCoverages(cloudChances, cloudCoverage);

        WeatherData weatherData = new WeatherData(
                weatherName,
                weatherID,
                cloudHandles,
                cloudCoverages,
                cloudDensityScales,
                cloudCoverage,
                cloudDensityMultiplier,
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

    // Clouds \\

    private void parseClouds(
            ArpgObjectStruct arpg,
            String weatherName,
            float cloudDensityMultiplier,
            ObjectArrayList<CloudHandle> outHandles,
            FloatArrayList outChances,
            FloatArrayList outDensityScales) {

        if (!arpg.has("clouds"))
            return;

        ArpgArrayStruct cloudsArray = arpg.getAsArray("clouds");

        if (cloudsArray.size() > EngineSetting.MAX_CLOUDS_PER_WEATHER)
            throwException("Weather \"" + weatherName + "\" defines " + cloudsArray.size()
                    + " clouds — exceeds the maximum of " + EngineSetting.MAX_CLOUDS_PER_WEATHER
                    + " clouds per weather");

        for (ArpgElementStruct element : cloudsArray)
            parseCloudEntry(element.getAsObject(), cloudDensityMultiplier, outHandles, outChances,
                    outDensityScales);
    }

    private void parseCloudEntry(
            ArpgObjectStruct entryObject,
            float cloudDensityMultiplier,
            ObjectArrayList<CloudHandle> outHandles,
            FloatArrayList outChances,
            FloatArrayList outDensityScales) {

        String cloudName = ArpgUtility.validateString(entryObject, "name");
        CloudHandle cloudHandle = cloudManager.getCloudHandleFromCloudName(cloudName);

        float chance = parseFloat(entryObject, "chance", EngineSetting.DEFAULT_CLOUD_ENTRY_CHANCE);
        float densityMultiplier = parseFloat(
                entryObject, "densityMultiplier", EngineSetting.DEFAULT_CLOUD_ENTRY_DENSITY_MULTIPLIER);

        if (chance < 0f)
            throwException("Cloud entry \"" + cloudName + "\" has a negative chance: " + chance);

        if (densityMultiplier < 0f)
            throwException("Cloud entry \"" + cloudName + "\" has a negative densityMultiplier: " + densityMultiplier);

        outHandles.add(cloudHandle);
        outChances.add(chance);
        outDensityScales.add(densityMultiplier * cloudDensityMultiplier);
    }

    private FloatArrayList resolveCloudCoverages(FloatArrayList cloudChances, float cloudCoverage) {

        float maxChance = 0f;

        for (int i = 0; i < cloudChances.size(); i++)
            maxChance = Math.max(maxChance, cloudChances.getFloat(i));

        FloatArrayList cloudCoverages = new FloatArrayList(cloudChances.size());

        for (int i = 0; i < cloudChances.size(); i++)
            cloudCoverages.add(maxChance > 0f ? cloudCoverage * cloudChances.getFloat(i) / maxChance : 0f);

        return cloudCoverages;
    }

    // Utility \\

    private float parseFloat(ArpgObjectStruct arpg, String field, float fallback) {

        if (!arpg.has(field))
            return fallback;

        return arpg.get(field).getAsFloat();
    }
}
