package application.bootstrap.worldpipeline.biomemanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.weatherpipeline.season.Season;
import application.bootstrap.worldpipeline.biome.BiomeData;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.WeatherChanceStruct;
import engine.graphics.color.Color;
import engine.root.BuilderPackage;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses biome JSON into a BiomeData and wraps it in a BiomeHandle.
     * Reads the optional "weathers" block into a per-season chance-weighted
     * pool that WeatherManager resolves into live WeatherHandles on demand.
     * Each season's array accepts either a bare weather name string (given
     * a default relative chance) or an object with explicit "name" and
     * "chance" fields — both forms may be mixed freely within one array.
     */

    // Default relative weight applied to a bare weather-name string entry.
    private static final float DEFAULT_WEATHER_CHANCE = 1.0f;

    // Build \\

    BiomeHandle build(File file, File root) {

        String biomeName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short biomeID = RegistryUtility.toShortID(biomeName);

        JsonObject json = JsonUtility.loadJsonObject(file);
        Object2ObjectOpenHashMap<Season, ObjectArrayList<WeatherChanceStruct>> seasonWeatherEntries = parseWeathers(
                json);

        BiomeData biomeData = new BiomeData(biomeName, biomeID, Color.WHITE, seasonWeatherEntries);

        BiomeHandle biomeHandle = create(BiomeHandle.class);
        biomeHandle.constructor(biomeData);

        return biomeHandle;
    }

    // Parsing \\

    private Object2ObjectOpenHashMap<Season, ObjectArrayList<WeatherChanceStruct>> parseWeathers(JsonObject json) {

        Object2ObjectOpenHashMap<Season, ObjectArrayList<WeatherChanceStruct>> seasonWeatherEntries = new Object2ObjectOpenHashMap<>();

        if (!json.has("weathers"))
            return seasonWeatherEntries;

        JsonObject weathersObject = json.getAsJsonObject("weathers");

        for (Season season : Season.values()) {

            if (!weathersObject.has(season.name()))
                continue;

            JsonArray weatherArray = weathersObject.getAsJsonArray(season.name());
            ObjectArrayList<WeatherChanceStruct> entries = new ObjectArrayList<>(weatherArray.size());

            for (JsonElement element : weatherArray)
                entries.add(parseWeatherEntry(element));

            seasonWeatherEntries.put(season, entries);
        }

        return seasonWeatherEntries;
    }

    private WeatherChanceStruct parseWeatherEntry(JsonElement element) {

        if (element.isJsonPrimitive())
            return new WeatherChanceStruct(element.getAsString(), DEFAULT_WEATHER_CHANCE);

        JsonObject entryObject = element.getAsJsonObject();
        String weatherName = JsonUtility.validateString(entryObject, "name");
        float chance = entryObject.has("chance")
                ? entryObject.get("chance").getAsFloat()
                : DEFAULT_WEATHER_CHANCE;

        return new WeatherChanceStruct(weatherName, chance);
    }
}